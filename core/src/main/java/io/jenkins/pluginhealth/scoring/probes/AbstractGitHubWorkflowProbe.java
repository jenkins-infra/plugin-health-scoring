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

    /**
     * Returns the path to the GitHub Workflow definition which should be use in one of the actions of the plugin repository.
     *
     * @return the workflow definition used in one of the jobs of one of the actions of the plugin repository.
     */
    public abstract String getWorkflowDefinition();

    /**
     * Ignores all other fields present in the yaml file.
     *
     * @return only the value of "jobs" field in the file
     * */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record WorkflowDefinition(Map<String, WorkflowJobDefinition> jobs) {
    }

    /**
     * Ignores all other fields present in the yaml file.
     *
     * @return only the value of "uses" field in the file
     * */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record WorkflowJobDefinition(String uses) {
    }

    /**
     * @return a failure message
     */
    public abstract String getFailureMessage();

    /**
     * @return a success message
     * */
    public abstract String getSuccessMessage();

}
