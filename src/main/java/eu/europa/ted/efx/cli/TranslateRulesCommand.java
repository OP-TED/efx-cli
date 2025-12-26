package eu.europa.ted.efx.cli;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Callable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import eu.europa.ted.efx.EfxTranslator;

@Command(name = "translate-rules", description = "Translates EFX rules to Schematron")
public class TranslateRulesCommand implements Callable<Integer> {

    @Option(names = { "-i", "--input" }, description = "Input EFX rules file", required = true)
    Path inputFile;

    @Option(names = { "-o", "--output" }, description = "Output directory", required = true)
    Path outputDir;

    @Option(names = { "-v", "--sdk-version" }, description = "eForms SDK version (e.g. 1.0, 1.1)", required = true)
    String sdkVersion;

    @Option(names = { "-p", "--sdk-path" }, description = "Path to eForms SDK root", required = true)
    Path sdkPath;

    @Override
    public Integer call() throws Exception {
        System.out.println("Translating rules from " + inputFile + " to " + outputDir);

        EfxCliTranslatorDependencyFactory factory = new EfxCliTranslatorDependencyFactory(sdkPath);

        try {
            Map<String, String> result = EfxTranslator.translateRules(factory, sdkVersion, inputFile);

            File outDir = outputDir.toFile();
            if (!outDir.exists()) {
                outDir.mkdirs();
            }

            for (Map.Entry<String, String> entry : result.entrySet()) {
                File outFile = new File(outDir, entry.getKey());
                // Ensure parent directories exist
                outFile.getParentFile().mkdirs();
                try (FileWriter writer = new FileWriter(outFile)) {
                    writer.write(entry.getValue());
                }
                System.out.println("Generated: " + outFile.getAbsolutePath());
            }

            System.out.println("Translation completed successfully.");
            return 0;
        } catch (Exception e) {
            System.err.println("Translation failed: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
}
