package io.jenkins.pluginhealth.scoring.probes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractGitHubWorkflowProbe extends Probe {

    public static final String KEY = "abstract-github-workflow";
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGitHubWorkflowProbe.class);
    private static final String WORKFLOWS_DIRECTORY = ".github/workflows";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final Path repository = context.getScmRepository();
        final Path workflowPath = repository.resolve(WORKFLOWS_DIRECTORY);

        if (!Files.exists(workflowPath)) {
            return ProbeResult.failure(key(), "Plugin has no GitHub Action configured");
        }

        try (Stream<Path> files = Files.find(workflowPath, 1,
            (path, $) -> Files.isRegularFile(path)
        )) {
            final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

            return files
                .map(file -> {
                    try {
                        return yaml.readValue(Files.newInputStream(file), WorkflowDefinition.class);
                    } catch (IOException e) {
                        LOGGER.warn("Couldn't not read {} for {} on {}", file, key(), plugin.getName(), e);
                        return new WorkflowDefinition(Map.of());
                    }
                })
                .filter(wf -> wf.jobs() != null && !wf.jobs().isEmpty())
                .flatMap(wf -> wf.jobs().values().stream())
                .map(WorkflowJobDefinition::uses)
                .filter(Objects::nonNull)
                .anyMatch(def -> def.startsWith(getWorkflowDefinition())) ?
                ProbeResult.success(key(), getSuccessMessage()) :
                ProbeResult.failure(key(), getFailureMessage());

        } catch (IOException e) {
            return ProbeResult.error(key(), e.getMessage());
        }
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Abstract implementation of GitHub Workflow";
    }

    public abstract String getWorkflowDefinition();

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WorkflowDefinition(Map<String, WorkflowJobDefinition> jobs) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WorkflowJobDefinition(String uses) {
    }

    public abstract String getFailureMessage();

    public abstract String getSuccessMessage();

}
