/*
 * Copyright 2026 European Union
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
