package cn.suhoan.peony.config;

import cn.suhoan.peony.net.ProxySettings;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public final class ConfigLoader {
    private ConfigLoader() {
    }

    public static AppConfig load(Path explicitPath, Path workingDirectory) throws IOException {
        Path configPath = explicitPath != null ? explicitPath.toAbsolutePath().normalize() : discoverDefaultConfig(workingDirectory);
        if (configPath == null || Files.notExists(configPath)) {
            return AppConfig.empty();
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(configPath)) {
            properties.load(inputStream);
        }

        return new AppConfig(
                getPath(properties, "java.home"),
                getPath(properties, "workspace.parent"),
                trimToNull(properties.getProperty("github.token")),
                trimToNull(properties.getProperty("github.token.env")),
                new ProxySettings(
                        trimToNull(properties.getProperty("proxy.url")),
                        trimToNull(properties.getProperty("proxy.prefix"))
                )
        );
    }

    private static Path discoverDefaultConfig(Path workingDirectory) {
        List<Path> candidates = List.of(
                workingDirectory.resolve("peony.properties"),
                Path.of(System.getProperty("user.home")).resolve(".peony.properties")
        );
        return candidates.stream().filter(Files::exists).findFirst().orElse(null);
    }

    private static Path getPath(Properties properties, String key) {
        String value = trimToNull(properties.getProperty(key));
        return value == null ? null : Path.of(value);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
