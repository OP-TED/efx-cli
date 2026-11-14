# Installation guide for efx-cli

`efx-cli` is a command-line tool for working with the eForms SDK. It can render a
notice as HTML (`visualise`), translate EFX rules to Schematron
(`translate-rules`), validate a notice against Schematron rules (`validate`), and
extract field and codelist dependencies from EFX rules into a JSON dependency
graph (`extract-dependencies`).

This guide describes the one-time setup you need before the commands will work.

### Contents

- [Prerequisites](#prerequisites)
- [How efx-cli uses Maven and the eForms SDK](#how-efx-cli-uses-maven-and-the-eforms-sdk)
- [Step 1 — Install a Java Development Kit](#step-1--install-a-java-development-kit)
- [Step 2 — Install Apache Maven](#step-2--install-apache-maven)
- [Step 3 — Configure Maven](#step-3--configure-maven)
- [Step 4 — Build efx-cli](#step-4--build-efx-cli)
- [Step 5 — Make an eForms SDK available](#step-5--make-an-eforms-sdk-available)
- [Command SDK requirements](#command-sdk-requirements)
- [Running the commands](#running-the-commands)
- [Troubleshooting](#troubleshooting)

## Prerequisites

- A **Java Development Kit (JDK)**, version 11 or later.
- The **Maven** build and dependency-management tool.
- **Git**, to get the source code.
- The [efx-cli](https://github.com/OP-TED/efx-cli) source code.

### Prerequisites at a glance

Run these checks in a terminal. Each should print a version; if any command is not
recognised, follow the corresponding step below.

```
java -version      # JDK 11 or later (Step 1)
javac -version     # same version as java (Step 1)
mvn -version       # Apache Maven, reporting your JDK (Step 2)
git --version      # to get the source code (Step 4)
```

## How efx-cli uses Maven and the eForms SDK

**Maven** is the build and dependency-management tool that `efx-cli` is built with.
It is required on two occasions:

1. It is used once to build `efx-cli` into a runnable `.jar` (Step 4).
2. The first time a command needs the eForms SDK or a supporting library, Maven
   downloads it from online repositories and stores a copy on your machine.

That local copy lives in a folder called the local repository, at:

- Windows: `C:\Users\<username>\.m2\repository`
- macOS / Linux: `~/.m2/repository`

Maven's own configuration lives in a file called `settings.xml` in the same
`.m2` folder. This is the file you edit in Step 3.

## Step 1 — Install a Java Development Kit

1. Install a JDK (Java Development Kit), version 11 or later.
2. Set the `JAVA_HOME` environment variable to the JDK installation folder, and add
   its `bin` directory to your `PATH`.
3. Open a new terminal and confirm both tools are found:

   ```
   java -version
   javac -version
   ```

Both commands should report the same version. If they are not recognised, reset
`JAVA_HOME` and `PATH`, then open a fresh terminal.

## Step 2 — Install Apache Maven

1. Download [Apache Maven](https://maven.apache.org/install.html) and unzip it to
   a suitable location, then add its `bin` directory to your `PATH`.

   On Debian or Ubuntu you can instead install it with the package manager, which
   requires administrative privileges:

   ```
   sudo apt install maven
   ```

2. Confirm that Maven is found and uses your JDK:

   ```
   mvn -version
   ```

## Step 3 — Configure Maven

Create `settings.xml` in your `.m2` folder if it does not exist yet. Steps 3.1 and
3.2 each add a block inside its `<settings>` element.

### 3.1 — Add the snapshot repository

The development version of `efx-cli` depends on snapshot versions of the EFX
Toolkit and the Notice Viewer. These are published in the Maven Central snapshot
repository, which Maven does not use unless you configure it. Without this, the
build in Step 4 fails with *"Could not find artifact
eu.europa.ted.eforms:efx-toolkit-java:jar:2.0.0-SNAPSHOT"*.

Add this to `settings.xml`:

```xml
<settings>
  <profiles>
    <profile>
      <id>central-snapshots</id>
      <repositories>
        <repository>
          <id>central-snapshots</id>
          <url>https://central.sonatype.com/repository/maven-snapshots/</url>
          <releases><enabled>false</enabled></releases>
          <snapshots><enabled>true</enabled></snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>central-snapshots</activeProfile>
  </activeProfiles>
</settings>
```

### 3.2 — Configure network access

If you have a direct internet connection, skip this step.

On a corporate network you usually reach the internet through an HTTP proxy.
Maven must be configured to use the proxy for HTTPS connections.

Add a `<proxies>` block to `settings.xml` with two entries — one `http`, one
`https` — pointing at the same proxy:

```xml
<settings>
  <proxies>
    <proxy>
      <id>corp-http</id>
      <active>true</active>
      <protocol>http</protocol>
      <host>PROXY_HOST</host>
      <port>PROXY_PORT</port>
      <username>USERNAME</username>
      <password>PASSWORD</password>
      <nonProxyHosts>*.internal.example</nonProxyHosts>
    </proxy>
    <proxy>
      <id>corp-https</id>
      <active>true</active>
      <protocol>https</protocol>
      <host>PROXY_HOST</host>
      <port>PROXY_PORT</port>
      <username>USERNAME</username>
      <password>PASSWORD</password>
      <nonProxyHosts>*.internal.example</nonProxyHosts>
    </proxy>
  </proxies>
</settings>
```

Replace `PROXY_HOST`, `PROXY_PORT`, `USERNAME` and `PASSWORD` with the values for
your network, and list any internal hosts that must be reached directly in
`<nonProxyHosts>`.

> **NOTE**
> `settings.xml` stores the proxy password in plain text. Protect the file with
> appropriate permissions, and rotate the password if your organisation requires
> it.

Incorrect or missing proxy settings will often result in
a *"connection timed out"* error message.

## Step 4 — Build efx-cli

[efx-cli](https://github.com/OP-TED/efx-cli) is distributed as source and built
into a runnable `.jar`.

1. Get the source and build it:

   ```
   git clone https://github.com/OP-TED/efx-cli
   cd efx-cli
   mvn clean package
   ```

2. After a successful build, the runnable file is:

   ```
   target/efx-cli-<version>.jar
   ```

## Step 5 — Make an eForms SDK available

Most commands need an eForms SDK (its fields, codelists, view templates and
Schematron rules). `efx-cli` obtains the SDK in one of two ways:

- **Automatically** — it downloads the SDK the first time it is needed; or
- **From disk** — you provide the SDK yourself and point `efx-cli` at it.

To provide an SDK from disk, arrange it as a **root folder containing a sub-folder
named exactly after the SDK version**, with the SDK contents inside:

```
eforms-sdk/                 <- the "SDK root" you pass to --sdk-path
  2.0.0-SNAPSHOT/           <- a folder named exactly the SDK version
    fields/
    codelists/
    view-templates/
    schematrons/
    ...
```

You then refer to it with two options: `--sdk-path` (the root) and `--sdk-version`
(the sub-folder name):

```
--sdk-path ./eforms-sdk --sdk-version 2.0.0-SNAPSHOT
```

If you omit `--sdk-path`, `efx-cli` uses a folder named `eforms-sdk` in the current
directory.

> **NOTE**
> The value of `--sdk-version` must match the sub-folder name exactly. If this is
> not the case, the command fails with
> *"Failed to instantiate Symbol Resolver for SDK version ..."*.

## Command SDK requirements

The different commands have different requirements for the SDK files needed and their locations.

| command | files/folders required | notes |
| --- | --- | --- |
| validate --schematron | complete-validation.sch and the other .sch files | |
| validate --rules | codelists, fields, notice-types | uses --sdk-path and --sdk-version; if --sdk-version is omitted, uses the SDK version declared in the notice. Does not check the version inside the SDK |
| translate-rules | codelists, fields, notice-types | uses --sdk-path and --sdk-version, does not check the version inside the SDK |
| extract-dependencies | codelists, fields, notice-types | uses --sdk-path and --sdk-version, does not check the version inside the SDK |
| visualise | the complete SDK | uses --sdk-path as the SDK root, but takes the SDK version from the notice and ignores --sdk-version. If that version is not in the SDK root, it is downloaded there |

## Running the commands

The examples below assume `efx-cli-<version>.jar` and a JDK are on your machine,
and an SDK root in `./eforms-sdk`. Replace paths and the version as appropriate.
Each command is on one line so that it works in any terminal (Bash, PowerShell or
the Windows command prompt).

**Translate EFX rules to Schematron**

```
java -jar efx-cli-<version>.jar translate-rules --input rules.efx --sdk-path ./eforms-sdk --sdk-version 2.0.0-SNAPSHOT --output output-folder
```

The generated `.sch` files are written to `output-folder`.

**Extract dependencies from EFX rules**

```
java -jar efx-cli-<version>.jar extract-dependencies --input rules.efx --sdk-path ./eforms-sdk --sdk-version 2.0.0-SNAPSHOT --output dependencies.json
```

The generated JSON is written to `dependencies.json`.

**Validate a notice against Schematron**

```
java -jar efx-cli-<version>.jar validate --notice notice.xml --schematron complete-validation.sch --output report.svrl
```

The raw SVRL report is written to `report.svrl`. Use `--mode schxslt` to select the
alternative validation engine if needed (the default is `phpure`).

**Validate a notice against EFX rules**

```
java -jar efx-cli-<version>.jar validate --notice notice.xml --rules rules.efx --sdk-path ./eforms-sdk --sdk-version 2.0.0-SNAPSHOT --output report.svrl
```

`--sdk-version` can be omitted if the notice declares its SDK version, and
`--sdk-path` if the SDK root is `./eforms-sdk`. The raw SVRL report is written to
`report.svrl`. Use `--mode schxslt` to select the alternative validation engine if
needed (the default is `phpure`).

**Visualise a notice as HTML**

```
java -jar efx-cli-<version>.jar visualise --notice notice.xml --sdk-path ./eforms-sdk --language en --output ./notice.html
```

If you omit `--output`, the HTML is written to the `output/html` folder.

## Troubleshooting

| Message | Likely cause | What to do |
| --- | --- | --- |
| `Could not find artifact eu.europa.ted.eforms:efx-toolkit-java:jar:2.0.0-SNAPSHOT` (during the build) | The snapshot repository is not configured | Add it to `settings.xml` (Step 3.1) |
| `Connect timed out` while downloading | Proxy not configured for `https` | Add the `https` `<proxy>` entry (Step 3.2) |
| `No artifacts found for SDK <x.y.z>` (during build/translate) | The version is not reachable from the configured repositories, or the proxy is not applied | Confirm Step 3; confirm the version exists in a repository you can reach |
| `--sdk-version is required` | No version supplied, and none declared in the notice | Pass `-v <version>`, or set `config sdk-version <version>` |
| `Failed to instantiate Symbol Resolver for SDK version [X]` | `--sdk-path` is not a root containing a `<version>` sub-folder, or `--sdk-version` does not match the sub-folder name | Fix the SDK layout / version (Step 5) |
| `visualise`: `Visualisation failed: Cannot invoke "java.nio.file.Path.getFileSystem()" because "path" is null` | `--output` is a bare file name, such as `notice.html` | Prefix the file name with a folder, such as `./notice.html`, or omit `--output` |
