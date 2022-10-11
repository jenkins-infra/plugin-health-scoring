package io.jenkins.pluginhealth.scoring.probes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(DependabotProbe.ORDER)
public class DependabotProbe extends Probe {
    public static final int ORDER = LastCommitDateProbe.ORDER + 1;

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final Path scmRepository = context.getScmRepository();
        final Path githubConfig = scmRepository.resolve(".github");

        try (Stream<Path> paths = Files
            .find(githubConfig, 1, (path, basicFileAttributes) -> Files.isRegularFile(path) && path.getFileName().toString().startsWith("dependabot"))) {
            return paths.findFirst()
                .map(file -> ProbeResult.success(key(), "Dependabot is configured"))
                .orElseGet(() -> ProbeResult.failure(key(), "No configuration file for dependabot"));
        } catch (IOException ex) {
            return ProbeResult.failure(key(), "No GitHub configuration folder");
        }
    }

    @Override
    public String key() {
        return "dependabot";
    }

    @Override
    public String getDescription() {
        return "Checks if dependabot is configured on a plugin.";
    }
}
