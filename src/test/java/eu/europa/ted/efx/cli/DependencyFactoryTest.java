package eu.europa.ted.efx.cli;

import org.junit.jupiter.api.Test;
import eu.europa.ted.efx.interfaces.ValidatorGenerator;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DependencyFactoryTest {

    @Test
    public void testCreateValidatorGenerator() {
        EfxCliTranslatorDependencyFactory factory = new EfxCliTranslatorDependencyFactory(Paths.get("."));
        ValidatorGenerator generator = factory.createValidatorGenerator("1.0", "",
                eu.europa.ted.efx.EfxTranslatorOptions.DEFAULT);
        assertNotNull(generator);
    }
}
