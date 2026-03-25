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
package eu.europa.ted.efx.cli;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.Callable;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import eu.europa.ted.efx.cli.command.ClearCommand;
import eu.europa.ted.efx.cli.command.ConfigCommand;
import eu.europa.ted.efx.cli.command.ExtractDependenciesCommand;
import eu.europa.ted.efx.cli.command.TranslateRulesCommand;
import eu.europa.ted.efx.cli.command.ValidateCommand;
import eu.europa.ted.efx.cli.command.VisualiseCommand;
import eu.europa.ted.efx.cli.shell.LoggingConfigurator;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@Command(name = "efx-cli", mixinStandardHelpOptions = true, description = {
        "             ██████                                  ████   ███ ",
        "            ███▒▒███                                ▒▒███  ▒▒▒  ",
        "  ██████   ▒███ ▒▒▒  █████ █████             ██████  ▒███  ████ ",
        " ███▒▒███ ███████   ▒▒███ ▒▒███  ██████████ ███▒▒███ ▒███ ▒▒███ ",
        "▒███████ ▒▒▒███▒     ▒▒▒█████▒  ▒▒▒▒▒▒▒▒▒▒ ▒███ ▒▒▒  ▒███  ▒███ ",
        "▒███▒▒▒    ▒███       ███▒▒▒███            ▒███  ███ ▒███  ▒███ ",
        "▒▒██████   █████     █████ █████           ▒▒██████  █████ █████",
        " ▒▒▒▒▒▒   ▒▒▒▒▒     ▒▒▒▒▒ ▒▒▒▒▒             ▒▒▒▒▒▒  ▒▒▒▒▒ ▒▒▒▒▒ ",
        "",
        "Command Line Interface for the EFX Toolkit"
}, versionProvider = CliCommand.ManifestVersionProvider.class, subcommands = { TranslateRulesCommand.class, ValidateCommand.class, VisualiseCommand.class, ExtractDependenciesCommand.class, ConfigCommand.class, ClearCommand.class })
public class CliCommand implements Callable<Integer> {

    @Option(names = { "--verbose" }, description = "Enable verbose output", scope = ScopeType.INHERIT)
    public boolean verbose;

    @Override
    public Integer call() throws Exception {
        if (this.verbose) {
            LoggingConfigurator.instance().enableDebug();
        }
        return 0;
    }

    /**
     * {@link IVersionProvider} implementation that returns
     * version information from the
     * jar file's {@code /META-INF/MANIFEST.MF} file.
     */
    static class ManifestVersionProvider implements IVersionProvider {
        public String[] getVersion() throws Exception {
            final Enumeration<URL> resources = picocli.CommandLine.class.getClassLoader()
                    .getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                final URL url = resources.nextElement();
                try {
                    final Manifest manifest = new Manifest(url.openStream());
                    if (isApplicableManifest(manifest)) {
                        final Attributes attr = manifest.getMainAttributes();
                        return new String[] { get(attr, "Implementation-Title") + " version \""
                                + get(attr, "Implementation-Version") + "\"" };
                    }
                } catch (final IOException ex) {
                    return new String[] { "Unable to read from " + url + ": " + ex };
                }
            }
            return new String[0];
        }

        private boolean isApplicableManifest(final Manifest manifest) {
            final Attributes attributes = manifest.getMainAttributes();
            return "efx-cli".equals(get(attributes, "Implementation-Title"));
        }

        private static Object get(final Attributes attributes, final String key) {
            return attributes.get(new Attributes.Name(key));
        }
    }
}
