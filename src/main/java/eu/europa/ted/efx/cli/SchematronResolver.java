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

import java.net.URL;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

/**
 * URI resolver that loads SchXSLT stylesheets from the classpath.
 * Falls back to the JAXP default resolver for all other URIs (e.g. file:// paths
 * for Schematron include directives).
 */
class SchematronResolver implements URIResolver {

    @Override
    public Source resolve(final String href, final String base) throws TransformerException {
        final URL resource = this.getClass().getResource(href);
        if (resource != null) {
            final StreamSource source = new StreamSource(this.getClass().getResourceAsStream(href));
            source.setSystemId(resource.toString());
            return source;
        }
        return null;
    }
}
