package cn.suhoan.peony.source;

import cn.suhoan.peony.net.HttpExecutor;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

public record ResolvedArtifact(String fileName, URI downloadUri, Map<String, String> headers) {
    public Path downloadTo(Path workspaceDirectory, HttpExecutor httpExecutor) throws Exception {
        Path filePath = workspaceDirectory.resolve(fileName);
        httpExecutor.download(downloadUri, headers, filePath);
        return filePath;
    }
}
