package cn.suhoan.peony.cli;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliParserTest {
    @Test
    void parsesGithubRunCommandAndProgramArgs() {
        RunRequest request = CliParser.parse(new String[]{
                "run", "github", "owner/repo",
                "--asset", "tool.jar",
                "--config", "conf.properties",
                "--java-home", "D:/jdk",
                "--workspace", "D:/work",
                "--download-only",
                "--stable-only",
                "--keep-workspace",
                "--proxy", "http://127.0.0.1:8080",
                "--prefix-proxy", "https://gp.example/",
                "--github-token-env", "CUSTOM_GH_TOKEN",
                "--", "--port", "3000"
        });

        assertFalse(request.showHelp());
        assertEquals("github", request.sourceType());
        assertEquals("owner/repo", request.repository());
        assertEquals("tool.jar", request.assetName());
        assertEquals(Path.of("conf.properties"), request.configPath());
        assertEquals(Path.of("D:/jdk"), request.javaHomeOverride());
        assertEquals(Path.of("D:/work"), request.workspaceParentOverride());
        assertTrue(request.downloadOnly());
        assertTrue(request.stableOnly());
        assertTrue(request.keepWorkspace());
        assertEquals("http://127.0.0.1:8080", request.proxyOverride().proxyUrl());
        assertEquals("https://gp.example/", request.proxyOverride().prefixProxy());
        assertEquals("CUSTOM_GH_TOKEN", request.githubTokenEnv());
        assertEquals(2, request.programArgs().size());
        assertEquals("--port", request.programArgs().getFirst());
    }
}
