package cn.suhoan.peony.config;

import cn.suhoan.peony.net.ProxySettings;

import java.nio.file.Path;

public record AppConfig(
        Path javaHome,
        Path workspaceParent,
        String githubToken,
        String githubTokenEnv,
        ProxySettings proxySettings
) {
    public static AppConfig empty() {
        return new AppConfig(null, null, null, null, ProxySettings.empty());
    }
}
