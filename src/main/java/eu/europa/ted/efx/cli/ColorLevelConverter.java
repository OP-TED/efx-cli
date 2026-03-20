package eu.europa.ted.efx.cli;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.pattern.ClassicConverter;

public class ColorLevelConverter extends ClassicConverter {

    private static final String RESET = "\033[0m";
    private static final String RED = "\033[31m";
    private static final String ORANGE = "\033[38;5;208m";
    private static final String BLUE = "\033[34m";

    @Override
    public String convert(final ILoggingEvent event) {
        final Level level = event.getLevel();
        final String label = String.format("%-5s", level);

        if (Level.ERROR.equals(level)) {
            return RED + label + RESET;
        } else if (Level.WARN.equals(level)) {
            return ORANGE + label + RESET;
        } else if (Level.INFO.equals(level)) {
            return BLUE + label + RESET;
        }
        return label;
    }
}
