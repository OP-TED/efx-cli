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

@Command(name = "validate", description = "Validate an XML notice against EFX rules")
public class ValidateCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(ValidateCommand.class);

    private static final String SCHEMATRON_NS = "http://purl.oclc.org/dsdl/schematron";
    private static final String API_URL_PREFIX = "apiUrl-";

    @Option(names = { "-i", "--input" }, description = "Input EFX rules file")
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

    @Option(names = { "--mode" }, description = "Validation mode: schxslt or phpure (default: schxslt)",
            defaultValue = "schxslt")
    String mode;

    @Override
    public Integer call() throws Exception {
        if (this.inputFile == null && this.schematronFile == null) {
            logger.error("Either --input (EFX file) or --schematron (pre-compiled .sch) is required");
            return 1;
        }

        if (this.inputFile != null && this.sdkVersion == null) {
            logger.error("--sdk-version is required when using --input");
            return 1;
        }

        System.out.println("Validating notice: " + this.noticeFile);
        System.out.println("Mode: " + this.mode);

        try {
            final File schFile;
            if (this.schematronFile != null) {
                schFile = this.schematronFile.toFile();
                System.out.println("Using pre-compiled Schematron: " + this.schematronFile);
            } else {
                System.out.println("Translating EFX rules from " + this.inputFile);
                if (this.sdkPath == null) {
                    this.sdkPath = SdkConstants.DEFAULT_SDK_ROOT;
                }
                final Path tempDir = Files.createTempDirectory("efx-validate-");
                this.translateRules(tempDir);
                schFile = tempDir.resolve("dynamic/complete-validation.sch").toFile();
            }

            if (!schFile.exists()) {
                logger.error("Schematron file not found: {}", schFile);
                return 1;
            }

            final Document svrl;
            if ("phpure".equalsIgnoreCase(this.mode)) {
                svrl = this.validatePhpure(schFile);
            } else {
                svrl = this.validateSchxslt(schFile);
            }

            final ValidationResult result = new ValidationResult(svrl);
            result.print(System.out);

            if (result.isValid()) {
                System.out.println("Validation passed.");
            } else {
                System.out.printf("Validation failed with %d failure(s).%n", result.failureCount());
            }

            return result.isValid() ? 0 : 1;
        } catch (final Exception e) {
            logger.error("Validation failed: {}", e.getMessage(), e);
            return 1;
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
