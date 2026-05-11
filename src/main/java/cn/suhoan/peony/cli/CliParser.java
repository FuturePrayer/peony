package cn.suhoan.peony.cli;

import cn.suhoan.peony.net.ProxySettings;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CliParser {
    private CliParser() {
    }

    public static PeonyRequest parse(String[] args) {
        if (args.length == 0) {
            return PeonyRequest.helpRequest();
        }

        List<String> tokens = List.of(args);
        if (tokens.size() == 1 && isHelpToken(tokens.getFirst())) {
            return PeonyRequest.helpRequest();
        }

        String firstToken = tokens.getFirst();
        Command command = switch (firstToken) {
            case "run" -> Command.RUN;
            case "pull" -> Command.PULL;
            default -> throw new IllegalArgumentException("expected 'run' or 'pull' command, got: " + firstToken);
        };
        if (tokens.size() < 3) {
            throw new IllegalArgumentException("expected source type and repository");
        }

        String sourceType = tokens.get(1);
        String repository = tokens.get(2);
        String assetName = null;
        String releaseTag = null;
        Path configPath = null;
        Path javaHome = null;
        Path workspaceParent = null;
        boolean keepWorkspace = false;
        boolean stableOnly = false;
        String proxy = null;
        String prefixProxy = null;
        String githubToken = null;
        String githubTokenEnv = null;
        String jvmArgs = null;
        List<String> programArgs = List.of();

        int index = 3;
        while (index < tokens.size()) {
            String token = tokens.get(index);
            if ("--".equals(token)) {
                if (command == Command.PULL) {
                    throw new IllegalArgumentException("'pull' command does not accept program args (--)");
                }
                programArgs = new ArrayList<>(tokens.subList(index + 1, tokens.size()));
                break;
            }

            switch (token) {
                case "-h", "--help" -> {
                    return PeonyRequest.helpRequest();
                }
                case "--asset" -> {
                    assetName = requireValue(tokens, ++index, token);
                    index++;
                }
                case "--release-tag" -> {
                    releaseTag = requireValue(tokens, ++index, token);
                    index++;
                }
                case "--config" -> {
                    configPath = Path.of(requireValue(tokens, ++index, token));
                    index++;
                }
                case "--java-home" -> {
                    javaHome = Path.of(requireValue(tokens, ++index, token));
                    index++;
                }
                case "--workspace" -> {
                    workspaceParent = Path.of(requireValue(tokens, ++index, token));
                    index++;
                }
                case "--keep-workspace" -> {
                    keepWorkspace = true;
                    index++;
                }
                case "--stable-only" -> {
                    stableOnly = true;
                    index++;
                }
                case "--proxy" -> {
                    proxy = requireValue(tokens, ++index, token);
                    index++;
                }
                case "--prefix-proxy" -> {
                    prefixProxy = requireValue(tokens, ++index, token);
                    index++;
                }
                case "--github-token" -> {
                    githubToken = requireValue(tokens, ++index, token);
                    index++;
                }
                case "--github-token-env" -> {
                    githubTokenEnv = requireValue(tokens, ++index, token);
                    index++;
                }
                case "--jvm-args" -> {
                    jvmArgs = requireValue(tokens, ++index, token);
                    index++;
                }
                default -> throw new IllegalArgumentException("unknown option: " + token);
            }
        }

        if (!repository.contains("/")) {
            throw new IllegalArgumentException("repository must be in owner/repo format");
        }

        if (releaseTag != null && stableOnly) {
            throw new IllegalArgumentException("--release-tag and --stable-only are mutually exclusive; when a specific release tag is specified, pre-release filtering is not applicable");
        }

        if (jvmArgs != null && command == Command.PULL) {
            throw new IllegalArgumentException("'pull' command does not accept --jvm-args");
        }

        return new PeonyRequest(
                false,
                command,
                stableOnly,
                sourceType,
                repository,
                releaseTag,
                assetName,
                configPath,
                javaHome,
                workspaceParent,
                keepWorkspace,
                new ProxySettings(proxy, prefixProxy),
                githubToken,
                githubTokenEnv,
                jvmArgs,
                programArgs
        );
    }

    public static String usage() {
        return """
                Usage:
                  peony run github <owner/repo> [options] -- [program args]
                  peony pull github <owner/repo> [options]

                Commands:
                  run   Download and run the jar
                  pull  Download the jar only (output the jar path to stdout, keep workspace)

                By default, GitHub release lookup includes the newest pre-release.
                Use --stable-only to restrict lookup to stable releases only.

                Options:
                  --asset <name>              Select a specific .jar asset when multiple jars exist
                  --release-tag <tag>         Download a specific release by tag name (e.g. v1.0.0); mutually exclusive with --stable-only
                  --config <path>             Load configuration from properties file
                  --java-home <path>          Override JDK home (not needed for 'pull')
                  --workspace <path>          Parent directory used to create a temporary workspace
                  --keep-workspace            Keep the temporary workspace after exit
                  --stable-only               Only consider stable releases and ignore pre-releases
                  --jvm-args <args>           JVM arguments passed to the java process (e.g. "-Xmx512m -Dfoo=bar"); \
                does not support spaces inside quoted values on the command line — use environment variable or config file instead
                  --proxy <url>               Proxy URL, supports http://, https:// and socks5://
                  --prefix-proxy <url>        Prefix proxy applied to every requested URL
                  --github-token <token>      GitHub token for private repositories, see https://github.com/settings/personal-access-tokens or https://github.com/settings/tokens
                  --github-token-env <name>   Environment variable name containing the GitHub token
                  -h, --help                  Show this help

                Configuration keys:
                  java.home
                  jvm.args
                  workspace.parent
                  github.token
                  github.token.env
                  proxy.url
                  proxy.prefix

                Environment variables:
                  PEONY_JAVA_HOME, JAVA_HOME
                  PEONY_JVM_ARGS
                  PEONY_GITHUB_TOKEN, GITHUB_TOKEN
                  PEONY_PROXY, HTTPS_PROXY, HTTP_PROXY, ALL_PROXY
                  PEONY_PREFIX_PROXY
                  PEONY_WORKSPACE_PARENT
                """;
    }

    private static boolean isHelpToken(String token) {
        return "-h".equals(token) || "--help".equals(token);
    }

    private static String requireValue(List<String> tokens, int index, String option) {
        if (index >= tokens.size() || "--".equals(tokens.get(index))) {
            throw new IllegalArgumentException("missing value for " + option);
        }
        return tokens.get(index);
    }
}
