package cn.suhoan.peony.config;

import cn.suhoan.peony.net.ProxySettings;

import java.nio.file.Path;
import java.util.List;

public record ExecutionSettings(
        Path javaHome,
        Path workspaceParent,
        ProxySettings proxySettings,
        String githubToken,
        List<String> jvmArgs
) {
}
