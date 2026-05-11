package cn.suhoan.peony.cli;

import cn.suhoan.peony.net.ProxySettings;

import java.nio.file.Path;
import java.util.List;

public record PeonyRequest(
        boolean showHelp,
        Command command,
        boolean stableOnly,
        String sourceType,
        String repository,
        String releaseTag,
        String assetName,
        Path configPath,
        Path javaHomeOverride,
        Path workspaceParentOverride,
        boolean keepWorkspace,
        ProxySettings proxyOverride,
        String githubToken,
        String githubTokenEnv,
        String jvmArgsOverride,
        List<String> programArgs
) {
    public static PeonyRequest helpRequest() {
        return new PeonyRequest(true, null, false, null, null, null, null, null, null, null, false, ProxySettings.empty(), null, null, null, List.of());
    }
}
