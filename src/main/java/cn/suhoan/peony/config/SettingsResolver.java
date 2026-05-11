package cn.suhoan.peony.config;

import cn.suhoan.peony.cli.RunRequest;
import cn.suhoan.peony.net.ProxySettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class SettingsResolver {
    private static final Path DEFAULT_WORKSPACE_PARENT = Path.of(System.getProperty("java.io.tmpdir"), "peony");

    private SettingsResolver() {
    }

    public static ExecutionSettings resolve(RunRequest request, AppConfig config) {
        return resolve(request, config, System.getenv());
    }

    static ExecutionSettings resolve(RunRequest request, AppConfig config, Map<String, String> env) {
        Path javaHome = firstNonNull(
                request.javaHomeOverride(),
                config.javaHome(),
                getPath(env, "PEONY_JAVA_HOME"),
                getPath(env, "JAVA_HOME")
        );
        if (javaHome == null) {
            throw new IllegalArgumentException("java home is not configured; use --java-home, java.home, PEONY_JAVA_HOME or JAVA_HOME");
        }
        javaHome = javaHome.toAbsolutePath().normalize();
        if (Files.notExists(javaHome)) {
            throw new IllegalArgumentException("java home does not exist: " + javaHome);
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

        return new ExecutionSettings(javaHome, workspaceParent, proxySettings, githubToken);
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
