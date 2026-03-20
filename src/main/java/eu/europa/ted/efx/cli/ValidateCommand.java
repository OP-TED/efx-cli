/*
 * Copyright 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European
 * Commission – subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package eu.europa.ted.efx.cli;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.helger.schematron.pure.SchematronResourcePure;
import com.helger.schematron.pure.xpath.XPathConfigBuilder;

import eu.europa.ted.eforms.sdk.SdkConstants;
import eu.europa.ted.efx.EfxTranslator;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "validate", description = "Validate an XML notice against EFX rules")
public class ValidateCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(ValidateCommand.class);

    private static final String SCHEMATRON_NS = "http://purl.oclc.org/dsdl/schematron";
    private static final String API_URL_PREFIX = "apiUrl-";

    @ParentCommand
    CliCommand parent;

    @Option(names = { "-r", "--rules" }, description = "EFX rules file")
    Path inputFile;

    @Option(names = { "-s", "--schematron" }, description = "Pre-compiled Schematron file (skips EFX translation)")
    Path schematronFile;

    @Option(names = { "-n", "--notice" }, description = "XML notice file to validate", required = true)
    Path noticeFile;

    @Option(names = { "-v", "--sdk-version" }, description = "eForms SDK version (e.g. 1.14.0)")
    String sdkVersion;

    @Option(names = { "-p", "--sdk-path" }, description = "Path to eForms SDK root")
    Path sdkPath;

    @Option(names = { "-e", "--endpoint" }, description = "Runtime endpoint URL override (e.g. default=http://localhost:8080/v1)")
    Map<String, String> endpointOverrides = new HashMap<>();

    @Option(names = { "-o", "--output" }, description = "Write raw SVRL report to file")
    Path outputFile;

    @Option(names = { "--mock" }, description = "Start a built-in mock API server. Values: true (returns 1), false (returns 0), error (returns -1)",
            arity = "0..1", defaultValue = Option.NULL_VALUE, fallbackValue = "true")
    String mock;

    @Option(names = { "--mode" }, description = "Validation mode: phpure (default) or schxslt")
    String mode;

    @Override
    public Integer call() throws Exception {
        final SessionContext session = SessionContext.instance();

        if (this.parent != null && this.parent.verbose) {
            session.setVerbose(true);
            LoggingConfigurator.enableDebug();
        } else if (session.verbose()) {
            LoggingConfigurator.enableDebug();
        }

        if (this.sdkVersion == null) {
            this.sdkVersion = session.sdkVersion();
        } else {
            session.setSdkVersion(this.sdkVersion);
        }

        if (this.sdkPath == null) {
            this.sdkPath = session.sdkPath();
        } else {
            session.setSdkPath(this.sdkPath);
        }

        if (this.mode == null) {
            this.mode = session.mode();
        } else {
            if (!"schxslt".equalsIgnoreCase(this.mode) && !"phpure".equalsIgnoreCase(this.mode)) {
                logger.error("Invalid mode: {} (expected: schxslt or phpure)", this.mode);
                return 1;
            }
            session.setMode(this.mode);
        }

        if (this.inputFile == null && this.schematronFile == null) {
            logger.error("Either --rules (EFX file) or --schematron (pre-compiled .sch) is required");
            return 1;
        }

        if (this.inputFile != null && !Files.exists(this.inputFile)) {
            logger.error("Rules file not found: {}", this.inputFile);
            return 1;
        }

        if (this.schematronFile != null && !Files.exists(this.schematronFile)) {
            logger.error("Schematron file not found: {}", this.schematronFile);
            return 1;
        }

        if (!Files.exists(this.noticeFile)) {
            logger.error("Notice file not found: {}", this.noticeFile);
            return 1;
        }

        if (this.inputFile != null && this.sdkVersion == null) {
            logger.error("--sdk-version is required when using --rules");
            return 1;
        }

        System.out.println("Validating notice: " + this.noticeFile);
        System.out.println("Mode: " + this.mode);

        try (MockApiServer mockServer = this.startMockServer()) {
            final File schFile;
            if (this.schematronFile != null) {
                schFile = this.schematronFile.toFile();
            } else {
                if (this.sdkPath == null) {
                    this.sdkPath = SdkConstants.DEFAULT_SDK_ROOT;
                }
                final Path tempDir = Files.createTempDirectory("efx-validate-");
                try (Spinner ignored = new Spinner("Translating EFX rules...")) {
                    this.translateRules(tempDir);
                }
                schFile = tempDir.resolve("dynamic/complete-validation.sch").toFile();
            }

            if (!schFile.exists()) {
                logger.error("Schematron file not found: {}", schFile);
                return 1;
            }

            if (mockServer != null) {
                this.overrideAllEndpoints(schFile, mockServer.baseUrl());
            }

            final Document svrl;
            try (Spinner ignored = new Spinner("Validating notice...")) {
                if ("phpure".equalsIgnoreCase(this.mode)) {
                    svrl = this.validatePhpure(schFile);
                } else {
                    svrl = this.validateSchxslt(schFile);
                }
            }

            if (this.outputFile == null) {
                final String noticeName = this.noticeFile.getFileName().toString();
                final int dot = noticeName.lastIndexOf('.');
                final String baseName = dot >= 0 ? noticeName.substring(0, dot) : noticeName;
                final Path outputDir = Path.of("output");
                Files.createDirectories(outputDir);
                this.outputFile = outputDir.resolve(baseName + ".svrl");
            }
            final Transformer serializer = TransformerFactory.newInstance().newTransformer();
            serializer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            serializer.transform(new DOMSource(svrl), new StreamResult(this.outputFile.toFile()));
            System.out.println("SVRL report written to: " + this.outputFile);

            final ValidationResult result = new ValidationResult(svrl);
            final LabelResolver labels;
            if (session.labels()) {
                try (Spinner ignored = new Spinner("Loading labels...")) {
                    labels = session.labelResolver();
                }
            } else {
                labels = null;
            }
            for (final ValidationResult.Failure failure : result.failures()) {
                final String message = labels != null
                        ? labels.resolve(failure.message()) : failure.message();
                System.out.printf("[%s] %s at %s: %s%n",
                        failure.role(), failure.id(), failure.location(), message);
            }

            System.out.printf("%d rule(s) checked, %d failure(s).%n",
                    result.rulesChecked(), result.failureCount());
            if (result.isValid()) {
                System.out.println("Validation passed.");
            } else {
                System.out.println("Validation failed.");
            }

            return result.isValid() ? 0 : 1;
        } catch (final Exception e) {
            logger.error("Validation failed: {}", e.getMessage());
            logger.debug("Stack trace:", e);
            return 1;
        }
    }

    private MockApiServer startMockServer() throws Exception {
        if (this.mock == null) {
            return null;
        }
        final String response;
        switch (this.mock.toLowerCase()) {
            case "true":
                response = "1";
                break;
            case "false":
                response = "0";
                break;
            case "error":
                response = "-1";
                break;
            default:
                response = this.mock;
                break;
        }
        final MockApiServer server = new MockApiServer(response);
        System.out.println("Mock API server started at " + server.baseUrl() + " (response: " + response + ")");
        return server;
    }

    private void overrideAllEndpoints(final File schFile, final String mockUrl) throws Exception {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        final Document doc = dbf.newDocumentBuilder().parse(schFile);
        final NodeList lets = doc.getElementsByTagNameNS(SCHEMATRON_NS, "let");
        for (int i = 0; i < lets.getLength(); i++) {
            final String name = ((Element) lets.item(i)).getAttribute("name");
            if (name.startsWith(API_URL_PREFIX)) {
                this.endpointOverrides.put(name.substring(API_URL_PREFIX.length()), mockUrl);
            }
        }
    }

    private void translateRules(final Path outputDir) throws Exception {
        final EfxCliTranslatorDependencyFactory factory =
                new EfxCliTranslatorDependencyFactory(this.sdkPath);
        final Map<String, String> translated =
                EfxTranslator.translateRules(factory, this.sdkVersion, this.inputFile);

        for (final Map.Entry<String, String> entry : translated.entrySet()) {
            final Path outFile = outputDir.resolve(entry.getKey());
            Files.createDirectories(outFile.getParent());
            Files.writeString(outFile, entry.getValue());
        }
    }

    private Document validateSchxslt(final File schFile) throws Exception {
        final Templates compiled = new SchematronCompiler().compile(schFile);
        final Transformer transformer = compiled.newTransformer();

        for (final Map.Entry<String, String> entry : this.endpointOverrides.entrySet()) {
            transformer.setParameter(API_URL_PREFIX + entry.getKey(), entry.getValue());
        }

        final DOMResult result = new DOMResult();
        transformer.transform(new StreamSource(this.noticeFile.toFile()), result);
        return (Document) result.getNode();
    }

    private Document validatePhpure(final File schFile) throws Exception {
        final File effectiveSchFile;

        if (this.endpointOverrides.isEmpty()) {
            effectiveSchFile = schFile;
        } else {
            final byte[] patched = this.patchEndpointParams(schFile);
            effectiveSchFile = new File(schFile.getParentFile(), "patched-" + schFile.getName());
            Files.write(effectiveSchFile.toPath(), patched);
        }

        final SchematronResourcePure resource = SchematronResourcePure.fromFile(effectiveSchFile);
        resource.setXPathConfig(new XPathConfigBuilder()
                .setXPathFunctionResolver(new EfxCallApiFunctionResolver())
                .build());

        final Document report = resource.applySchematronValidation(
                new StreamSource(this.noticeFile.toFile()));
        if (report == null) {
            throw new IllegalStateException("ph-schematron returned null validation report");
        }
        return report;
    }

    private byte[] patchEndpointParams(final File schFile) throws Exception {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        final DocumentBuilder builder = dbf.newDocumentBuilder();
        final Document doc = builder.parse(schFile);

        final NodeList lets = doc.getElementsByTagNameNS(SCHEMATRON_NS, "let");
        for (int i = 0; i < lets.getLength(); i++) {
            final Element let = (Element) lets.item(i);
            final String name = let.getAttribute("name");
            if (name.startsWith(API_URL_PREFIX)) {
                final String endpointName = name.substring(API_URL_PREFIX.length());
                final String override = this.endpointOverrides.get(endpointName);
                if (override != null) {
                    let.setAttribute("value", "'" + override + "'");
                }
            }
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final Transformer serializer = TransformerFactory.newInstance().newTransformer();
        serializer.transform(new DOMSource(doc), new StreamResult(baos));
        return baos.toByteArray();
    }
}
