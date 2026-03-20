package eu.europa.ted.efx.cli;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionResolver;

/**
 * JAXP {@link XPathFunctionResolver} that resolves the {@code efx:call-api} function
 * for use with ph-schematron-pure in PHPURE validation mode.
 */
class EfxCallApiFunctionResolver implements XPathFunctionResolver {

    private static final String EFX_NAMESPACE = "http://eforms.ted.europa.eu/efx";
    private static final String CALL_API = "call-api";

    @Override
    public XPathFunction resolveFunction(final QName functionName, final int arity) {
        if (EFX_NAMESPACE.equals(functionName.getNamespaceURI())
                && CALL_API.equals(functionName.getLocalPart())
                && arity >= 2) {
            return new EfxCallApiFunction();
        }
        return null;
    }
}
