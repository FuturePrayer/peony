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
                    "tag_name": "v2.0.0-beta",
                    "prerelease": true,
                    "assets": [
                      {
                        "name": "tool-beta.jar",
                        "url": "https://api.github.com/assets/1"
                      }
                    ]
                  },
                  {
                    "tag_name": "v1.0.0",
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
        assertEquals("v2.0.0-beta", releases.getFirst().tagName());
        assertEquals(true, releases.getFirst().prerelease());
        assertEquals("v1.0.0", releases.get(1).tagName());
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
                new GitHubRelease("v2.0.0-beta", true, List.of(new GitHubReleaseAsset("beta.jar", "https://api.github.com/assets/beta", null))),
                new GitHubRelease("v1.0.0", false, List.of(new GitHubReleaseAsset("stable.jar", "https://api.github.com/assets/stable", null)))
        );

        assertEquals("beta.jar", GitHubSource.selectRelease(releases, false, null).assets().getFirst().name());
        assertEquals("stable.jar", GitHubSource.selectRelease(releases, true, null).assets().getFirst().name());
    }

    @Test
    void selectsReleaseByTag() {
        List<GitHubRelease> releases = List.of(
                new GitHubRelease("v2.0.0-beta", true, List.of(new GitHubReleaseAsset("beta.jar", "https://api.github.com/assets/beta", null))),
                new GitHubRelease("v1.0.0", false, List.of(new GitHubReleaseAsset("stable.jar", "https://api.github.com/assets/stable", null)))
        );

        assertEquals("stable.jar", GitHubSource.selectRelease(releases, false, "v1.0.0").assets().getFirst().name());
        assertEquals("beta.jar", GitHubSource.selectRelease(releases, false, "v2.0.0-beta").assets().getFirst().name());
    }

    @Test
    void throwsWhenReleaseTagNotFound() {
        List<GitHubRelease> releases = List.of(
                new GitHubRelease("v1.0.0", false, List.of(new GitHubReleaseAsset("stable.jar", "https://api.github.com/assets/stable", null)))
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> GitHubSource.selectRelease(releases, false, "v9.9.9"));
        assertEquals("release not found with tag: v9.9.9", exception.getMessage());
    }

    @Test
    void throwsWhenReleaseTagHasNoJarAssets() {
        List<GitHubRelease> releases = List.of(
                new GitHubRelease("v1.0.0", false, List.of(new GitHubReleaseAsset("readme.txt", "https://api.github.com/assets/readme", null)))
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> GitHubSource.selectRelease(releases, false, "v1.0.0"));
        assertEquals("release not found with tag: v1.0.0", exception.getMessage());
    }
}
