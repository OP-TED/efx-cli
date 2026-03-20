package eu.europa.ted.efx.cli;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;

@Command(name = "clear", description = "Clear the terminal screen")
public class ClearCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        return 0;
    }
}
