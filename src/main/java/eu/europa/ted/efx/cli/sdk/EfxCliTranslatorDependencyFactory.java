/*
 * Copyright 2025 European Union
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
package eu.europa.ted.efx.cli.sdk;

import java.nio.file.Path;

import org.antlr.v4.runtime.BaseErrorListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ted.eforms.sdk.SdkSymbolResolver;
import eu.europa.ted.eforms.sdk.resource.SdkDownloader;
import eu.europa.ted.efx.exceptions.ThrowingErrorListener;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.interfaces.TranslatorDependencyFactory;
import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.interfaces.ValidatorGenerator;
import eu.europa.ted.eforms.sdk.schematron.SchematronGenerator;
import eu.europa.ted.efx.xpath.XPathScriptGenerator;

public class EfxCliTranslatorDependencyFactory implements TranslatorDependencyFactory {

    private static final Logger logger = LoggerFactory.getLogger(EfxCliTranslatorDependencyFactory.class);

    private final Path sdkRootPath;

    public EfxCliTranslatorDependencyFactory(final Path sdkRootPath) {
        this.sdkRootPath = sdkRootPath;
    }

    @Override
    public SymbolResolver createSymbolResolver(final String sdkVersion, final String qualifier) {
        try {
            SdkDownloader.downloadSdk(sdkVersion, this.sdkRootPath, false);
        } catch (final Exception e) {
            logger.warn("SDK download failed ({}), assuming SDK is already available on disk.", e.getMessage());
        }

        try {
            return new SdkSymbolResolver(sdkVersion, this.sdkRootPath);
        } catch (final InstantiationException e) {
            throw new RuntimeException("Failed to instantiate SdkSymbolResolver", e);
        }
    }

    @Override
    public ScriptGenerator createScriptGenerator(final String sdkVersion, final String qualifier, final TranslatorOptions options) {
        return new XPathScriptGenerator(options);
    }

    @Override
    public MarkupGenerator createMarkupGenerator(final String sdkVersion, final String qualifier, final TranslatorOptions options) {
        throw new UnsupportedOperationException(
                "CLI currently supports only Rules translation which uses ValidatorGenerator");
    }

    @Override
    public ValidatorGenerator createValidatorGenerator(final String sdkVersion, final String qualifier,
            final TranslatorOptions options) {
        return new SchematronGenerator();
    }

    @Override
    public BaseErrorListener createErrorListener() {
        return ThrowingErrorListener.INSTANCE;
    }
}
