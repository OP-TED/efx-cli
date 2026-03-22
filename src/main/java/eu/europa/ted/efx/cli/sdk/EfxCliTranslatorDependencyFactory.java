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

import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;

import org.antlr.v4.runtime.BaseErrorListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ted.eforms.sdk.ComponentFactory;
import eu.europa.ted.eforms.sdk.resource.SdkDownloader;
import eu.europa.ted.eforms.sdk.schematron.SchematronGenerator;
import eu.europa.ted.efx.exceptions.ThrowingErrorListener;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.interfaces.TranslatorDependencyFactory;
import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.interfaces.ValidatorGenerator;

/**
 * Unified dependency factory for both validation (rules translation) and
 * visualisation (template translation). Uses {@link ComponentFactory} for all
 * component creation and {@link SdkDownloader} for SDK auto-download.
 */
public class EfxCliTranslatorDependencyFactory implements TranslatorDependencyFactory {

    private static final Logger logger = LoggerFactory.getLogger(EfxCliTranslatorDependencyFactory.class);

    private final Path sdkRootPath;
    private final boolean allowSnapshots;

    public EfxCliTranslatorDependencyFactory(final Path sdkRootPath, final boolean allowSnapshots) {
        this.sdkRootPath = sdkRootPath;
        this.allowSnapshots = allowSnapshots;
    }

    @Override
    public SymbolResolver createSymbolResolver(final String sdkVersion, final String qualifier) {
        try {
            SdkDownloader.downloadSdk(sdkVersion, this.sdkRootPath, this.allowSnapshots);
        } catch (final Exception e) {
            logger.warn("SDK download failed ({}), assuming SDK is already available on disk.",
                    e.getMessage());
        }

        try {
            return ComponentFactory.getSymbolResolver(sdkVersion, qualifier, this.sdkRootPath);
        } catch (final InstantiationException e) {
            throw new RuntimeException(MessageFormat.format(
                    "Failed to instantiate Symbol Resolver for SDK version [{0}]", sdkVersion), e);
        }
    }

    @Override
    public ScriptGenerator createScriptGenerator(final String sdkVersion, final String qualifier,
            final TranslatorOptions options) {
        try {
            return ComponentFactory.getScriptGenerator(sdkVersion, qualifier, options);
        } catch (final InstantiationException e) {
            throw new RuntimeException(MessageFormat.format(
                    "Failed to instantiate Script Generator for SDK version [{0}]", sdkVersion), e);
        }
    }

    @Override
    public MarkupGenerator createMarkupGenerator(final String sdkVersion, final String qualifier,
            final TranslatorOptions options) {
        try {
            return ComponentFactory.getMarkupGenerator(sdkVersion, qualifier, options);
        } catch (final InstantiationException e) {
            throw new RuntimeException(MessageFormat.format(
                    "Failed to instantiate Markup Generator for SDK version [{0}]", sdkVersion), e);
        }
    }

    @Override
    public ValidatorGenerator createValidatorGenerator(final String sdkVersion, final String qualifier,
            final TranslatorOptions options) {
        return new SchematronGenerator(options);
    }

    @Override
    public BaseErrorListener createErrorListener() {
        return ThrowingErrorListener.INSTANCE;
    }
}
