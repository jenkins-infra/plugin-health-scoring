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
@Order(RenovateProbe.ORDER)
public class RenovateProbe extends Probe {
    public static final int ORDER = LastCommitDateProbe.ORDER + 100;
    public static final String KEY = "renovate";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final Path scmRepository = context.getScmRepository();
        final Path githubConfig = scmRepository.resolve(".github");
        if (Files.notExists(githubConfig)) {
            return ProbeResult.failure(key(), "No GitHub configuration folder");
        }

        try (Stream<Path> paths = Files
            .find(githubConfig, 1, (path, $) -> Files.isRegularFile(path)
                && path.getFileName().toString().startsWith("renovate"))) {
            return paths.findFirst()
                    .map(file -> ProbeResult.success(key(), "Renovate is configured"))
                .orElseGet(() -> ProbeResult.failure(key(), "No configuration file for renovate"));
        } catch (IOException ex) {
            return ProbeResult.error(key(), "Could not browse the plugin folder");
        }
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Check if renovate is configured in the plugin";
    }
}
