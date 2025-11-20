package eu.europa.ted.efx.cli;

import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        CommandLine cli = new CommandLine(new CliCommand());

        cli.setExecutionExceptionHandler(new IExecutionExceptionHandler() {
            @Override
            public int handleExecutionException(Exception ex, CommandLine commandLine, ParseResult parseResult) {
                logger.error("Error executing the application with arguments {}. Please see the logs.",
                        Arrays.asList(args));
                logger.debug("Exception thrown:", ex);
                return 1;
            }
        });

        int exitCode = cli.execute(args);
        System.exit(exitCode);
    }
}
