package eu.europa.ted.efx.cli;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Compiles a Schematron file into a reusable XSLT {@link Templates} using the
 * SchXSLT 3-step pipeline: include, expand, compile-for-svrl.
 */
class SchematronCompiler {

    private static final String[] PIPELINE_STEPS = {
            "/xslt/2.0/include.xsl",
            "/xslt/2.0/expand.xsl",
            "/xslt/2.0/compile-for-svrl.xsl"
    };

    private final TransformerFactory transformerFactory;
    private final SchematronResolver resolver;

    SchematronCompiler() {
        this.resolver = new SchematronResolver();
        this.transformerFactory = TransformerFactory.newInstance();
        this.transformerFactory.setURIResolver(this.resolver);
    }

    Templates compile(final File schematronFile) throws TransformerException {
        final String systemId = schematronFile.toURI().toString();
        Source source = new StreamSource(schematronFile);
        String result = null;

        for (final String step : PIPELINE_STEPS) {
            final Source stepSource = this.resolver.resolve(step, null);
            final Transformer transformer = this.transformerFactory.newTransformer(stepSource);
            final StringWriter writer = new StringWriter();
            transformer.transform(source, new StreamResult(writer));
            result = writer.toString();
            source = new StreamSource(new StringReader(result), systemId);
        }

        return this.transformerFactory.newTemplates(
                new StreamSource(new StringReader(result), systemId));
    }
}
