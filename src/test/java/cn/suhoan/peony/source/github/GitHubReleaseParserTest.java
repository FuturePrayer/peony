package cn.suhoan.peony.source.github;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitHubReleaseParserTest {
    @Test
    void parsesJarAssets() throws Exception {
        String json = """
                {
                  "assets": [
                    {
                      "name": "tool.jar",
                      "url": "https://api.github.com/assets/1"
                    },
                    {
                      "name": "tool.zip",
                      "browser_download_url": "https://github.com/example/tool.zip"
                    }
                  ]
                }
                """;

        List<GitHubReleaseAsset> assets = GitHubReleaseParser.parseAssets(json);

        assertEquals(2, assets.size());
        assertEquals("tool.jar", assets.getFirst().name());
        assertEquals("https://api.github.com/assets/1", assets.getFirst().apiUrl());
    }

    @Test
    void requiresAssetNameWhenMultipleJarsExist() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> GitHubSource.selectAsset(List.of(
                new GitHubReleaseAsset("a.jar", "https://api.github.com/assets/a", null),
                new GitHubReleaseAsset("b.jar", "https://api.github.com/assets/b", null)
        ), null));

        assertEquals("multiple .jar assets found; use --asset. Available jars: a.jar, b.jar", exception.getMessage());
    }
}
