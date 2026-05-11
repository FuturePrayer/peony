package cn.suhoan.peony.cli;

import cn.suhoan.peony.net.ProxySettings;

import java.nio.file.Path;
import java.util.List;

public record RunRequest(
        boolean showHelp,
        boolean downloadOnly,
        boolean stableOnly,
        String sourceType,
        String repository,
        String assetName,
        Path configPath,
        Path javaHomeOverride,
        Path workspaceParentOverride,
        boolean keepWorkspace,
        ProxySettings proxyOverride,
        String githubToken,
        String githubTokenEnv,
        List<String> programArgs
) {
    public static RunRequest helpRequest() {
        return new RunRequest(true, false, false, null, null, null, null, null, null, false, ProxySettings.empty(), null, null, List.of());
    }
}
