package cn.suhoan.peony;

import cn.suhoan.peony.cli.CliParser;
import cn.suhoan.peony.cli.RunRequest;
import cn.suhoan.peony.config.AppConfig;
import cn.suhoan.peony.config.ConfigLoader;
import cn.suhoan.peony.config.ExecutionSettings;
import cn.suhoan.peony.config.SettingsResolver;
import cn.suhoan.peony.net.HttpExecutor;
import cn.suhoan.peony.process.JarRunner;
import cn.suhoan.peony.source.ArtifactSourceRegistry;
import cn.suhoan.peony.source.ResolvedArtifact;
import cn.suhoan.peony.workspace.WorkspaceSession;

import java.nio.file.Path;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        try {
            RunRequest request = CliParser.parse(args);
            if (request.showHelp()) {
                System.out.println(CliParser.usage());
                return 0;
            }

            Path workingDirectory = Path.of("").toAbsolutePath().normalize();
            AppConfig appConfig = ConfigLoader.load(request.configPath(), workingDirectory);
            ExecutionSettings settings = SettingsResolver.resolve(request, appConfig);
            boolean deleteWorkspaceOnClose = !request.keepWorkspace() && !request.downloadOnly();

            try (WorkspaceSession workspace = WorkspaceSession.create(settings.workspaceParent(), deleteWorkspaceOnClose);
                 HttpExecutor httpExecutor = new HttpExecutor(settings.proxySettings())) {
                ResolvedArtifact artifact = ArtifactSourceRegistry.resolve(request, settings, httpExecutor);
                Path jarPath = artifact.downloadTo(workspace.directory(), httpExecutor);
                if (request.downloadOnly()) {
                    System.out.println(jarPath.toAbsolutePath().normalize());
                    return 0;
                }
                return JarRunner.run(settings.javaHome(), workspace.directory(), jarPath, request.programArgs());
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            System.err.println(CliParser.usage());
            return 2;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
