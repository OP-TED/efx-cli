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
package eu.europa.ted.efx.cli.validation;

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
public class SchematronCompiler {

    private static final String[] PIPELINE_STEPS = {
            "/xslt/2.0/include.xsl",
            "/xslt/2.0/expand.xsl",
            "/xslt/2.0/compile-for-svrl.xsl"
    };

    private final TransformerFactory transformerFactory;
    private final SchematronResolver resolver;

    public SchematronCompiler() {
        this.resolver = new SchematronResolver();
        this.transformerFactory = TransformerFactory.newInstance();
        this.transformerFactory.setURIResolver(this.resolver);
    }

    public Templates compile(final File schematronFile) throws TransformerException {
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
