package cn.suhoan.peony.process;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class JarRunner {
    private JarRunner() {
    }

    public static int run(Path javaHome, Path workingDirectory, Path jarPath, List<String> jvmArgs, List<String> programArgs) throws IOException, InterruptedException {
        Path javaExecutable = resolveJavaExecutable(javaHome);
        List<String> command = new ArrayList<>();
        command.add(javaExecutable.toString());
        command.addAll(jvmArgs);
        command.add("-jar");
        command.add(jarPath.toString());
        command.addAll(programArgs);

        Process process = new ProcessBuilder(command)
                .directory(workingDirectory.toFile())
                .inheritIO()
                .start();
        return process.waitFor();
    }

    private static Path resolveJavaExecutable(Path javaHome) {
        Path windowsExecutable = javaHome.resolve("bin").resolve("java.exe");
        if (Files.isRegularFile(windowsExecutable)) {
            return windowsExecutable;
        }
        Path unixExecutable = javaHome.resolve("bin").resolve("java");
        if (Files.isRegularFile(unixExecutable)) {
            return unixExecutable;
        }
        throw new IllegalArgumentException("java executable not found under " + javaHome);
    }
}
