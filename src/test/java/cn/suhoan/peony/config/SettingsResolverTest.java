package cn.suhoan.peony.config;

import cn.suhoan.peony.cli.Command;
import cn.suhoan.peony.cli.PeonyRequest;
import cn.suhoan.peony.net.ProxySettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsResolverTest {
    @TempDir
    Path tempDir;

    @Test
    void usesConfigJavaHomeBeforeEnvironmentVariable() throws Exception {
        Path jdkHome = Files.createDirectories(tempDir.resolve("jdk-21"));
        PeonyRequest request = new PeonyRequest(false, Command.RUN, false, "github", "owner/repo", null, null, null, null, null, false, ProxySettings.empty(), null, null, null, List.of());
        AppConfig config = new AppConfig(jdkHome, tempDir.resolve("workspace-parent"), null, null, ProxySettings.empty(), null);

        ExecutionSettings settings = SettingsResolver.resolve(request, config, Map.of(
                "PEONY_JAVA_HOME", tempDir.resolve("env-jdk").toString(),
                "PEONY_WORKSPACE_PARENT", tempDir.resolve("env-work").toString()
        ));

        assertEquals(jdkHome.toAbsolutePath().normalize(), settings.javaHome());
        assertEquals(tempDir.resolve("env-work").toAbsolutePath().normalize(), settings.workspaceParent());
    }

    @Test
    void mergesProxyAndTokenWithCliPrecedence() throws Exception {
        Path jdkHome = Files.createDirectories(tempDir.resolve("jdk-21"));
        PeonyRequest request = new PeonyRequest(
                false,
                Command.RUN,
                false,
                "github",
                "owner/repo",
                null,
                null,
                null,
                jdkHome,
                null,
                false,
                new ProxySettings("socks5://127.0.0.1:1080", "https://gp.example/"),
                null,
                "CUSTOM_TOKEN",
                null,
                List.of()
        );
        AppConfig config = new AppConfig(null, null, "config-token", null, new ProxySettings("http://127.0.0.1:8080", null), null);

        ExecutionSettings settings = SettingsResolver.resolve(request, config, Map.of(
                "CUSTOM_TOKEN", "cli-env-token",
                "PEONY_GITHUB_TOKEN", "default-env-token"
        ));

        assertEquals("socks5://127.0.0.1:1080", settings.proxySettings().proxyUrl());
        assertEquals("https://gp.example/", settings.proxySettings().prefixProxy());
        assertEquals("cli-env-token", settings.githubToken());
    }

    @Test
    void pullCommandDoesNotRequireJavaHome() {
        PeonyRequest request = new PeonyRequest(false, Command.PULL, false, "github", "owner/repo", null, null, null, null, null, false, ProxySettings.empty(), null, null, null, List.of());
        AppConfig config = AppConfig.empty();

        ExecutionSettings settings = SettingsResolver.resolve(request, config, Map.of());

        assertEquals(null, settings.javaHome());
    }

    @Test
    void runCommandRequiresJavaHome() {
        PeonyRequest request = new PeonyRequest(false, Command.RUN, false, "github", "owner/repo", null, null, null, null, null, false, ProxySettings.empty(), null, null, null, List.of());
        AppConfig config = AppConfig.empty();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> SettingsResolver.resolve(request, config, Map.of()));

        assertTrue(exception.getMessage().contains("java home"));
    }

    @Test
    void jvmArgsFromCliTakesPrecedence() throws Exception {
        Path jdkHome = Files.createDirectories(tempDir.resolve("jdk-21"));
        PeonyRequest request = new PeonyRequest(false, Command.RUN, false, "github", "owner/repo", null, null, null, jdkHome, null, false, ProxySettings.empty(), null, null, "-Xmx512m", List.of());
        AppConfig config = new AppConfig(null, null, null, null, ProxySettings.empty(), "-Xmx256m");

        ExecutionSettings settings = SettingsResolver.resolve(request, config, Map.of(
                "PEONY_JVM_ARGS", "-Xmx1g"
        ));

        assertEquals(List.of("-Xmx512m"), settings.jvmArgs());
    }

    @Test
    void jvmArgsFromEnvTakesPrecedenceOverConfig() throws Exception {
        Path jdkHome = Files.createDirectories(tempDir.resolve("jdk-21"));
        PeonyRequest request = new PeonyRequest(false, Command.RUN, false, "github", "owner/repo", null, null, null, jdkHome, null, false, ProxySettings.empty(), null, null, null, List.of());
        AppConfig config = new AppConfig(null, null, null, null, ProxySettings.empty(), "-Xmx256m");

        ExecutionSettings settings = SettingsResolver.resolve(request, config, Map.of(
                "PEONY_JVM_ARGS", "-Xmx1g"
        ));

        assertEquals(List.of("-Xmx1g"), settings.jvmArgs());
    }

    @Test
    void jvmArgsFromConfigWhenNoOtherSource() throws Exception {
        Path jdkHome = Files.createDirectories(tempDir.resolve("jdk-21"));
        PeonyRequest request = new PeonyRequest(false, Command.RUN, false, "github", "owner/repo", null, null, null, jdkHome, null, false, ProxySettings.empty(), null, null, null, List.of());
        AppConfig config = new AppConfig(null, null, null, null, ProxySettings.empty(), "-Xmx256m");

        ExecutionSettings settings = SettingsResolver.resolve(request, config, Map.of());

        assertEquals(List.of("-Xmx256m"), settings.jvmArgs());
    }

    @Test
    void jvmArgsDefaultToEmptyList() throws Exception {
        Path jdkHome = Files.createDirectories(tempDir.resolve("jdk-21"));
        PeonyRequest request = new PeonyRequest(false, Command.RUN, false, "github", "owner/repo", null, null, null, jdkHome, null, false, ProxySettings.empty(), null, null, null, List.of());
        AppConfig config = AppConfig.empty();

        ExecutionSettings settings = SettingsResolver.resolve(request, config, Map.of());

        assertTrue(settings.jvmArgs().isEmpty());
    }

    @Test
    void splitArgsPreservingQuotedSpaces() {
        assertEquals(List.of("-Xmx512m", "-Dfoo=bar"), SettingsResolver.splitArgsPreservingQuotedSpaces("-Xmx512m -Dfoo=bar"));
        assertEquals(List.of("-Dpath=C:\\Program Files\\Java"), SettingsResolver.splitArgsPreservingQuotedSpaces("-Dpath=\"C:\\Program Files\\Java\""));
        assertEquals(List.of("-Dname=hello world"), SettingsResolver.splitArgsPreservingQuotedSpaces("-Dname='hello world'"));
        assertEquals(List.of("-Xmx1g", "-Dpath=C:\\Program Files\\Java", "-Dfoo=bar"), SettingsResolver.splitArgsPreservingQuotedSpaces("-Xmx1g -Dpath=\"C:\\Program Files\\Java\" -Dfoo=bar"));
    }
}
