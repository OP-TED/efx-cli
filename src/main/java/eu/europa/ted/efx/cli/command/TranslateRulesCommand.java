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
package eu.europa.ted.efx.cli.command;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ted.efx.EfxTranslator;
import eu.europa.ted.efx.cli.CliCommand;
import eu.europa.ted.efx.cli.shell.LoggingConfigurator;
import eu.europa.ted.efx.cli.shell.SessionContext;
import eu.europa.ted.efx.cli.shell.Spinner;
import eu.europa.ted.efx.cli.sdk.EfxCliTranslatorDependencyFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "translate-rules", description = "Translates EFX rules to Schematron")
public class TranslateRulesCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(TranslateRulesCommand.class);

    @ParentCommand
    CliCommand parent;

    @Option(names = { "-i", "--input" }, description = "Input EFX rules file", required = true)
    Path inputFile;

    @Option(names = { "-o", "--output" }, description = "Output directory", required = true)
    Path outputDir;

    @Option(names = { "-v", "--sdk-version" }, description = "eForms SDK version (e.g. 1.0, 1.1)")
    String sdkVersion;

    @Option(names = { "-p", "--sdk-path" }, description = "Path to eForms SDK root")
    Path sdkPath;

    @Override
    public Integer call() throws Exception {
        final SessionContext session = SessionContext.instance();

        if (this.parent != null && this.parent.verbose) {
            session.setVerbose(true);
            LoggingConfigurator.instance().enableDebug();
        } else if (session.verbose()) {
            LoggingConfigurator.instance().enableDebug();
        }

        if (this.sdkVersion == null) {
            this.sdkVersion = session.sdkVersion();
        } else {
            session.setSdkVersion(this.sdkVersion);
        }

        if (this.sdkPath == null) {
            this.sdkPath = session.sdkPath();
        } else {
            session.setSdkPath(this.sdkPath);
        }

        if (this.sdkVersion == null) {
            logger.error("--sdk-version is required (or set via: config sdk-version <version>)");
            return 1;
        }

        if (!Files.exists(this.inputFile)) {
            logger.error("Input file not found: {}", this.inputFile);
            return 1;
        }

        System.out.println("Translating rules from " + this.inputFile + " to " + this.outputDir);
        System.out.println("Using SDK at: " + this.sdkPath.toAbsolutePath());

        final EfxCliTranslatorDependencyFactory factory = new EfxCliTranslatorDependencyFactory(this.sdkPath);

        try (Spinner ignored = new Spinner("Translating EFX rules...")) {
            final Map<String, String> result = EfxTranslator.translateRules(factory, this.sdkVersion, this.inputFile);

            final File outDir = this.outputDir.toFile();
            if (!outDir.exists()) {
                outDir.mkdirs();
            }

            for (final Map.Entry<String, String> entry : result.entrySet()) {
                final File outFile = new File(outDir, entry.getKey());
                outFile.getParentFile().mkdirs();
                try (FileWriter writer = new FileWriter(outFile)) {
                    writer.write(entry.getValue());
                }
                System.out.println("Generated: " + outFile.getAbsolutePath());
            }
        } catch (final Exception e) {
            logger.error("Translation failed: {}", e.getMessage());
            logger.debug("Stack trace:", e);
            return 1;
        }

        System.out.println("Translation completed successfully.");
        return 0;
    }
}
