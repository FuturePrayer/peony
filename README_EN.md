<p align="center">
  <img src="logo.png" alt="Peony" width="128" height="128">
</p>

<h1 align="center">Peony</h1>

<p align="center">
  <strong>A command-line launcher for the Java ecosystem, providing an npx-like experience</strong>
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License"></a>
  <a href="README.md"><img src="https://img.shields.io/badge/中文-README-lightgrey.svg" alt="中文"></a>
  <img src="https://img.shields.io/badge/JDK-21-green.svg" alt="JDK 21">
  <img src="https://img.shields.io/badge/Maven-3.9+-green.svg" alt="Maven">
</p>

---

`peony` is a command-line launcher for the Java ecosystem, aiming to provide an `npx`-like experience:

- Resolve and download runnable artifacts from remote sources
- Run in a temporary workspace by default, with automatic cleanup
- Pass through subprocess arguments and standard I/O streams
- Ideal for quickly launching stdio-based Java MCP Servers or other single-file `.jar` tools

Currently supported:

- GitHub repository latest release `.jar` assets
- GitHub private repository tokens
- `http`, `https`, `socks5` proxies
- Prefix proxies
- GraalVM Native Image builds

Planned sources:

- Gitee release
- Remote Maven repositories
- Other custom distribution sources

## Features

- Minimal dependencies — runtime only requires `jackson-core` and `hutool-http`
- Built on JDK 21 and Hutool HTTP for HTTP, process launching, and configuration loading
- Deletes download workspace by default to avoid leftover files
- Supports configuration via CLI flags, config files, and environment variables
- Supports native executable builds

## Quick Start

### Installation

Download the executable for your platform from [GitHub Releases](https://github.com/suhoan/peony/releases), or download the cross-platform fat jar.

### Command Format

```bash
peony run github <owner/repo> [options] -- [program args]
peony pull github <owner/repo> [options]
```

- `run`: Download and run a jar
- `pull`: Download only, output the absolute path, do not run, keep the workspace

Examples:

```bash
peony run github modelcontextprotocol/servers --asset mcp-server.jar -- --stdio
```

```bash
peony run github owner/private-repo --github-token-env MY_GITHUB_TOKEN -- --port 3000
```

```bash
peony run github owner/repo --proxy socks5://127.0.0.1:1080 --prefix-proxy https://gp-proxy.com/ -- --help
```

```bash
peony pull github owner/repo --asset app.jar
```

```bash
peony run github owner/repo --stable-only --asset app.jar
```

```bash
peony run github owner/repo --release-tag v1.0.0
```

```bash
peony run github owner/repo --jvm-args "-Xmx512m -Dfoo=bar" -- --stdio
```

## CLI Options

| Option | Description |
| --- | --- |
| `--asset <name>` | Specify the asset name to download when the latest release contains multiple `.jar` files |
| `--release-tag <tag>` | Specify the release tag to download (e.g. `v1.0.0`); mutually exclusive with `--stable-only`; pre-release filtering does not apply when a version is specified |
| `--config <path>` | Specify the configuration file path |
| `--java-home <path>` | Specify the JDK path (not needed for `pull`) |
| `--workspace <path>` | Specify the workspace parent directory; a temporary subdirectory will be created under it |
| `--keep-workspace` | Keep the workspace after execution |
| `--stable-only` | Only look for stable releases, ignoring pre-releases; by default pre-releases are included |
| `--jvm-args <args>` | JVM arguments (e.g. `"-Xmx512m -Dfoo=bar"`), only available for `run`; spaces inside quotes are not supported on the command line — use environment variables or config file instead |
| `--proxy <url>` | Specify a proxy, supports `http://`, `https://`, `socks5://` |
| `--prefix-proxy <url>` | Specify a prefix proxy; the final access URL will be `<prefix><real-url>` |
| `--github-token <token>` | Specify the GitHub token directly |
| `--github-token-env <name>` | Specify the environment variable name holding the GitHub token |
| `-h`, `--help` | Print help |

## Configuration Priority

For most configurations, the priority order is:

1. CLI arguments
2. Environment variables
3. Configuration file
4. Default values

For `java.home`, the lookup order is:

1. `--java-home`
2. Config file `java.home`
3. `PEONY_JAVA_HOME`
4. `JAVA_HOME`

If none are available, the `run` command will exit with an error; the `pull` command does not require java home.

For `jvm.args`, the lookup order is:

1. `--jvm-args`
2. `PEONY_JVM_ARGS`
3. Config file `jvm.args`

## Configuration File

Default search order:

1. `peony.properties` in the current directory
2. `.peony.properties` in the user home directory

You can also explicitly specify a path with `--config`.

Example:

```properties
java.home=/usr/lib/jvm/jdk-21
jvm.args=-Xmx512m -Dfoo=bar
workspace.parent=/tmp/peony
github.token.env=PEONY_GITHUB_TOKEN
proxy.url=socks5://127.0.0.1:1080
proxy.prefix=https://gp-proxy.com/
```

Supported configuration keys:

| Key | Description |
| --- | --- |
| `java.home` | JDK root directory |
| `jvm.args` | JVM arguments, supports spaces inside quotes |
| `workspace.parent` | Workspace parent directory |
| `github.token` | GitHub token |
| `github.token.env` | Environment variable name for the GitHub token |
| `proxy.url` | Proxy URL |
| `proxy.prefix` | Prefix proxy URL |

## Environment Variables

| Variable | Description |
| --- | --- |
| `PEONY_JAVA_HOME` | JDK root directory |
| `JAVA_HOME` | JDK root directory fallback |
| `PEONY_JVM_ARGS` | JVM arguments, supports spaces inside quotes |
| `PEONY_GITHUB_TOKEN` | Default GitHub token |
| `GITHUB_TOKEN` | Default GitHub token fallback |
| `PEONY_PROXY` | Default proxy |
| `HTTPS_PROXY` | Default proxy fallback |
| `HTTP_PROXY` | Default proxy fallback |
| `ALL_PROXY` | Default proxy fallback |
| `PEONY_PREFIX_PROXY` | Default prefix proxy |
| `PEONY_WORKSPACE_PARENT` | Default workspace parent directory |

## Workspace & Cleanup Strategy

- `--workspace` refers to the workspace parent directory, not the final working directory
- Each run creates an independent temporary subdirectory, e.g. `peony-xxxxx`
- Downloaded `.jar` files are placed in this temporary subdirectory
- The `run` command deletes the entire temporary subdirectory on exit by default
- The `pull` command keeps the workspace by default
- Use `--keep-workspace` to preserve the directory for troubleshooting

## Proxy

By default, if a regular proxy or prefix proxy is configured:

- The program will use these proxies to make requests first
- If 3 consecutive retries fail to produce the expected result, the proxy is considered unavailable
- Unavailability includes connection timeouts, request exceptions, 4xx/5xx responses, etc.
- The program will automatically fall back to direct connection and retry

### Regular Proxy

Examples:

```bash
--proxy http://127.0.0.1:7890
--proxy https://127.0.0.1:7890
--proxy socks5://127.0.0.1:1080
```

### Prefix Proxy

If the actual access URL is:

```text
https://github.com/example/repo
```

With configuration:

```text
--prefix-proxy https://gp-proxy.com/
```

The final request URL becomes:

```text
https://gp-proxy.com/https://github.com/example/repo
```

This strategy applies to:

- GitHub API requests
- Release asset downloads
- Redirected download requests

## Local Development

This project uses:

- JDK 21
- Maven 3.9+

Windows example:

```powershell
$env:JAVA_HOME="D:\develop\jdk-21"
& "D:\develop\apache-maven-3.9.6\bin\mvn.cmd" "-Dmaven.repo.local=D:/maven/repo" test
```

Build a runnable fat jar:

```powershell
$env:JAVA_HOME="D:\develop\jdk-21"
& "D:\develop\apache-maven-3.9.6\bin\mvn.cmd" "-Dmaven.repo.local=D:/maven/repo" package
```

Output location:

```text
target/peony-<version>.jar
```

## GraalVM Native Image Build

The project has `org.graalvm.buildtools:native-maven-plugin` configured in the `native` profile.

### Local Prerequisites

1. Install GraalVM for JDK 21
2. Install the `native-image` component
3. Use GraalVM as `JAVA_HOME`
4. On Windows, install Visual Studio 2022 C++ Build Tools, or run the build from a Native Tools Command Prompt

Example:

```powershell
$env:JAVA_HOME="D:\develop\graalvm-community-openjdk-21.0.1+12.1"
& "D:\develop\apache-maven-3.9.6\bin\mvn.cmd" "-Dmaven.repo.local=D:/maven/repo" -Pnative native:compile
```

The output is typically located at:

```text
target/peony.exe
```

Notes:

- A regular `mvn test` or `mvn package` will not load the native plugin
- The native build is only triggered when `-Pnative native:compile` is explicitly specified
- The current configuration uses `--no-fallback`
- The native build has explicitly enabled `http` and `https` URL protocols to avoid runtime errors when accessing remote addresses
- If a Windows build is missing `vcvarsall.bat`, Visual Studio C++ native toolchain is not installed
- The current native profile disables the GraalVM reachability metadata repository to avoid GraalVM/metadata schema compatibility issues across different runners

## GitHub Release Automation

The repository includes a GitHub Actions workflow that automatically publishes a release when:

1. A tag is pushed
2. The tag name exactly matches the `<version>` in `pom.xml`
3. The commit the tag points to is included in the default branch `main` or `master`

Workflow behavior:

1. Build fat jar
2. Build GraalVM native executables
3. Automatically create or update a GitHub Release
4. Upload jar, Linux native, and Windows native binaries

The workflow currently runs on `ubuntu-latest`, so it publishes:

- Cross-platform fat jar
- Linux x86_64 native executable
- Windows x86_64 native executable

Pre-release rules:

- If the version number contains `SNAPSHOT`, `alpha`, `beta`, `rc`, `milestone`, or `preview`, it is marked as a Pre-release
- Otherwise, it is published as a stable release

Recommended release process:

1. Update `<version>` in `pom.xml`
2. Commit and merge to `main` or `master`
3. Create a tag with the same name, e.g. `1.0` or `1.1.0-SNAPSHOT`
4. Push the tag

Example:

```bash
git tag 1.0
git push origin 1.0
```

```bash
git tag 1.1.0-SNAPSHOT
git push origin 1.1.0-SNAPSHOT
```

## Current Limitations

- Only the `github` source is implemented so far
- `http/https/socks5` proxies are handled uniformly through Hutool HTTP
- No download caching yet

## Roadmap

- Add Gitee source
- Add Maven repository source
- Add caching and checksum verification
- Add more end-to-end integration tests

## Contributing

Issues and Pull Requests are welcome!

## License

This project is licensed under the [Apache License 2.0](LICENSE).
