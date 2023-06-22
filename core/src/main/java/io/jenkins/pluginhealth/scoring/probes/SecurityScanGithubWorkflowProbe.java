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
import java.nio.file.Paths;
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
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(SecurityScanGithubWorkflowProbe.ORDER)
public class SecurityScanGithubWorkflowProbe extends Probe {
    public static final int ORDER = LastCommitDateProbe.ORDER + 100;
    public static final String KEY = "security-scan";
    public static final String SEARCH_LINE = "jenkins-infra/jenkins-security-scan/.github/workflows/jenkins-security-scan.yaml@v2";
    private static final String WORKFLOWS_DIRECTORY = ".github/workflows";
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityScanGithubWorkflowProbe.class);

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final Path repository = context.getScmRepository();
        final Path workflowPath = Paths.get(repository.toString(), WORKFLOWS_DIRECTORY);

        if (! Files.exists(workflowPath)) {
            return ProbeResult.failure(key(), "GitHub workflow directory could not be found in the plugin");
        }

            try (Stream<Path> files = Files.find(workflowPath, 1,
                (path, basicFileAttributes) -> Files.isRegularFile(path)
            )) {
                final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

                return files
                    .map(file -> {
                        try {
                            return yaml.readValue(Files.newInputStream(file), SecurityScanGithubWorkflowProbe.WorkflowDefinition.class);
                        } catch (IOException e) {
                            LOGGER.error("Couldn't not read {} for {} on {}", file, key(), plugin.getName(), e);
                            return new SecurityScanGithubWorkflowProbe.WorkflowDefinition(Map.of());
                        }
                    })
                    .filter(wf -> wf.jobs() != null && !wf.jobs().isEmpty())
                    .flatMap(wf -> wf.jobs().values().stream())
                    .map(SecurityScanGithubWorkflowProbe.WorkflowJobDefinition::uses)
                    .filter(Objects::nonNull)
                    .anyMatch(def -> def.startsWith(SEARCH_LINE)) ?
                    ProbeResult.success(key(), "GitHub workflow security scan is configured in the plugin") :
                    ProbeResult.failure(key(), "GitHub workflow security scan is not configured in the plugin");

        } catch (IOException e) {
            return ProbeResult.error(key(), e.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WorkflowJobDefinition(String uses) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WorkflowDefinition(Map<String, SecurityScanGithubWorkflowProbe.WorkflowJobDefinition> jobs) {
    }


    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Checks if Security Scan is configured in GitHub workflow";
    }

    @Override
    protected boolean isSourceCodeRelated() {
        return true;
    }

    @Override
    public String[] getProbeResultRequirement() {
        return new String[] { SCMLinkValidationProbe.KEY, LastCommitDateProbe.KEY };
    }
}
