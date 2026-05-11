package cn.suhoan.peony.workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public final class WorkspaceSession implements AutoCloseable {
    private final Path directory;
    private final boolean deleteOnClose;
    private final Thread shutdownHook;
    private boolean closed;

    private WorkspaceSession(Path directory, boolean deleteOnClose) {
        this.directory = directory;
        this.deleteOnClose = deleteOnClose;
        this.shutdownHook = new Thread(this::cleanupQuietly, "peony-workspace-cleanup");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public static WorkspaceSession create(Path parentDirectory, boolean deleteOnClose) throws IOException {
        Files.createDirectories(parentDirectory);
        Path directory = Files.createTempDirectory(parentDirectory, "peony-");
        return new WorkspaceSession(directory, deleteOnClose);
    }

    public Path directory() {
        return directory;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        removeShutdownHook();
        if (deleteOnClose) {
            deleteRecursively(directory);
        }
    }

    private void cleanupQuietly() {
        if (!deleteOnClose) {
            return;
        }
        try {
            deleteRecursively(directory);
        } catch (IOException ignored) {
        }
    }

    private void removeShutdownHook() {
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException ignored) {
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (Files.notExists(root)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw e;
        }
    }
}
