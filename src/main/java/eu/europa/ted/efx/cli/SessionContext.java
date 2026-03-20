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

import eu.europa.ted.eforms.sdk.SdkConstants;

/**
 * Holds session-level settings that persist across commands within an interactive shell session.
 */
public final class SessionContext {

    private static final SessionContext INSTANCE = new SessionContext();

    private String sdkVersion;
    private Path sdkPath = SdkConstants.DEFAULT_SDK_ROOT;
    private String mode = "phpure";
    private boolean verbose;

    private SessionContext() {
    }

    public static SessionContext instance() {
        return INSTANCE;
    }

    public String sdkVersion() {
        return this.sdkVersion;
    }

    public void setSdkVersion(final String sdkVersion) {
        this.sdkVersion = sdkVersion;
    }

    public Path sdkPath() {
        return this.sdkPath;
    }

    public void setSdkPath(final Path sdkPath) {
        this.sdkPath = sdkPath;
    }

    public String mode() {
        return this.mode;
    }

    public void setMode(final String mode) {
        this.mode = mode;
    }

    public boolean verbose() {
        return this.verbose;
    }

    public void setVerbose(final boolean verbose) {
        this.verbose = verbose;
    }

    public String rightPrompt() {
        final StringBuilder sb = new StringBuilder();
        sb.append("\033[90m");
        boolean first = true;
        if (this.sdkVersion != null) {
            sb.append("sdk:").append(this.sdkVersion);
            first = false;
        }
        if (!first) {
            sb.append(" | ");
        }
        sb.append(this.mode);
        if (this.verbose) {
            sb.append(" | verbose");
        }
        sb.append("\033[0m");
        return sb.toString();
    }
}
