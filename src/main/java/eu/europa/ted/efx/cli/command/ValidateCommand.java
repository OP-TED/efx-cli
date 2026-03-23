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
package eu.europa.ted.efx.cli.command;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.OutputKeys;
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
import eu.europa.ted.eforms.NoticeDocument;
import eu.europa.ted.efx.EfxTranslator;
import eu.europa.ted.efx.cli.CliCommand;
import eu.europa.ted.efx.cli.shell.LoggingConfigurator;
import eu.europa.ted.efx.cli.shell.SessionContext;
import eu.europa.ted.efx.cli.shell.Spinner;
import eu.europa.ted.efx.cli.validation.EfxCallApiFunctionResolver;
import eu.europa.ted.efx.cli.sdk.EfxCliTranslatorDependencyFactory;
import eu.europa.ted.efx.cli.sdk.LabelResolver;
import eu.europa.ted.efx.cli.validation.MockApiServer;
import eu.europa.ted.efx.cli.validation.SchematronCompiler;
import eu.europa.ted.efx.cli.validation.ValidationResult;
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

    @Option(names = { "-n", "--notice" }, description = "XML notice file to validate")
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

    private static final Map<String, String> MOCK_RESPONSES = Map.of(
            "true", "1",
            "false", "0",
            "error", "-1"
    );

    @Override
    public Integer call() throws Exception {
        this.hydrateFromSession();

        final String validationError = this.validateInputs();
        if (validationError != null) {
            logger.error(validationError);
            return 1;
        }

        System.out.println("Validating notice: " + this.noticeFile);
        System.out.println("Mode: " + this.mode);

        try (MockApiServer mockServer = this.startMockServer()) {
            final File schFile = this.resolveSchematron();

            if (mockServer != null) {
                this.overrideAllEndpoints(schFile, mockServer.baseUrl());
            }

            final Document svrl;
            try (Spinner ignored = new Spinner("Validating notice...")) {
                svrl = this.validate(schFile);
            }

            this.writeSvrlReport(svrl);
            this.printResults(svrl);

            return new ValidationResult(svrl).isValid() ? 0 : 1;
        } catch (final Exception e) {
            logger.error("Validation failed: {}", e.getMessage());
            logger.debug("Stack trace:", e);
            return 1;
        }
    }

    private void hydrateFromSession() {
        final SessionContext session = SessionContext.instance();

        if (this.parent != null && this.parent.verbose) {
            session.setVerbose(true);
            LoggingConfigurator.instance().enableDebug();
        } else if (session.verbose()) {
            LoggingConfigurator.instance().enableDebug();
        }

        if (this.noticeFile == null) {
            this.noticeFile = session.notice();
        } else {
            session.setNotice(this.noticeFile);
        }

        if (this.sdkVersion == null && this.noticeFile != null && Files.exists(this.noticeFile)) {
            try {
                this.sdkVersion = new NoticeDocument(this.noticeFile).getEformsSdkVersion();
            } catch (final Exception e) {
                logger.debug("Could not extract SDK version from notice: {}", e.getMessage());
            }
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
        } else if ("schxslt".equalsIgnoreCase(this.mode) || "phpure".equalsIgnoreCase(this.mode)) {
            session.setMode(this.mode);
        }
    }

    private String validateInputs() {
        if (this.mode != null
                && !"schxslt".equalsIgnoreCase(this.mode)
                && !"phpure".equalsIgnoreCase(this.mode)) {
            return "Invalid mode: " + this.mode + " (expected: schxslt or phpure)";
        }
        if (this.inputFile == null && this.schematronFile == null) {
            return "Either --rules (EFX file) or --schematron (pre-compiled .sch) is required";
        }
        if (this.inputFile != null && !Files.exists(this.inputFile)) {
            return "Rules file not found: " + this.inputFile;
        }
        if (this.schematronFile != null && !Files.exists(this.schematronFile)) {
            return "Schematron file not found: " + this.schematronFile;
        }
        if (this.noticeFile == null) {
            return "Notice file is required (--notice or set via 'config notice <path>')";
        }
        if (!Files.exists(this.noticeFile)) {
            return "Notice file not found: " + this.noticeFile;
        }
        if (this.inputFile != null && this.sdkVersion == null) {
            return "--sdk-version is required when using --rules";
        }
        return null;
    }

    private File resolveSchematron() throws Exception {
        if (this.schematronFile != null) {
            return this.schematronFile.toFile();
        }

        if (this.sdkPath == null) {
            this.sdkPath = SdkConstants.DEFAULT_SDK_ROOT;
        }
        final Path tempDir = Files.createTempDirectory("efx-validate-");
        try (Spinner ignored = new Spinner("Translating EFX rules...")) {
            this.translateRules(tempDir);
        }
        final File schFile = tempDir.resolve("dynamic/complete-validation.sch").toFile();
        if (!schFile.exists()) {
            throw new IllegalStateException("Schematron file not found: " + schFile);
        }
        return schFile;
    }

    private Document validate(final File schFile) throws Exception {
        if ("phpure".equalsIgnoreCase(this.mode)) {
            return this.validatePhpure(schFile);
        }
        return this.validateSchxslt(schFile);
    }

    private void writeSvrlReport(final Document svrl) throws Exception {
        if (this.outputFile == null) {
            final String noticeName = this.noticeFile.getFileName().toString();
            final int dot = noticeName.lastIndexOf('.');
            final String baseName = dot >= 0 ? noticeName.substring(0, dot) : noticeName;
            final Path outputDir = Path.of("output");
            Files.createDirectories(outputDir);
            this.outputFile = outputDir.resolve(baseName + ".svrl");
        }
        final Transformer serializer = TransformerFactory.newInstance().newTransformer();
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        serializer.transform(new DOMSource(svrl), new StreamResult(this.outputFile.toFile()));
        System.out.println("SVRL report written to: " + this.outputFile);
    }

    private void printResults(final Document svrl) {
        final SessionContext session = SessionContext.instance();
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
        System.out.println(result.isValid() ? "Validation passed." : "Validation failed.");
    }

    private MockApiServer startMockServer() throws Exception {
        if (this.mock == null) {
            return null;
        }
        final String response = MOCK_RESPONSES.getOrDefault(this.mock.toLowerCase(), this.mock);
        final MockApiServer server = new MockApiServer(response);
        System.out.println("Mock API server started at " + server.baseUrl()
                + " (response: " + response + ")");
        return server;
    }

    private void overrideAllEndpoints(final File schFile, final String overrideUrl) throws Exception {
        for (final String endpointName : this.extractEndpointNames(schFile)) {
            this.endpointOverrides.put(endpointName, overrideUrl);
        }
    }

    private void translateRules(final Path outputDir) throws Exception {
        final EfxCliTranslatorDependencyFactory factory =
                new EfxCliTranslatorDependencyFactory(this.sdkPath,
                        SessionContext.instance().snapshots());
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
        final Map<String, String> endpointRegistry = this.buildEndpointRegistry(schFile);

        final SchematronResourcePure resource = SchematronResourcePure.fromFile(schFile);
        resource.setXPathConfig(new XPathConfigBuilder()
                .setXPathFunctionResolver(new EfxCallApiFunctionResolver(endpointRegistry))
                .build());

        final Document report = resource.applySchematronValidation(
                new StreamSource(this.noticeFile.toFile()));
        if (report == null) {
            throw new IllegalStateException("ph-schematron returned null validation report");
        }
        return report;
    }

    private Map<String, String> buildEndpointRegistry(final File schFile) throws Exception {
        final Map<String, String> registry = new HashMap<>();

        for (final String endpointName : this.extractEndpointNames(schFile)) {
            registry.put(endpointName, this.readEndpointUrl(schFile, endpointName));
        }

        registry.putAll(this.endpointOverrides);
        return registry;
    }

    private List<String> extractEndpointNames(final File schFile) throws Exception {
        final List<String> names = new ArrayList<>();
        final NodeList lets = this.parseSchematronLets(schFile);
        for (int i = 0; i < lets.getLength(); i++) {
            final String name = ((Element) lets.item(i)).getAttribute("name");
            if (name.startsWith(API_URL_PREFIX)) {
                names.add(name.substring(API_URL_PREFIX.length()));
            }
        }
        return names;
    }

    private String readEndpointUrl(final File schFile, final String endpointName) throws Exception {
        final NodeList lets = this.parseSchematronLets(schFile);
        final String targetName = API_URL_PREFIX + endpointName;
        for (int i = 0; i < lets.getLength(); i++) {
            final Element let = (Element) lets.item(i);
            if (targetName.equals(let.getAttribute("name"))) {
                String value = let.getAttribute("value");
                if (value.startsWith("'") && value.endsWith("'")) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
        }
        return "";
    }

    private NodeList parseSchematronLets(final File schFile) throws Exception {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        return dbf.newDocumentBuilder().parse(schFile)
                .getElementsByTagNameNS(SCHEMATRON_NS, "let");
    }
}
