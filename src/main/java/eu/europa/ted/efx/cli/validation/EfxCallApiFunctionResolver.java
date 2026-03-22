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

import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionResolver;

/**
 * JAXP {@link XPathFunctionResolver} that resolves the {@code efx:call-api} function
 * for use with ph-schematron-pure in PHPURE validation mode.
 *
 * <p>The resolver holds an endpoint registry mapping endpoint names to URLs.
 * When {@code efx:call-api('endpointName', ...)} is evaluated, the function
 * resolves the endpoint name to its URL using this registry.</p>
 */
public class EfxCallApiFunctionResolver implements XPathFunctionResolver {

    private static final String EFX_NAMESPACE = "http://eforms.ted.europa.eu/efx";
    private static final String CALL_API = "call-api";

    private final Map<String, String> endpointRegistry;

    public EfxCallApiFunctionResolver(final Map<String, String> endpointRegistry) {
        this.endpointRegistry = endpointRegistry;
    }

    @Override
    public XPathFunction resolveFunction(final QName functionName, final int arity) {
        if (EFX_NAMESPACE.equals(functionName.getNamespaceURI())
                && CALL_API.equals(functionName.getLocalPart())
                && arity >= 2) {
            return new EfxCallApiFunction(this.endpointRegistry);
        }
        return null;
    }
}
