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

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import java.util.concurrent.Callable;

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
}, versionProvider = CliCommand.ManifestVersionProvider.class, subcommands = { TranslateRulesCommand.class, ValidateCommand.class, ConfigCommand.class, ClearCommand.class })
public class CliCommand implements Callable<Integer> {

    @Option(names = { "--verbose" }, description = "Enable verbose output", scope = picocli.CommandLine.ScopeType.INHERIT)
    boolean verbose;

    @Override
    public Integer call() throws Exception {
        if (this.verbose) {
            LoggingConfigurator.enableDebug();
        }
        return 0;
    }

    /**
     * {@link picocli.CommandLine.IVersionProvider} implementation that returns
     * version information from the
     * jar file's {@code /META-INF/MANIFEST.MF} file.
     */
    static class ManifestVersionProvider implements picocli.CommandLine.IVersionProvider {
        public String[] getVersion() throws Exception {
            java.util.Enumeration<java.net.URL> resources = picocli.CommandLine.class.getClassLoader()
                    .getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                java.net.URL url = resources.nextElement();
                try {
                    java.util.jar.Manifest manifest = new java.util.jar.Manifest(url.openStream());
                    if (isApplicableManifest(manifest)) {
                        java.util.jar.Attributes attr = manifest.getMainAttributes();
                        return new String[] { get(attr, "Implementation-Title") + " version \""
                                + get(attr, "Implementation-Version") + "\"" };
                    }
                } catch (java.io.IOException ex) {
                    return new String[] { "Unable to read from " + url + ": " + ex };
                }
            }
            return new String[0];
        }

        private boolean isApplicableManifest(java.util.jar.Manifest manifest) {
            java.util.jar.Attributes attributes = manifest.getMainAttributes();
            return "efx-cli".equals(get(attributes, "Implementation-Title"));
        }

        private static Object get(java.util.jar.Attributes attributes, String key) {
            return attributes.get(new java.util.jar.Attributes.Name(key));
        }
    }
}
