package cn.suhoan.peony.config;

import cn.suhoan.peony.cli.Command;
import cn.suhoan.peony.cli.PeonyRequest;
import cn.suhoan.peony.net.ProxySettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SettingsResolver {
    private static final Path DEFAULT_WORKSPACE_PARENT = Path.of(System.getProperty("java.io.tmpdir"), "peony");

    private SettingsResolver() {
    }

    public static ExecutionSettings resolve(PeonyRequest request, AppConfig config) {
        return resolve(request, config, System.getenv());
    }

    static ExecutionSettings resolve(PeonyRequest request, AppConfig config, Map<String, String> env) {
        Path javaHome = firstNonNull(
                request.javaHomeOverride(),
                config.javaHome(),
                getPath(env, "PEONY_JAVA_HOME"),
                getPath(env, "JAVA_HOME")
        );
        if (javaHome == null) {
            if (request.command() == Command.RUN) {
                throw new IllegalArgumentException("java home is not configured; use --java-home, java.home, PEONY_JAVA_HOME or JAVA_HOME");
            }
        } else {
            javaHome = javaHome.toAbsolutePath().normalize();
            if (Files.notExists(javaHome)) {
                throw new IllegalArgumentException("java home does not exist: " + javaHome);
            }
        }

        Path workspaceParent = firstNonNull(
                request.workspaceParentOverride(),
                getPath(env, "PEONY_WORKSPACE_PARENT"),
                config.workspaceParent(),
                DEFAULT_WORKSPACE_PARENT
        ).toAbsolutePath().normalize();

        ProxySettings proxySettings = ProxySettings.merge(
                config.proxySettings(),
                new ProxySettings(
                        firstEnvNonBlank(env, "PEONY_PROXY", "HTTPS_PROXY", "HTTP_PROXY", "ALL_PROXY"),
                        firstEnvNonBlank(env, "PEONY_PREFIX_PROXY")
                ),
                request.proxyOverride()
        );

        String githubToken = firstNonBlank(
                request.githubToken(),
                getEnv(env, request.githubTokenEnv()),
                firstEnvNonBlank(env, "PEONY_GITHUB_TOKEN", "GITHUB_TOKEN"),
                config.githubToken(),
                getEnv(env, config.githubTokenEnv())
        );

        List<String> jvmArgs = resolveJvmArgs(request, config, env);

        return new ExecutionSettings(javaHome, workspaceParent, proxySettings, githubToken, jvmArgs);
    }

    private static List<String> resolveJvmArgs(PeonyRequest request, AppConfig config, Map<String, String> env) {
        String rawArgs = firstNonBlank(
                request.jvmArgsOverride(),
                firstEnvNonBlank(env, "PEONY_JVM_ARGS"),
                config.jvmArgs()
        );
        if (rawArgs == null) {
            return List.of();
        }
        return splitArgsPreservingQuotedSpaces(rawArgs);
    }

    static List<String> splitArgsPreservingQuotedSpaces(String input) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inDoubleQuotes = false;
        boolean inSingleQuotes = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
            } else if (c == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
            } else if (Character.isWhitespace(c) && !inDoubleQuotes && !inSingleQuotes) {
                if (!current.isEmpty()) {
                    args.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (!current.isEmpty()) {
            args.add(current.toString());
        }

        return args;
    }

    private static String getEnv(Map<String, String> env, String name) {
        return isBlank(name) ? null : trimToNull(env.get(name));
    }

    private static Path getPath(Map<String, String> env, String name) {
        String value = trimToNull(env.get(name));
        return value == null ? null : Path.of(value);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String firstEnvNonBlank(Map<String, String> env, String... keys) {
        for (String key : keys) {
            String value = trimToNull(env.get(key));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
