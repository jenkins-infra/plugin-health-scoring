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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.utility.GitHubWorkflowReader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(ContinuousDeliveryProbe.ORDER)
public class ContinuousDeliveryProbe extends GitHubWorkflowReader {
    public static final int ORDER = LastCommitDateProbe.ORDER + 100;
    public static final String KEY = "jep-229";
    private static final Logger LOGGER = LoggerFactory.getLogger(ContinuousDeliveryProbe.class);
    private static final String WORKFLOWS_DIRECTORY = ".github/workflows";
    final String MAVEN_CD_FILE_PATH = "jenkins-infra/github-reusable-workflows/.github/workflows/maven-cd.yml";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final Path repo = context.getScmRepository();
        final Path githubWorkflow = repo.resolve(WORKFLOWS_DIRECTORY);

        if (Files.notExists(githubWorkflow)) {
            return ProbeResult.failure(key(), "Plugin has no GitHub Action configured");
        }
        return getWorkflowDefinition(githubWorkflow).startsWith(MAVEN_CD_FILE_PATH) ?
            ProbeResult.success(key(), "JEP-229 workflow definition found") :
            ProbeResult.failure(key(), "Could not find JEP-229 workflow definition");
    }

    @Override
    public String getWorkflowDefinition(Path worflowPath) {
        try (Stream<Path> files = Files.find(worflowPath, 1,
            (path, basicFileAttributes) -> Files.isRegularFile(path)
        )) {
            final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

            return files
                .map(file -> {
                    try {
                        return yaml.readValue(Files.newInputStream(file), WorkflowDefinition.class);
                    } catch (IOException e) {
                        LOGGER.error("Couldn't not read {} for {} on {}", file, key(), e);
                        return new WorkflowDefinition(Map.of());
                    }
                }).filter(wf -> wf.jobs() != null && !wf.jobs().isEmpty())
                .flatMap(wf -> wf.jobs().values().stream())
                .map(WorkflowJobDefinition::uses)
                .collect(Collectors.joining(","));
        } catch (IOException e) {
            LOGGER.warn("Could not walk {} Git clone {}", key(), e);
            return "";
        }
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Checks if JEP-229 (Continuous Delivery) has been activated on the plugin";
    }

    @Override
    protected boolean isSourceCodeRelated() {
        return true;
    }

    @Override
    public String[] getProbeResultRequirement() {
        return new String[]{SCMLinkValidationProbe.KEY, LastCommitDateProbe.KEY};
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WorkflowDefinition(Map<String, WorkflowJobDefinition> jobs) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WorkflowJobDefinition(String uses) {
    }
}
