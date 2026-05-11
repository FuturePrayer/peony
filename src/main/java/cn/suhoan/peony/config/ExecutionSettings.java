package cn.suhoan.peony.config;

import cn.suhoan.peony.net.ProxySettings;

import java.nio.file.Path;

public record ExecutionSettings(
        Path javaHome,
        Path workspaceParent,
        ProxySettings proxySettings,
        String githubToken
) {
}
