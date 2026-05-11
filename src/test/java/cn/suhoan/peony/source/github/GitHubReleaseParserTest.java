package cn.suhoan.peony.source.github;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitHubReleaseParserTest {
    @Test
    void parsesReleaseListAndPrereleaseFlag() throws Exception {
        String json = """
                [
                  {
                    "prerelease": true,
                    "assets": [
                      {
                        "name": "tool-beta.jar",
                        "url": "https://api.github.com/assets/1"
                      }
                    ]
                  },
                  {
                    "prerelease": false,
                    "assets": [
                      {
                        "name": "tool.jar",
                        "url": "https://api.github.com/assets/2"
                      }
                    ]
                  }
                ]
                """;

        List<GitHubRelease> releases = GitHubReleaseParser.parseReleases(json);

        assertEquals(2, releases.size());
        assertEquals(true, releases.getFirst().prerelease());
        assertEquals("tool-beta.jar", releases.getFirst().assets().getFirst().name());
    }

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

    @Test
    void prefersPrereleaseByDefaultButCanFilterStableOnly() {
        List<GitHubRelease> releases = List.of(
                new GitHubRelease(true, List.of(new GitHubReleaseAsset("beta.jar", "https://api.github.com/assets/beta", null))),
                new GitHubRelease(false, List.of(new GitHubReleaseAsset("stable.jar", "https://api.github.com/assets/stable", null)))
        );

        assertEquals("beta.jar", GitHubSource.selectRelease(releases, false).assets().getFirst().name());
        assertEquals("stable.jar", GitHubSource.selectRelease(releases, true).assets().getFirst().name());
    }
}
