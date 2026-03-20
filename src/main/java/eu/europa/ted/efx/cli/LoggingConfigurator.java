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
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.OutputStreamAppender;
import org.slf4j.LoggerFactory;

/**
 * Configures logback logging levels and patterns at runtime.
 */
final class LoggingConfigurator {

    private LoggingConfigurator() {
    }

    static void enableDebug() {
        configure(Level.DEBUG, "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
    }


    static void disableDebug() {
        configure(Level.WARN, "%colorLevel %msg%n");

        final Logger cliLogger = (Logger) LoggerFactory.getLogger("eu.europa.ted.efx.cli");
        cliLogger.setLevel(Level.INFO);
    }


    private static void configure(final Level level, final String pattern) {
        final Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(level);

        final Appender<ILoggingEvent> appender = root.getAppender("STDOUT");
        if (appender instanceof OutputStreamAppender) {
            final PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(root.getLoggerContext());
            encoder.setPattern(pattern);
            encoder.start();
            ((OutputStreamAppender<ILoggingEvent>) appender).setEncoder(encoder);
        }
    }
}
