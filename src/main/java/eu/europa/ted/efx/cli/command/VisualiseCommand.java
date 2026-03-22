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
package eu.europa.ted.efx.cli.command;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ted.eforms.sdk.SdkConstants;
import eu.europa.ted.eforms.NoticeDocument;
import eu.europa.ted.eforms.viewer.NoticeViewer;
import eu.europa.ted.eforms.viewer.NoticeViewerConstants;
import eu.europa.ted.eforms.viewer.enums.ProfilerConfig;
import eu.europa.ted.eforms.viewer.util.xml.TranslationUriResolver;
import eu.europa.ted.efx.cli.CliCommand;
import eu.europa.ted.efx.cli.shell.LoggingConfigurator;
import eu.europa.ted.efx.cli.shell.SessionContext;
import eu.europa.ted.efx.cli.shell.Spinner;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "visualise", description = "Render an XML notice as HTML")
public class VisualiseCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(VisualiseCommand.class);

    @ParentCommand
    CliCommand parent;

    @Option(names = { "-n", "--notice" }, description = "XML notice file to render")
    Path noticeFile;

    @Option(names = { "-l", "--language" }, description = "Language code (e.g. en, fr)")
    String language;

    @Option(names = { "-v", "--sdk-version" }, description = "eForms SDK version")
    String sdkVersion;

    @Option(names = { "-p", "--sdk-path" }, description = "Path to eForms SDK root")
    Path sdkPath;

    @Option(names = { "--view-id" }, description = "Override view template (defaults to notice subtype)")
    String viewId;

    @Option(names = { "-o", "--output" }, description = "Output HTML file path")
    Path outputFile;

    @Option(names = { "--profile" }, description = "Enable profiling: efx, xslt, all (default: all)",
            arity = "0..1", defaultValue = Option.NULL_VALUE, fallbackValue = "all")
    String profile;

    @Option(names = { "-f", "--force" }, description = "Force rebuild of XSL templates")
    boolean force;

    @Override
    public Integer call() throws Exception {
        this.hydrateFromSession();

        final String validationError = this.validateInputs();
        if (validationError != null) {
            logger.error(validationError);
            return 1;
        }

        try {
            final NoticeDocument notice = new NoticeDocument(this.noticeFile);

            if (this.viewId == null) {
                this.viewId = notice.getNoticeSubType();
            }

            if (this.sdkPath == null) {
                this.sdkPath = SdkConstants.DEFAULT_SDK_ROOT;
            }

            System.out.println("Rendering notice: " + this.noticeFile);
            System.out.println("Language: " + this.language + ", View: " + this.viewId);

            final SessionContext session = SessionContext.instance();
            final Set<ProfilerConfig> profilerConfig = this.profile != null
                    ? ProfilerConfig.parseOptions(this.profile) : Set.of();
            final NoticeViewer viewer = NoticeViewer.Builder.create()
                    .withAllowSnapshots(session.snapshots())
                    .withXsltProfiler(ProfilerConfig.isXsltProfilerEnabled(profilerConfig))
                    .withEfxProfiler(ProfilerConfig.isEfxProfilerEnabled(profilerConfig))
                    .withUriResolver(new TranslationUriResolver(
                            notice.getEformsSdkVersion(), this.sdkPath))
                    .build();

            final Path htmlFile;
            try (Spinner ignored = new Spinner("Generating HTML...")) {
                htmlFile = viewer.generateHtmlFile(
                        this.language,
                        this.viewId,
                        notice,
                        this.resolveOutputFile(),
                        this.sdkPath,
                        NoticeViewerConstants.DEFAULT_TRANSLATOR_OPTIONS.getDecimalFormat(),
                        this.force);
            }

            System.out.println("HTML written to: " + htmlFile);
            return 0;
        } catch (final Exception e) {
            logger.error("Visualisation failed: {}", e.getMessage());
            logger.debug("Stack trace:", e);
            return 1;
        }
    }

    private void hydrateFromSession() {
        final SessionContext session = SessionContext.instance();

        if (this.parent != null && this.parent.verbose) {
            session.setVerbose(true);
            LoggingConfigurator.instance().enableDebug();
        } else if (session.verbose()) {
            LoggingConfigurator.instance().enableDebug();
        }

        if (this.noticeFile == null) {
            this.noticeFile = session.notice();
        } else {
            session.setNotice(this.noticeFile);
        }

        if (this.language == null) {
            this.language = session.language();
        }

        if (this.sdkVersion == null) {
            this.sdkVersion = session.sdkVersion();
        } else {
            session.setSdkVersion(this.sdkVersion);
        }

        if (this.sdkPath == null) {
            this.sdkPath = session.sdkPath();
        } else {
            session.setSdkPath(this.sdkPath);
        }
    }

    private String validateInputs() {
        if (this.noticeFile == null) {
            return "Notice file is required (--notice or set via 'config notice <path>')";
        }
        if (!Files.exists(this.noticeFile)) {
            return "Notice file not found: " + this.noticeFile;
        }
        if (this.language == null) {
            return "Language is required (--language or set via 'config language <code>')";
        }
        return null;
    }

    private Path resolveOutputFile() throws Exception {
        if (this.outputFile != null) {
            Files.createDirectories(this.outputFile.getParent());
            return this.outputFile;
        }
        final Path outputDir = Path.of("output", "html");
        Files.createDirectories(outputDir);
        return outputDir.resolve(this.viewId + "-" + this.language + ".html");
    }
}
