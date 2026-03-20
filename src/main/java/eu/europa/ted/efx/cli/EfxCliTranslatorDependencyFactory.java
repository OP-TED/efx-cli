package eu.europa.ted.efx.cli;

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
    public ScriptGenerator createScriptGenerator(String sdkVersion, String qualifier, TranslatorOptions options) {
        return new XPathScriptGenerator(options);
    }

    @Override
    public MarkupGenerator createMarkupGenerator(String sdkVersion, String qualifier, TranslatorOptions options) {
        throw new UnsupportedOperationException(
                "CLI currently supports only Rules translation which uses ValidatorGenerator");
    }

    @Override
    public ValidatorGenerator createValidatorGenerator(String sdkVersion, String qualifier,
            TranslatorOptions options) {
        return new SchematronGenerator();
    }

    @Override
    public BaseErrorListener createErrorListener() {
        return ThrowingErrorListener.INSTANCE;
    }
}
