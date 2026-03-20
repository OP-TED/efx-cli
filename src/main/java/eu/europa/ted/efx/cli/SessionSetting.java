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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Enumerates all session settings, their allowed values, and how to read/write them.
 * Single source of truth for config metadata — used by {@link ConfigCommand},
 * tab completion in the shell, and the syntax highlighter.
 */
public enum SessionSetting {

    SDK_VERSION("sdk-version", Collections.emptyList(), false,
            ctx -> objectToString(ctx.sdkVersion()),
            (ctx, val) -> ctx.setSdkVersion(val)),

    SDK_PATH("sdk-path", Collections.emptyList(), true,
            ctx -> objectToString(ctx.sdkPath()),
            (ctx, val) -> ctx.setSdkPath(Path.of(val))),

    MODE("mode", Arrays.asList("phpure", "schxslt"), false,
            SessionContext::mode,
            (ctx, val) -> ctx.setMode(val)),

    VERBOSE("verbose", Arrays.asList("true", "false"), false,
            ctx -> String.valueOf(ctx.verbose()),
            (ctx, val) -> ctx.setVerbose(Boolean.parseBoolean(val)));

    private final String key;
    private final List<String> allowedValues;
    private final boolean pathValued;
    private final Function<SessionContext, String> getter;
    private final BiConsumer<SessionContext, String> setter;

    SessionSetting(final String key, final List<String> allowedValues, final boolean pathValued,
            final Function<SessionContext, String> getter,
            final BiConsumer<SessionContext, String> setter) {
        this.key = key;
        this.allowedValues = allowedValues;
        this.pathValued = pathValued;
        this.getter = getter;
        this.setter = setter;
    }

    public String key() {
        return this.key;
    }

    public List<String> allowedValues() {
        return this.allowedValues;
    }

    public boolean isPathValued() {
        return this.pathValued;
    }

    public String get(final SessionContext session) {
        return this.getter.apply(session);
    }

    public void set(final SessionContext session, final String value) {
        this.setter.accept(session, value);
    }

    public boolean accepts(final String value) {
        return this.allowedValues.isEmpty() || this.allowedValues.contains(value);
    }

    public static SessionSetting forKey(final String key) {
        for (final SessionSetting setting : values()) {
            if (setting.key.equals(key)) {
                return setting;
            }
        }
        return null;
    }

    private static String objectToString(final Object value) {
        return value != null ? value.toString() : null;
    }
}
