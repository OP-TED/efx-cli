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
    private String mode = "schxslt";
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
