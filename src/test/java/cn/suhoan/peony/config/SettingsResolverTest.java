package cn.suhoan.peony.config;

import cn.suhoan.peony.cli.RunRequest;
import cn.suhoan.peony.net.ProxySettings;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingsResolverTest {
    @Test
    void usesConfigJavaHomeBeforeEnvironmentVariable() {
        RunRequest request = new RunRequest(false, false, "github", "owner/repo", null, null, null, null, false, ProxySettings.empty(), null, null, List.of());
        AppConfig config = new AppConfig(Path.of("D:/develop/jdk-21"), Path.of("D:/workspace-parent"), null, null, ProxySettings.empty());

        ExecutionSettings settings = SettingsResolver.resolve(request, config, Map.of(
                "PEONY_JAVA_HOME", "D:/env-jdk",
                "PEONY_WORKSPACE_PARENT", "D:/env-work"
        ));

        assertEquals(Path.of("D:/develop/jdk-21").toAbsolutePath().normalize(), settings.javaHome());
        assertEquals(Path.of("D:/env-work").toAbsolutePath().normalize(), settings.workspaceParent());
    }

    @Test
    void mergesProxyAndTokenWithCliPrecedence() {
        RunRequest request = new RunRequest(
                false,
                false,
                "github",
                "owner/repo",
                null,
                null,
                Path.of("D:/develop/jdk-21"),
                null,
                false,
                new ProxySettings("socks5://127.0.0.1:1080", "https://gp.example/"),
                null,
                "CUSTOM_TOKEN",
                List.of()
        );
        AppConfig config = new AppConfig(null, null, "config-token", null, new ProxySettings("http://127.0.0.1:8080", null));

        ExecutionSettings settings = SettingsResolver.resolve(request, config, Map.of(
                "CUSTOM_TOKEN", "cli-env-token",
                "PEONY_GITHUB_TOKEN", "default-env-token"
        ));

        assertEquals("socks5://127.0.0.1:1080", settings.proxySettings().proxyUrl());
        assertEquals("https://gp.example/", settings.proxySettings().prefixProxy());
        assertEquals("cli-env-token", settings.githubToken());
    }
}
