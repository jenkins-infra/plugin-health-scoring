/*
 * MIT License
 *
 * Copyright (c) 2023 Jenkins Infra
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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

/**
 * This is an abstract class that looks for desired configuration in the files present in GitHub Workflows directory.
 *
 * @return The class returns success when the configuration is found, otherwise returns a failure.
 */

public abstract class AbstractGitHubWorkflowProbe extends Probe {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGitHubWorkflowProbe.class);
    private static final String WORKFLOWS_DIRECTORY = ".github/workflows";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final Path repository = context.getScmRepository();
        final Path workflowPath = repository.resolve(WORKFLOWS_DIRECTORY);

        if (!Files.exists(workflowPath)) {
            return ProbeResult.failure(key(), "Plugin has no GitHub Action configured");
        }

        try (Stream<Path> files = Files.find(workflowPath, 1, (path, $) -> Files.isRegularFile(path))) {
             boolean isWorkflowConfigured = files
                .map(file -> readWorkflowFile(file))
                /**
                 * Checks if the map is null or empty. This means no GitHub action is defined.
                 * */
                .filter(workflow -> workflow.jobs() != null && !workflow.jobs().isEmpty())
                .flatMap(workflow -> workflow.jobs().values().stream())
                .map(WorkflowJobDefinition::specificJobDefinition)
                .filter(Objects::nonNull)
                .anyMatch(jobDefinition -> jobDefinition.startsWith(getWorkflowDefinition()));

            return isWorkflowConfigured ?
                    ProbeResult.success(key(), getSuccessMessage()) :
                    ProbeResult.failure(key(), getFailureMessage());
        } catch (IOException e) {
            return ProbeResult.error(key(), e.getMessage());
        }
    }

    /**
     * Returns the path to the GitHub Workflow definition which should be use in one of the actions of the plugin repository.
     *
     * @return the workflow definition used in one of the jobs of one of the actions of the plugin repository.
     */
    public abstract String getWorkflowDefinition();

    /**
     * This method reads the files in GitHub Workflow directory using ObjectMapper
     *
     *  @return a map of all the GitHub Actions defined in the file.
     * */
    private WorkflowDefinition readWorkflowFile(Path filePath) {
        final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
        try {
            return yaml.readValue(Files.newInputStream(filePath), WorkflowDefinition.class);
        } catch (IOException e) {
            LOGGER.warn("Couldn't not read {} for {} on {}", filePath, key(), e);
            return new WorkflowDefinition(Map.of());
        }
    }

    /**
     * @return a map of all the GitHub Actions defined in the file.
     * */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WorkflowDefinition(Map<String, WorkflowJobDefinition> jobs) {
    }

    /**
     *  @return a String i.e, returns one GitHub action configured at a time.
     * **/
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WorkflowJobDefinition(String specificJobDefinition) {
    }

    /**
     * @return a failure message
     * */
    public abstract String getFailureMessage();

    /**
     * @return a success message
     * */
    public abstract String getSuccessMessage();

}
