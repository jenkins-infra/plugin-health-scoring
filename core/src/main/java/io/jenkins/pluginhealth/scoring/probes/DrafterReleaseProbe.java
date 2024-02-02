package io.jenkins.pluginhealth.scoring.probes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(DrafterReleaseProbe.ORDER)
public class DrafterReleaseProbe extends  Probe {

    public static final String KEY = "release-drafter";

    public static final int ORDER = LastCommitDateProbe.ORDER + 100;
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDependencyBotConfigurationProbe.class);

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        if (context.getScmRepository().isEmpty()) {
            return this.error("There is no local repository for plugin " + plugin.getName() + ".");
        }
        final Path scmRepository = context.getScmRepository().get();
        final Path githubConfig = scmRepository.resolve(".github");
        if (Files.notExists(githubConfig)) {
            LOGGER.trace("No GitHub configuration folder at {} ", key());
            return this.success("No GitHub configuration folder found.");
        }

        try (Stream<Path> paths = Files.find(githubConfig, 1, (path, $) ->
            Files.isRegularFile(path) && isPathDrafterConfigFile((path.getFileName().toString())))) {
            return paths.findFirst()
                .map(file -> this.success(String.format("%s is configured.", KEY)))
                .orElseGet(() -> this.success(String.format("%s is not configured.", KEY)));
        } catch (IOException ex) {
            LOGGER.error("Could not browse the plugin folder during probe {}", key(), ex);
            return this.error("Could not browse the plugin folder");
        }
    }

    private boolean isPathDrafterConfigFile(String filename) {
        return "release-drafter.yml".equals(filename) || "release-drafter.yaml".equals(filename);
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Check if release-drafter is configured on a plugin or not";
    }

    @Override
    public long getVersion() {
        return 1;
    }
}
