# EFX CLI 0.1.0 Release Notes

_The EFX CLI is a command line interface for the EFX Toolkit. It provides an interactive shell and one-shot commands for translating, validating, and visualising eForms notices using EFX._

---

## Initial release

This is the first release of the EFX CLI. It provides the following commands:

### `validate`

Validates an XML notice against EFX rules by transpiling them to Schematron and running validation. Supports both ph-schematron (`phpure`) and SchXSLT validation modes. Includes a built-in mock API server for testing dynamic rules. Outputs SVRL validation reports with optional label resolution for human-readable messages.

### `visualise`

Renders an XML notice as HTML using EFX view templates from the SDK. Supports language selection and view template overrides. Caches compiled XSL templates for performance.

### `translate-rules`

Transpiles EFX rules files to Schematron output.

### `extract-dependencies`

Extracts field and codelist dependencies from EFX rules files into a dependency graph.

### `config`

Session configuration for the interactive shell. Settings (SDK version, notice path, language, validation mode, etc.) persist for the duration of the session and are automatically updated by command options.

### Interactive shell

The CLI provides an interactive shell with tab completion, command history, syntax highlighting, and auto-suggestions. Session settings are displayed in the right prompt.

---

This version depends on:

- [EFX Toolkit for Java](https://github.com/OP-TED/efx-toolkit-java) version 2.0.0-alpha.6.
- [eForms Notice Viewer](https://github.com/OP-TED/eforms-notice-viewer) version 0.12.0.
