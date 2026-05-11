package cn.suhoan.peony.source.github;

import java.util.List;

public record GitHubRelease(String tagName, boolean prerelease, List<GitHubReleaseAsset> assets) {
}
