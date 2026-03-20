package eu.europa.ted.efx.cli;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "config", description = "Show or set session configuration")
public class ConfigCommand implements Callable<Integer> {

    @Parameters(index = "0", arity = "0..1", description = "Setting name")
    String key;

    @Parameters(index = "1", arity = "0..1", description = "New value")
    String value;

    @Override
    public Integer call() {
        final SessionContext session = SessionContext.instance();

        if (this.key == null) {
            for (final SessionSetting setting : SessionSetting.values()) {
                final String display = setting.get(session);
                System.out.printf("  %-13s : %s%n", setting.key(),
                        display != null ? display : "(not set)");
            }
            return 0;
        }

        final SessionSetting setting = SessionSetting.forKey(this.key);
        if (setting == null) {
            System.out.println("Unknown setting: " + this.key);
            return 1;
        }

        if (this.value == null) {
            final String display = setting.get(session);
            System.out.println(display != null ? display : "(not set)");
            return 0;
        }

        if (!setting.accepts(this.value)) {
            System.out.println("Invalid value: " + this.value
                    + " (expected: " + String.join(" or ", setting.allowedValues()) + ")");
            return 1;
        }

        setting.set(session, this.value);
        if (setting == SessionSetting.VERBOSE) {
            if (session.verbose()) {
                LoggingConfigurator.enableDebug();
            } else {
                LoggingConfigurator.disableDebug();
            }
        }
        System.out.println(this.key + " = " + this.value);
        return 0;
    }
}
