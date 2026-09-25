# Installation guide for efx-cli

`efx-cli` is a command-line tool for working with the eForms SDK. It can render a 
notice as HTML (`visualise`), translate EFX rules to Schematron 
(`translate-rules`), validate a notice against Schematron rules (`validate`), and 
extract field and codelist dependencies from EFX rules into a JSON dependency 
graph (`extract-dependencies`).

This guide describes the one-time setup you need before the commands will work. 

### Contents

- Prerequisites
- How efx-cli uses Maven and the eForms SDK
- Step 1 — Install a Java Development Kit
- Step 2 — Obtain the efx-cli tool
- Step 3 — Configure network access
- Step 4 — Make an eForms SDK available
- Running the commands
- Troubleshooting
- Known limitations

## Prerequisites

- A **Java Development Kit (JDK)**, version 17 or later.
- The **Maven** build and dependency-management tool.
- The [efx-cli](https://github.com/OP-TED/efx-cli) tool.

### Prerequisites at a glance

Run these checks in a terminal. Each should print a version; if any command is not
recognised, follow the corresponding step below.

```
java -version      # JDK 17+ (Step 1)
javac -version     # same version as java (Step 1)
mvn -version       # Apache Maven, reporting your JDK (Step 2)
git --version      # to clone the source (Step 2)
```

## How efx-cli uses Maven and the eForms SDK

**Maven** is the build and dependency-management tool that `efx-cli` is built with.
It is required in two occasions:

1. It is used once to build `efx-cli` into a runnable `.jar` (Step 2).
2. The first time a command needs the eForms SDK or a supporting library, Maven
   downloads it from online repositories and stores a copy on your machine.

That local copy lives in a folder called the local repository, at:

- Windows: `C:\Users\<username>\.m2\repository`
- macOS / Linux: `~/.m2/repository`

Maven's own configuration lives in a file called `settings.xml` in the same
`.m2` folder. This is the file you edit to configure network access in Step 3.


## Step 1 — Install a Java Development Kit

1. Install a JDK (Java Development Kit).
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
   a suitable location.

OR install it from the command line:

   ```
   apt install maven
   ```

2. Add it's `bin` directory to your `PATH`, then confirm it uses your JDK:

   ```
   mvn -version
   ```

## Step 3 — Configure network access

If you have a direct internet connection, skip this step.

On a corporate network you usually reach the internet through an HTTP proxy.
Maven must be configured to use the proxy for HTTPS connections. 

Create or edit `settings.xml` in your `.m2` folder and add a `<proxies>` block with
two entries — one `http`, one `https` — pointing at the same proxy:

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

## Step 4 — Obtain the efx-cli 

[efx-cli](https://github.com/OP-TED/efx-cli) is distributed as source and built 
into a runnable `.jar`.

1. Get the source and build it:

   ```
   git clone https://github.com/OP-TED/efx-cli
   cd efx-cli
   mvn clean package
   ```

3. After a successful build, the runnable file is:

   ```
   target/efx-cli-<version>.jar
   ```

## Step 4 — Make an eForms SDK available

Most commands need an eForms SDK (its fields, codelists, view templates and
Schematron rules). `efx-cli` obtains the SDK in one of two ways:

- **Automatically** — it downloads the SDK the first time it is needed ; or
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
--sdk-path C:\path\to\eforms-sdk --sdk-version 2.0.0-SNAPSHOT
```

> **NOTE**
> The value of `--sdk-version` must match the sub-folder name exactly. If this is
> not the case, the command fails with
> *"Failed to instantiate Symbol Resolver for SDK version ..."*.


## Command SDK requirements

The different commands have different requirements for the SDK files needed and their locations.

| command | files/folders required | notes |
| --- | --- | --- |
| validate --schematron | complete-validation.sch and the other .sch files | |
| validate --rules | codelists, fields, notice-types | uses --sdk-path and --sdk-version, does not check version inside SDK |
| translate-rules | codelists, fields, notice-types | uses --sdk-path and --sdk-version, does not check version inside SDK |
| extract-dependencies | codelists, fields, notice-types | uses --sdk-path and --sdk-version, does not check version inside SDK |
| visualise | ? | ignores --sdk-path and --sdk-version, uses SDK declared inside the notice XML and the Maven metadata in .m2 repository, and tries to resolve to a Maven artifact. This method currently fails. |


## Running the commands

The examples below assume `efx-cli-<version>.jar` and a JDK are on your machine.
Replace paths and the version as appropriate.

**Translate EFX rules to Schematron**

```
java -jar efx-cli-<version>.jar translate-rules \
  --input rules.efx \
  --input C:\path\to\eforms-sdk -v 2.0.0-SNAPSHOT \
  --output output-folder
```

The generated `.sch` files are written to the `out` directory.

**Extract Dependencies from EFX rules**

```
java -jar efx-cli-<version>.jar extract-dependencies \
  --input rules.efx \
  --input C:\path\to\eforms-sdk -v 2.0.0-SNAPSHOT \
  --output json-file
```

The generated JSON is written to the `output` file.

**Validate a notice against Schematron**

```
java -jar efx-cli-<version>.jar validate \
  --notice notice.xml \
  --schematron complete-validation.sch \
  --output report.svrl
```


The raw SVRL report is written to `report.svrl`. Use `--mode schxslt` to select the
alternative validation engine if needed (the default is `phpure`).

**Validate a notice against EFX rules**

```
java -jar efx-cli-<version>.jar validate \
  --notice notice.xml \
  --rules rules.efx \
  --output report.svrl
```

The raw SVRL report is written to `report.svrl`. Use `--mode schxslt` to select the
alternative validation engine if needed (the default is `phpure`).

**Visualise a notice as HTML**

```
java -jar efx-cli-<version>.jar visualise \
  --notice notice.xml \
  --input C:\path\to\eforms-sdk --language en \
  --output notice.html
```
> This has not been successfully tested

## Troubleshooting

| Message | Likely cause | What to do |
| --- | --- | --- |
| `Connect timed out` while downloading | Proxy not configured for `https` | Add the `https` `<proxy>` entry (Step 3) |
| `No artifacts found for SDK <x.y.z>` (during build/translate) | The version is not reachable from the configured repositories, or the proxy is not applied | Confirm Step 3; confirm the version exists in a repository you can reach |
| `--sdk-version is required` | No version supplied | Pass `-v <version>`, or set `config sdk-version <version>` |
| `Failed to instantiate Symbol Resolver for SDK version [X]` | `--sdk-path` is not a root containing a `<version>` sub-folder, or `--sdk-version` does not match the sub-folder name | Fix the SDK layout / version (Step 4) |
| `visualise`: `Visualisation failed: Cannot invoke "java.nio.file.Path.getFileSystem()" because "path" is null` | `--sdk-path` is not a root containing a `<version>` sub-folder, or `--sdk-version` does not match the sub-folder name | Fix the SDK layout / version (Step 4) |
| `visualise`: `No artifacts found for SDK <x.y>` | The notice's declared SDK version cannot be resolved from the repositories `efx-cli` consults | See Known limitations |


## Known limitations

- **`visualise` SDK resolution.** `visualise` determines the SDK version from the
  notice itself (it ignores the --sdk-version parameter) and tries to resolve the SDK 
  artifact from a limited set of repositories:
  - the Maven metadata in .m2/repository/eu/europa/ted/eforms/eforms-sdk
  - the [Maven Central snapshots](https://central.sonatype.com/repository/maven-snapshots/eu/europa/ted/eforms/eforms-sdk/maven-metadata.xml)

  Notices whose SDK version is not available in those repositories cannot currently
  be rendered.

> **NOTE**
> This method currently fails, as the resolver cannot match a minor version of the SDK (e.g. "1.14") with an artifact version (e.g. "1.14.2").
