package eu.europa.ted.eforms.efx.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(name = "efx-cli", mixinStandardHelpOptions = true, version = "efx-cli 1.0.0-SNAPSHOT", description = "Command Line Interface for the EFX Toolkit")
public class EfxCli implements Callable<Integer> {

    @Option(names = { "-v", "--verbose" }, description = "Enable verbose output")
    boolean verbose;

    @Override
    public Integer call() throws Exception {
        System.out.println("Hello from EFX CLI!");
        if (verbose) {
            System.out.println("Verbose mode is on.");
        }
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new EfxCli()).execute(args);
        System.exit(exitCode);
    }
}
