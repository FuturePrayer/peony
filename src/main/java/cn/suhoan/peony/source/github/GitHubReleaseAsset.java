package cn.suhoan.peony.source.github;

import java.net.URI;

public record GitHubReleaseAsset(String name, String apiUrl, String browserDownloadUrl) {
    public boolean isJar() {
        return name.toLowerCase().endsWith(".jar");
    }

    public URI downloadUri() {
        // browserDownloadUrl 对公开仓库直接可用，无需额外请求头
        // apiUrl 需要 Accept: application/octet-stream 请求头才能返回二进制内容
        return URI.create(browserDownloadUrl != null ? browserDownloadUrl : apiUrl);
    }
}
