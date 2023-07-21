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
 * Abstract Probe allowing to search for the usage of a particular workflow within a project's GitHub workflows directory.
 */
public abstract class AbstractGitHubWorkflowProbe extends Probe {
    public static final int ORDER = LastCommitDateProbe.ORDER + 100;
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGitHubWorkflowProbe.class);
    private static final String WORKFLOWS_DIRECTORY = ".github/workflows";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        if (context.getScmRepository().isEmpty()) {
            return ProbeResult.error(key(), "There is no local repository for plugin " + plugin.getName() + ".");
        }
        final Path repository = context.getScmRepository().get();
        final Path workflowPath = repository.resolve(WORKFLOWS_DIRECTORY);

        if (!Files.exists(workflowPath)) {
            return ProbeResult.success(key(), "Plugin has no GitHub Action configured");
        }

        try (Stream<Path> files = Files.find(workflowPath, 1, (path, $) -> Files.isRegularFile(path))) {
            boolean isWorkflowConfigured = files
                .map(this::parseWorkflowFile)
                .filter(this::hasWorkflowJobs)
                .flatMap(workflow -> workflow.jobs().values().stream())
                .map(WorkflowJobDefinition::uses)
                .filter(Objects::nonNull)
                .anyMatch(jobDefinition -> jobDefinition.startsWith(getWorkflowDefinition()));

            return isWorkflowConfigured ?
                ProbeResult.success(key(), getSuccessMessage()) :
                ProbeResult.success(key(), getFailureMessage());
        } catch (IOException e) {
            LOGGER.warn("Couldn't not read file for plugin {} during probe {}", plugin.getName(), key(), e);
            return ProbeResult.error(key(), e.getMessage());
        }
    }

    /**
     * This method it reads a file, parses its Yaml content, and maps it to an object.
     *
     * @return a partial object mapping of the Yaml content of the file provided in the argument.
     */
    private WorkflowDefinition parseWorkflowFile(Path filePath) {
        final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
        try {
            return yaml.readValue(Files.newInputStream(filePath), WorkflowDefinition.class);
        } catch (IOException e) {
            LOGGER.warn("Couldn't not read {} for probe {}", filePath, key(), e);
            return new WorkflowDefinition(Map.of());
        }
    }

    /**
     * Checks if the map is null or empty. This means no GitHub action is defined.
     */
    private boolean hasWorkflowJobs(WorkflowDefinition workflow) {
        return workflow.jobs() != null && !workflow.jobs().isEmpty();
    }

    /**
     * Partial object mapping of a GitHub workflow YAML file, containing only the "jobs" that are defined in it.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WorkflowDefinition(Map<String, WorkflowJobDefinition> jobs) {
    }

    /**
     * Partial Object mapping of a GitHub workflow job definition, containing only the "uses" field of its YAML content.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WorkflowJobDefinition(String uses) {
    }

    /**
     * @return a String array of probes that should be executed before AbstractGitHubWorkflowProbe
     */
    @Override
    public String[] getProbeResultRequirement() {
        return new String[] { SCMLinkValidationProbe.KEY, LastCommitDateProbe.KEY };
    }

    @Override
    protected boolean isSourceCodeRelated() {
        return true;
    }

    /**
     * Returns the path to the GitHub Workflow definition which should be used in one of the actions of the plugin repository.
     *
     * @return the path of the workflow we are searching for.
     */
    public abstract String getWorkflowDefinition();

    /**
     * @return a failure message
     */
    public abstract String getFailureMessage();

    /**
     * @return a success message
     */
    public abstract String getSuccessMessage();
}
