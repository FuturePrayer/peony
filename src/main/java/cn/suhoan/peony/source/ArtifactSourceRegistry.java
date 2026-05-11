package cn.suhoan.peony.source;

import cn.suhoan.peony.cli.PeonyRequest;
import cn.suhoan.peony.config.ExecutionSettings;
import cn.suhoan.peony.net.HttpExecutor;
import cn.suhoan.peony.source.github.GitHubSource;

public final class ArtifactSourceRegistry {
    private ArtifactSourceRegistry() {
    }

    public static ResolvedArtifact resolve(PeonyRequest request, ExecutionSettings settings, HttpExecutor httpExecutor) throws Exception {
        ArtifactSource artifactSource = switch (request.sourceType()) {
            case "github" -> new GitHubSource();
            default -> throw new IllegalArgumentException("unsupported source type: " + request.sourceType());
        };
        return artifactSource.resolve(request, settings, httpExecutor);
    }
}
