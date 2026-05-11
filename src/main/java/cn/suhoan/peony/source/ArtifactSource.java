package cn.suhoan.peony.source;

import cn.suhoan.peony.cli.PeonyRequest;
import cn.suhoan.peony.config.ExecutionSettings;
import cn.suhoan.peony.net.HttpExecutor;

public interface ArtifactSource {
    ResolvedArtifact resolve(PeonyRequest request, ExecutionSettings settings, HttpExecutor httpExecutor) throws Exception;
}
