package cn.suhoan.peony.cli;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliParserTest {
    @Test
    void parsesGithubRunCommandAndProgramArgs() {
        PeonyRequest request = CliParser.parse(new String[]{
                "run", "github", "owner/repo",
                "--asset", "tool.jar",
                "--config", "conf.properties",
                "--java-home", "D:/jdk",
                "--workspace", "D:/work",
                "--stable-only",
                "--keep-workspace",
                "--proxy", "http://127.0.0.1:8080",
                "--prefix-proxy", "https://gp.example/",
                "--github-token-env", "CUSTOM_GH_TOKEN",
                "--jvm-args", "-Xmx512m -Dfoo=bar",
                "--", "--port", "3000"
        });

        assertFalse(request.showHelp());
        assertEquals(Command.RUN, request.command());
        assertEquals("github", request.sourceType());
        assertEquals("owner/repo", request.repository());
        assertEquals("tool.jar", request.assetName());
        assertEquals(Path.of("conf.properties"), request.configPath());
        assertEquals(Path.of("D:/jdk"), request.javaHomeOverride());
        assertEquals(Path.of("D:/work"), request.workspaceParentOverride());
        assertTrue(request.stableOnly());
        assertTrue(request.keepWorkspace());
        assertEquals("http://127.0.0.1:8080", request.proxyOverride().proxyUrl());
        assertEquals("https://gp.example/", request.proxyOverride().prefixProxy());
        assertEquals("CUSTOM_GH_TOKEN", request.githubTokenEnv());
        assertEquals("-Xmx512m -Dfoo=bar", request.jvmArgsOverride());
        assertEquals(2, request.programArgs().size());
        assertEquals("--port", request.programArgs().getFirst());
    }

    @Test
    void parsesPullCommand() {
        PeonyRequest request = CliParser.parse(new String[]{
                "pull", "github", "owner/repo",
                "--asset", "tool.jar",
                "--stable-only"
        });

        assertFalse(request.showHelp());
        assertEquals(Command.PULL, request.command());
        assertEquals("github", request.sourceType());
        assertEquals("owner/repo", request.repository());
        assertEquals("tool.jar", request.assetName());
        assertTrue(request.stableOnly());
    }

    @Test
    void pullCommandRejectsProgramArgs() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CliParser.parse(new String[]{
                        "pull", "github", "owner/repo", "--", "--port", "3000"
                }));

        assertTrue(exception.getMessage().contains("pull"));
        assertTrue(exception.getMessage().contains("program args"));
    }

    @Test
    void pullCommandRejectsJvmArgs() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CliParser.parse(new String[]{
                        "pull", "github", "owner/repo", "--jvm-args", "-Xmx512m"
                }));

        assertTrue(exception.getMessage().contains("pull"));
        assertTrue(exception.getMessage().contains("--jvm-args"));
    }

    @Test
    void parsesReleaseTag() {
        PeonyRequest request = CliParser.parse(new String[]{
                "run", "github", "owner/repo", "--release-tag", "v1.0.0"
        });

        assertEquals("v1.0.0", request.releaseTag());
        assertFalse(request.stableOnly());
    }

    @Test
    void releaseTagDefaultsToNull() {
        PeonyRequest request = CliParser.parse(new String[]{
                "run", "github", "owner/repo"
        });

        assertNull(request.releaseTag());
    }

    @Test
    void releaseTagAndStableOnlyAreMutuallyExclusive() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CliParser.parse(new String[]{
                        "run", "github", "owner/repo",
                        "--release-tag", "v1.0.0",
                        "--stable-only"
                }));

        assertTrue(exception.getMessage().contains("--release-tag"));
        assertTrue(exception.getMessage().contains("--stable-only"));
    }

    @Test
    void releaseTagMissingValueThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> CliParser.parse(new String[]{
                        "run", "github", "owner/repo", "--release-tag"
                }));
    }

    @Test
    void invalidCommandThrows() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CliParser.parse(new String[]{
                        "push", "github", "owner/repo"
                }));

        assertTrue(exception.getMessage().contains("run"));
        assertTrue(exception.getMessage().contains("pull"));
    }
}
