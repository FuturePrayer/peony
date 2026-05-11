package cn.suhoan.peony.source.github;

import java.net.URI;

public record GitHubReleaseAsset(String name, String apiUrl, String browserDownloadUrl) {
    public boolean isJar() {
        return name.toLowerCase().endsWith(".jar");
    }

    public URI downloadUri() {
        return URI.create(apiUrl != null ? apiUrl : browserDownloadUrl);
    }
}
