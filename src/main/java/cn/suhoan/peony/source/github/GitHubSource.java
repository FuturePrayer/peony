package cn.suhoan.peony.source.github;

import cn.suhoan.peony.cli.PeonyRequest;
import cn.suhoan.peony.config.ExecutionSettings;
import cn.suhoan.peony.net.HttpExecutor;
import cn.suhoan.peony.source.ArtifactSource;
import cn.suhoan.peony.source.ResolvedArtifact;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class GitHubSource implements ArtifactSource {
    @Override
    public ResolvedArtifact resolve(PeonyRequest request, ExecutionSettings settings, HttpExecutor httpExecutor) throws Exception {
        String apiResponse = httpExecutor.getText(releasesUri(request.repository()), releaseHeaders(settings.githubToken()));
        List<GitHubRelease> releases = GitHubReleaseParser.parseReleases(apiResponse);
        GitHubRelease release = selectRelease(releases, request.stableOnly(), request.releaseTag());
        List<GitHubReleaseAsset> jarAssets = release.assets().stream().filter(GitHubReleaseAsset::isJar).toList();

        GitHubReleaseAsset asset = selectAsset(jarAssets, request.assetName());
        Map<String, String> headers = new LinkedHashMap<>();
        if (settings.githubToken() != null && asset.apiUrl() != null) {
            headers.put("Authorization", "Bearer " + settings.githubToken());
            headers.put("Accept", "application/octet-stream");
        }
        return new ResolvedArtifact(asset.name(), asset.downloadUri(), headers);
    }

    static GitHubRelease selectRelease(List<GitHubRelease> releases, boolean stableOnly, String releaseTag) {
        if (releaseTag != null) {
            return releases.stream()
                    .filter(release -> releaseTag.equals(release.tagName()))
                    .filter(release -> release.assets().stream().anyMatch(GitHubReleaseAsset::isJar))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("release not found with tag: " + releaseTag));
        }
        return releases.stream()
                .filter(release -> !stableOnly || !release.prerelease())
                .filter(release -> release.assets().stream().anyMatch(GitHubReleaseAsset::isJar))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(stableOnly
                        ? "no stable release with .jar assets was found"
                        : "no release with .jar assets was found"));
    }

    static GitHubReleaseAsset selectAsset(List<GitHubReleaseAsset> jarAssets, String requestedAssetName) {
        if (jarAssets.isEmpty()) {
            throw new IllegalArgumentException("selected release does not contain any .jar assets");
        }
        if (requestedAssetName != null) {
            return jarAssets.stream()
                    .filter(asset -> asset.name().equals(requestedAssetName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("requested asset not found: " + requestedAssetName
                            + "; available jars: " + names(jarAssets)));
        }
        if (jarAssets.size() > 1) {
            throw new IllegalArgumentException("multiple .jar assets found; use --asset. Available jars: " + names(jarAssets));
        }
        return jarAssets.getFirst();
    }

    private static URI releasesUri(String repository) {
        return URI.create("https://api.github.com/repos/" + repository + "/releases?per_page=20");
    }

    private static Map<String, String> releaseHeaders(String githubToken) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept", "application/vnd.github+json");
        headers.put("X-GitHub-Api-Version", "2022-11-28");
        if (githubToken != null) {
            headers.put("Authorization", "Bearer " + githubToken);
        }
        return headers;
    }

    private static String names(List<GitHubReleaseAsset> assets) {
        return assets.stream().map(GitHubReleaseAsset::name).collect(Collectors.joining(", "));
    }
}
