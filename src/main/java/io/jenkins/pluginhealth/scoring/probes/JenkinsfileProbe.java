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
@Order(value = JenkinsfileProbe.ORDER)
public class JenkinsfileProbe extends Probe {
    public static final int ORDER = LastCommitDateProbe.ORDER + 100;

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final Path repository = context.getScmRepository();
        try (Stream<Path> paths = Files
            .find(repository, 1, (file, basicFileAttributes) -> Files.isReadable(file) && "Jenkinsfile".equals(file.getFileName().toString()))
        ) {
            return paths.findFirst()
                .map(file -> ProbeResult.success(key(), "Jenkinsfile found"))
                .orElseGet(() -> ProbeResult.failure(key(), "No Jenkinsfile found"));
        } catch (IOException e) {
            return ProbeResult.error(key(), e.getMessage());
        }
    }

    @Override
    public String key() {
        return "jenkinsfile";
    }

    @Override
    public String getDescription() {
        return """
    Validates the existence of a `Jenkinsfile` file in the repository.
    This file is used to configure the plugin Continuous Integration on ci.jenkins.io.
    """;
    }
}
