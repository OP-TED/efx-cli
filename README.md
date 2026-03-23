# EFX CLI

Command Line Interface for the EFX Toolkit.

## Requirements

The EFX CLI requires Java 11 or later.

## Building

```bash
mvn clean package
```

This will create an executable jar in the `target` directory.

## Usage

### Interactive Shell

Run without arguments to start the interactive shell:

```bash
java -jar target/efx-cli-1.0.0-SNAPSHOT.jar
```

The shell provides tab completion, command history (persisted in `~/.efx-cli/history`), syntax highlighting, and auto-suggestions from history.

### One-Shot Mode

Pass a command directly:

```bash
java -jar target/efx-cli-1.0.0-SNAPSHOT.jar validate --rules rules.efx --notice notice.xml -v 2.0.0
```

### Commands

#### `validate`

Validate an XML notice against EFX rules.

```
validate --rules <file> --notice <file> [options]
```

| Option | Description |
|--------|-------------|
| `-r`, `--rules` | EFX rules file |
| `-s`, `--schematron` | Pre-compiled Schematron file (skips EFX translation) |
| `-n`, `--notice` | XML notice file to validate (required) |
| `-v`, `--sdk-version` | eForms SDK version (e.g. `2.0.0`) |
| `-p`, `--sdk-path` | Path to eForms SDK root |
| `-e`, `--endpoint` | Runtime endpoint URL override (e.g. `default=http://localhost:8080/v1`) |
| `-o`, `--output` | Write SVRL report to file (default: `output/<notice-name>.svrl`) |
| `--mode` | Validation mode: `phpure` (default) or `schxslt` |
| `--mock` | Start a built-in mock API server (`true`, `false`, or `error`) |

Either `--rules` or `--schematron` is required. When using `--rules`, `--sdk-version` is also required (or set via `config`).

The SVRL report is always written to the `output/` directory by default. Use `--output` to override the path.

When `labels` is enabled (via `config labels true`), validation messages are resolved to human-readable text using the SDK's translation files.

**Examples:**

```bash
# Validate with EFX rules (SVRL written to output/notice.svrl)
validate --rules rules.efx --notice notice.xml -v 2.0.0

# Validate with a pre-compiled Schematron
validate --schematron validation.sch --notice notice.xml

# Validate with a mock API server (dynamic rules return true)
validate --rules rules.efx --notice notice.xml -v 2.0.0 --mock

# Validate with mock returning false (dynamic rules fail)
validate --rules rules.efx --notice notice.xml -v 2.0.0 --mock false

# Validate using SchXSLT mode
validate --rules rules.efx --notice notice.xml -v 2.0.0 --mode schxslt

# Write SVRL report to a specific file
validate --rules rules.efx --notice notice.xml -v 2.0.0 -o report.svrl
```

#### `translate-rules`

Translate EFX rules to Schematron.

```
translate-rules --input <file> --output <dir> [options]
```

| Option | Description |
|--------|-------------|
| `-i`, `--input` | Input EFX rules file (required) |
| `-o`, `--output` | Output directory (required) |
| `-v`, `--sdk-version` | eForms SDK version |
| `-p`, `--sdk-path` | Path to eForms SDK root |

#### `config`

Show or set session configuration (interactive shell only).

```
config                          # show all settings
config <key>                    # show a single setting
config <key> <value>            # set a value
```

Available settings:

| Setting | Description | Default |
|---------|-------------|---------|
| `sdk-version` | eForms SDK version | (not set) |
| `sdk-path` | Path to eForms SDK root | `eforms-sdk` |
| `mode` | Validation mode (`phpure` or `schxslt`) | `phpure` |
| `language` | Language for label resolution (e.g. `en`, `fr`) | `en` |
| `verbose` | Enable debug logging | `false` |
| `labels` | Resolve rule labels to human-readable text | `false` |

Settings persist for the duration of the shell session. Options passed to commands (e.g. `--sdk-version`) update the session automatically. Current values are shown in the right prompt.

#### `clear`

Clear the terminal screen.

### Global Options

| Option | Description |
|--------|-------------|
| `--verbose` | Enable verbose (debug) output |
| `--help` | Show help |
| `--version` | Show version |

### Keyboard Shortcuts (Interactive Shell)

| Key | Action |
|-----|--------|
| Tab | Auto-complete commands, options, file paths |
| Up/Down | Navigate command history |
| Right Arrow | Accept auto-suggestion |
| Esc | Clear the current line |
| Ctrl+D | Exit |
| Ctrl+C | Cancel current input |

## License

_Copyright 2025 European Union_

_Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission –
subsequent versions of the EUPL (the "Licence");_
_You may not use this work except in compliance with the Licence. You may obtain [a copy of the Licence here](LICENSE)._
_Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for the specific language governing permissions and limitations under the Licence._
