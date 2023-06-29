package io.jenkins.pluginhealth.scoring.utility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class GitHubWorkflowReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubWorkflowReader.class);

    final Path WORKFLOWS_DIRECTORY = Path.of(".github/workflows");

    public List<String> getJobDetail() {

        try (Stream<Path> files = Files.find(WORKFLOWS_DIRECTORY, 1,
            (path, $) -> Files.isRegularFile(path)
        )) {
            final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

            return files.map(file -> {
                try {
                    return yaml.readValue(Files.newInputStream(file), GitHubWorkflowReader.WorkflowDefinition.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }). filter(wf -> wf.jobs() != null && !wf.jobs().isEmpty())
                .flatMap(wf -> wf.jobs().values().stream())
                .map(GitHubWorkflowReader.WorkflowJobDefinition::uses)
                .collect(Collectors.toList());


        }catch (IOException e) {
            LOGGER.error("Could not read {} in {} because {}", WORKFLOWS_DIRECTORY.getFileName(), this.getClass().getSimpleName(), e );
        }
        return new ArrayList<>();
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WorkflowJobDefinition(String uses) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WorkflowDefinition(Map<String, GitHubWorkflowReader.WorkflowJobDefinition> jobs) {
    }

}

