package cn.suhoan.peony.source.github;

import java.util.List;

public record GitHubRelease(boolean prerelease, List<GitHubReleaseAsset> assets) {
}
