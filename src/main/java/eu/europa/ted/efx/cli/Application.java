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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eu.europa.ted.efx.cli.shell.CliHighlighter;
import eu.europa.ted.efx.cli.shell.SessionContext;
import eu.europa.ted.efx.cli.shell.SessionSetting;
import org.jline.builtins.Completers;
import org.jline.builtins.ConfigurationPath;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.Builtins;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.widget.AutosuggestionWidgets;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;
import picocli.shell.jline3.PicocliCommands;
import picocli.shell.jline3.PicocliCommands.PicocliCommandsFactory;

public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(final String[] args) {
        org.slf4j.bridge.SLF4JBridgeHandler.removeHandlersForRootLogger();
        org.slf4j.bridge.SLF4JBridgeHandler.install();

        if (args.length > 0) {
            runOneShotCommand(args);
        } else {
            runInteractiveShell();
        }
    }

    private static void runOneShotCommand(final String[] args) {
        final CommandLine cli = new CommandLine(new CliCommand());

        cli.setExecutionExceptionHandler(new IExecutionExceptionHandler() {
            @Override
            public int handleExecutionException(final Exception ex, final CommandLine commandLine,
                    final ParseResult parseResult) {
                logger.error("Error executing the application with arguments {}. Please see the logs.",
                        Arrays.asList(args));
                logger.debug("Exception thrown:", ex);
                return 1;
            }
        });

        final int exitCode = cli.execute(args);
        System.exit(exitCode);
    }

    private static void runInteractiveShell() {
        try {
            final PicocliCommandsFactory factory = new PicocliCommandsFactory();
            final CommandLine cli = new CommandLine(new CliCommand(), factory);
            final PicocliCommands picocliCommands = new PicocliCommands(cli);

            final Path workDir = Paths.get("");
            final Builtins builtins = new Builtins(workDir, new ConfigurationPath(workDir, workDir), null);
            builtins.rename(Builtins.Command.TTOP, "top");
            builtins.alias("zle", "widget");
            builtins.alias("bindkey", "keymap");

            final Parser parser = new DefaultParser();

            try (final Terminal terminal = TerminalBuilder.builder().build()) {
                final SystemRegistry systemRegistry =
                        new SystemRegistryImpl(parser, terminal, Application::workDir, null);
                systemRegistry.setCommandRegistries(builtins, picocliCommands);
                systemRegistry.register("help", picocliCommands);

                final Completer completer = new FilenameFallbackCompleter(
                        systemRegistry.completer());

                final Set<String> commandNames = new HashSet<>(cli.getSubcommands().keySet());
                commandNames.addAll(Arrays.asList("help", "history", "clear"));

                final Path historyFile = Paths.get(System.getProperty("user.home"),
                        ".efx-cli", "history");
                Files.createDirectories(historyFile.getParent());

                final LineReader reader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .completer(completer)
                        .highlighter(new CliHighlighter(commandNames))
                        .parser(parser)
                        .variable(LineReader.LIST_MAX, 50)
                        .variable(LineReader.AMBIGUOUS_BINDING, 50L)
                        .variable(LineReader.HISTORY_FILE, historyFile)
                        .variable(LineReader.HISTORY_IGNORE, "^(clear|exit|help)$")
                        .build();
                reader.getKeyMaps().get(LineReader.MAIN)
                        .bind(new Reference(LineReader.KILL_WHOLE_LINE),
                                "\033");
                new AutosuggestionWidgets(reader).enable();
                builtins.setLineReader(reader);
                factory.setTerminal(terminal);

                System.out.print("\033[2J\033[H");
                System.out.flush();
                printBanner();

                final String prompt = "\033[36mefx-cli>\033[0m ";

                while (true) {
                    try {
                        systemRegistry.cleanUp();
                        final String rightPrompt = "\033[90m"
                                + SessionContext.instance().summary() + "\033[0m";
                        final String line = reader.readLine(prompt, rightPrompt,
                                (MaskingCallback) null, null);
                        systemRegistry.execute(line);
                    } catch (final UserInterruptException e) {
                        // Ctrl+C — ignore, just show a new prompt
                    } catch (final EndOfFileException e) {
                        // Ctrl+D — exit
                        return;
                    } catch (final Exception e) {
                        systemRegistry.trace(e);
                    }
                }
            }
        } catch (final IOException e) {
            logger.error("Failed to start interactive shell", e);
            System.exit(1);
        }
    }

    private static Path workDir() {
        return Paths.get(System.getProperty("user.dir"));
    }

    /**
     * Delegates to the system registry completer for commands and options,
     * adds positional parameter completion for known commands,
     * then falls back to file name completion when no candidates are found.
     */
    private static class FilenameFallbackCompleter implements Completer {

        private final Completer delegate;
        private final Completers.FileNameCompleter fileCompleter = new Completers.FileNameCompleter();

        FilenameFallbackCompleter(final Completer delegate) {
            this.delegate = delegate;
        }

        @Override
        public void complete(final LineReader reader, final ParsedLine line,
                final List<Candidate> candidates) {
            this.delegate.complete(reader, line, candidates);
            boolean handled = false;
            if (candidates.isEmpty()) {
                final List<String> words = line.words();
                if (!words.isEmpty() && "config".equals(words.get(0))) {
                    handled = true;
                    final int argIndex = line.wordIndex() - 1;
                    final String prefix = line.word();
                    if (argIndex == 0) {
                        for (final SessionSetting setting : SessionSetting.values()) {
                            if (setting.key().startsWith(prefix)) {
                                candidates.add(new Candidate(setting.key()));
                            }
                        }
                    } else if (argIndex == 1 && words.size() > 1) {
                        final SessionSetting setting = SessionSetting.forKey(words.get(1));
                        if (setting != null && setting.isPathValued()) {
                            this.fileCompleter.complete(reader, line, candidates);
                        } else if (setting != null) {
                            for (final String value : setting.allowedValues()) {
                                if (value.startsWith(prefix)) {
                                    candidates.add(new Candidate(value));
                                }
                            }
                        }
                    }
                }
            }
            if (!handled && candidates.isEmpty()) {
                final String word = line.word();
                final int eqIdx = word.indexOf('=');
                if (eqIdx >= 0 && word.startsWith("--")) {
                    final String optionPrefix = word.substring(0, eqIdx + 1);
                    final String pathPart = word.substring(eqIdx + 1);
                    final List<Candidate> fileCandidates = new ArrayList<>();
                    final ParsedLine fakeLine = new FakePathParsedLine(line, pathPart);
                    this.fileCompleter.complete(reader, fakeLine, fileCandidates);
                    for (final Candidate c : fileCandidates) {
                        candidates.add(new Candidate(optionPrefix + c.value(),
                                c.displ(), c.group(), c.descr(), c.suffix(),
                                c.key(), c.complete()));
                    }
                } else {
                    this.fileCompleter.complete(reader, line, candidates);
                }
            }
        }
    }

    /**
     * Wraps a {@link ParsedLine} to present only the path portion after {@code =}
     * to the file name completer.
     */
    private static class FakePathParsedLine implements ParsedLine {
        private final ParsedLine delegate;
        private final String pathPart;

        FakePathParsedLine(final ParsedLine delegate, final String pathPart) {
            this.delegate = delegate;
            this.pathPart = pathPart;
        }

        @Override
        public String word() {
            return this.pathPart;
        }

        @Override
        public int wordCursor() {
            return this.pathPart.length();
        }

        @Override
        public int wordIndex() {
            return this.delegate.wordIndex();
        }

        @Override
        public List<String> words() {
            return this.delegate.words();
        }

        @Override
        public String line() {
            return this.delegate.line();
        }

        @Override
        public int cursor() {
            return this.delegate.cursor();
        }
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("             ██████                                  ████   ███ ");
        System.out.println("            ███▒▒███                                ▒▒███  ▒▒▒  ");
        System.out.println("  ██████   ▒███ ▒▒▒  █████ █████             ██████  ▒███  ████ ");
        System.out.println(" ███▒▒███ ███████   ▒▒███ ▒▒███  ██████████ ███▒▒███ ▒███ ▒▒███ ");
        System.out.println("▒███████ ▒▒▒███▒     ▒▒▒█████▒  ▒▒▒▒▒▒▒▒▒▒ ▒███ ▒▒▒  ▒███  ▒███ ");
        System.out.println("▒███▒▒▒    ▒███       ███▒▒▒███            ▒███  ███ ▒███  ▒███ ");
        System.out.println("▒▒██████   █████     █████ █████           ▒▒██████  █████ █████");
        System.out.println(" ▒▒▒▒▒▒   ▒▒▒▒▒     ▒▒▒▒▒ ▒▒▒▒▒             ▒▒▒▒▒▒  ▒▒▒▒▒ ▒▒▒▒▒ ");
        System.out.println();
        System.out.println("  Type 'help' for available commands, TAB for completion, Ctrl+D to exit.");
        System.out.println();
    }
}
