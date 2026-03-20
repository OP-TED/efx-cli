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
