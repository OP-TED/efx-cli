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
package eu.europa.ted.efx.cli.shell;

import java.nio.file.Path;
import java.util.Objects;

import eu.europa.ted.eforms.sdk.SdkConstants;
import eu.europa.ted.efx.cli.sdk.LabelResolver;

/**
 * Holds session-level settings that persist across commands within an interactive shell session.
 */
public final class SessionContext {

    private static final SessionContext INSTANCE = new SessionContext();

    private String sdkVersion;
    private Path sdkPath = SdkConstants.DEFAULT_SDK_ROOT;
    private String mode = "phpure";
    private String language = "en";
    private Path notice;
    private Path templatesRoot;
    private boolean snapshots;
    private boolean verbose;
    private boolean labels;
    private LabelResolver cachedLabelResolver;

    private SessionContext() {
    }

    public static SessionContext instance() {
        return INSTANCE;
    }

    public String sdkVersion() {
        return this.sdkVersion;
    }

    public void setSdkVersion(final String sdkVersion) {
        if (!Objects.equals(this.sdkVersion, sdkVersion)) {
            this.sdkVersion = sdkVersion;
            this.cachedLabelResolver = null;
        }
    }

    public Path sdkPath() {
        return this.sdkPath;
    }

    public void setSdkPath(final Path sdkPath) {
        if (!Objects.equals(this.sdkPath, sdkPath)) {
            this.sdkPath = sdkPath;
            this.cachedLabelResolver = null;
        }
    }

    public String mode() {
        return this.mode;
    }

    public void setMode(final String mode) {
        this.mode = mode;
    }

    public String language() {
        return this.language;
    }

    public void setLanguage(final String language) {
        if (!Objects.equals(this.language, language)) {
            this.language = language;
            this.cachedLabelResolver = null;
        }
    }

    public Path notice() {
        return this.notice;
    }

    public void setNotice(final Path notice) {
        this.notice = notice;
    }

    public Path templatesRoot() {
        return this.templatesRoot;
    }

    public void setTemplatesRoot(final Path templatesRoot) {
        this.templatesRoot = templatesRoot;
    }

    public boolean snapshots() {
        return this.snapshots;
    }

    public void setSnapshots(final boolean snapshots) {
        this.snapshots = snapshots;
    }

    public boolean verbose() {
        return this.verbose;
    }

    public void setVerbose(final boolean verbose) {
        this.verbose = verbose;
    }

    public boolean labels() {
        return this.labels;
    }

    public void setLabels(final boolean labels) {
        this.labels = labels;
    }

    public LabelResolver labelResolver() {
        if (this.cachedLabelResolver == null) {
            this.cachedLabelResolver = new LabelResolver(this.sdkVersion, this.sdkPath, this.language);
        }
        return this.cachedLabelResolver;
    }

    public String summary() {
        final StringBuilder sb = new StringBuilder();
        if (this.sdkVersion != null) {
            sb.append("sdk:").append(this.sdkVersion).append(" | ");
        }
        sb.append(this.mode);
        sb.append(" | ").append(this.language);
        if (this.verbose) {
            sb.append(" | verbose");
        }
        return sb.toString();
    }
}
