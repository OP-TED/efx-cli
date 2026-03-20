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

The shell provides tab completion, command history (persisted across sessions), syntax highlighting, and auto-suggestions from history.

### One-Shot Mode

Pass a command directly:

```bash
java -jar target/efx-cli-1.0.0-SNAPSHOT.jar validate --rules rules.efx --notice notice.xml -v 2.0.0
```

### Commands

#### `validate`

Validate an XML notice against EFX rules.

```
validate --rules <file> --notice <file> --sdk-version <version> [options]
```

| Option | Description |
|--------|-------------|
| `-r`, `--rules` | EFX rules file |
| `-s`, `--schematron` | Pre-compiled Schematron file (skips EFX translation) |
| `-n`, `--notice` | XML notice file to validate (required) |
| `-v`, `--sdk-version` | eForms SDK version (e.g. `2.0.0`) |
| `-p`, `--sdk-path` | Path to eForms SDK root |
| `-e`, `--endpoint` | Runtime endpoint URL override (e.g. `default=http://localhost:8080/v1`) |
| `-o`, `--output` | Write raw SVRL report to file |
| `--mode` | Validation mode: `schxslt` (default) or `phpure` |
| `--mock` | Start a built-in mock API server (`true`, `false`, or `error`) |

Either `--rules` or `--schematron` is required. When using `--rules`, `--sdk-version` is also required.

**Examples:**

```bash
# Validate with EFX rules
validate --rules rules.efx --notice notice.xml -v 2.0.0

# Validate with a pre-compiled Schematron
validate --schematron validation.sch --notice notice.xml

# Validate with a mock API server (dynamic rules return true)
validate --rules rules.efx --notice notice.xml -v 2.0.0 --mock

# Validate using ph-schematron-pure mode
validate --rules rules.efx --notice notice.xml -v 2.0.0 --mode phpure

# Write SVRL report to file
validate --rules rules.efx --notice notice.xml -v 2.0.0 -o report.svrl
```

#### `translate-rules`

Translate EFX rules to Schematron.

```
translate-rules --input <file> --output <dir> --sdk-version <version> [options]
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

Available settings: `sdk-version`, `sdk-path`, `mode`, `verbose`.

Settings set via `config` or passed as command options persist for the duration of the shell session and are shown in the right prompt.

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
