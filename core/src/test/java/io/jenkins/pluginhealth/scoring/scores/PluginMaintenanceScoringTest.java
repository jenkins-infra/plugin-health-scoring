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

package io.jenkins.pluginhealth.scoring.scores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
import io.jenkins.pluginhealth.scoring.probes.ContinuousDeliveryProbe;
import io.jenkins.pluginhealth.scoring.probes.DependabotProbe;
import io.jenkins.pluginhealth.scoring.probes.DependabotPullRequestProbe;
import io.jenkins.pluginhealth.scoring.probes.DocumentationMigrationProbe;
import io.jenkins.pluginhealth.scoring.probes.JenkinsfileProbe;
import io.jenkins.pluginhealth.scoring.probes.RenovateProbe;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PluginMaintenanceScoringTest extends AbstractScoringTest<PluginMaintenanceScoring> {
    public static final String KEY = "repository-configuration";
    public static final float COEFFICIENT = .5f;

    @Override
    PluginMaintenanceScoring getSpy() {
        return spy(PluginMaintenanceScoring.class);
    }

    static Stream<Arguments> probeResultsAndValue() {
        return Stream.of(
            arguments(// Nothing
                Map.of(),
                0
            ),
            arguments(// All bad
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "No Jenkinsfile found", 1),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, "dependabot is not configured.", 1),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "1", 1),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, "Could not find JEP-229 workflow definition.", 1),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "Documentation is not located in the plugin repository.", 1)
                ),
                0
            ),
            arguments(// All bad with open dependabot pull request
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "No Jenkinsfile found", 1),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, "dependabot is not configured.", 1),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "1", 1),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, "Could not find JEP-229 workflow definition.", 1),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "Documentation is not located in the plugin repository.", 1)
                ),
                0
            ),
            arguments(// All good
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "Jenkinsfile found", 1),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, "Dependabot is configured.", 1),
                    RenovateProbe.KEY, ProbeResult.success(RenovateProbe.KEY, "Renovate is not configured.", 1),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "0", 1),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, "JEP-229 workflow definition found.", 1),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "Documentation is located in the plugin repository.", 1)
                ),
                100
            ),
            arguments(// All good
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "Jenkinsfile found", 1),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, "Dependabot is not configured.", 1),
                    RenovateProbe.KEY, ProbeResult.success(RenovateProbe.KEY, "Renovate is configured.", 1),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "0", 1),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, "JEP-229 workflow definition found.", 1),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "Documentation is located in the plugin repository.", 1)
                ),
                100
            ),
            arguments(// Only Jenkinsfile
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "Jenkinsfile found", 1),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, "Dependabot is not configured.", 1),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, "Could not find JEP-229 workflow definition.", 1),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "Documentation is not located in the plugin repository.", 1)
                ),
                65
            ),
            arguments(// Jenkinsfile and dependabot but with open pull request
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "Jenkinsfile found", 1),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, "Dependabot is configured.", 1),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "1", 1),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, "Could not find JEP-229 workflow definition.", 1),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "Documentation is not located in the plugin repository.", 1)
                ),
                73
            ),
            arguments(// Jenkinsfile and dependabot with no open pull request
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "Jenkinsfile found", 1),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, "Dependabot is configured.", 1),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "0", 1),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, "Could not find JEP-229 workflow definition.", 1),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "Documentation is not located in the plugin repository.", 1)
                ),
                80
            ),
            arguments(// Jenkinsfile and CD
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "Jenkinsfile found", 1),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, "Dependabot is not configured.", 1),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, "JEP-229 workflow definition found.", 1),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "Documentation is not located in the plugin repository.", 1)
                ),
                70
            ),
            arguments(// Jenkinsfile and documentation
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "Jenkinsfile found", 1),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, "Dependabot is not configured.", 1),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, "Could not find JEP-229 workflow definition.", 1),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "Documentation is located in the plugin repository.", 1)
                ),
                80
            ),
            arguments(// Jenkinsfile and CD and dependabot but with open pull request
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "Jenkinsfile found", 1),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, "Dependabot is configured.", 1),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "1", 1),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, "JEP-229 workflow definition found.", 1),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "Documentation is not located in the plugin repository.", 1)
                ),
                78
            ),
            arguments(// Jenkinsfile and CD and dependabot with no open pull request
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "Jenkinsfile found", 1),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, "Dependabot is configured.", 1),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "0", 1),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, "JEP-229 workflow definition found.", 1),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "Documentation is not located in the plugin repository.", 1)
                ),
                85
            ),
            arguments(// Dependabot only with no open pull requests
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "No Jenkinsfile found", 1),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, "Dependabot is configured.", 1),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "0", 1),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, "Could not find JEP-229 workflow definition.", 1),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "Documentation is not located in the plugin repository.", 1)
                ),
                15
            ),
            arguments(// CD only
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "No Jenkinsfile found", 1),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, "Dependabot is not configured.", 1),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, "JEP-229 workflow definition found.", 1),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "Documentation is not located in the plugin repository.", 1)
                ),
                5
            ),
            arguments(// Dependabot with no open pull request and CD
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "No Jenkinsfile found", 1),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, "Dependabot is configured.", 1),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "0", 1),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, "JEP-229 workflow definition found.", 1),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "Documentation is not located in the plugin repository.", 1)
                ),
                20
            ),
            arguments(// Dependabot with no open pull request and documentation
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "No Jenkinsfile found", 1),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, "Dependabot is configured.", 1),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "0", 1),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, "Could not find JEP-229 workflow definition.", 1),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "Documentation is located in the plugin repository.", 1)
                ),
                30
            ),
            arguments(// Documentation migration only
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "No Jenkinsfile found", 1),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, "Dependabot is not configured.", 1),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, "Could not find JEP-229 workflow definition.", 1),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "Documentation is located in the plugin repository.", 1)
                ),
                15
            ),
            arguments(// Documentation migration and Dependabot but with open pull requests
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "No Jenkinsfile found", 1),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, "Dependabot is configured.", 1),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "1", 1),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, "Could not find JEP-229 workflow definition.", 1),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "Documentation is located in the plugin repository.", 1)
                ),
                23
            ),
            arguments(// Documentation migration and Dependabot with no open pull requests
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "No Jenkinsfile found", 1),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, "Dependabot is configured.", 1),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "0", 1),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, "Could not find JEP-229 workflow definition.", 1),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "Documentation is located in the plugin repository.", 1)
                ),
                30
            ),
            arguments(// Documentation migration and CD
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "No Jenkinsfile found", 1),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, "Dependabot is not configured.", 1),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, "JEP-229 workflow definition found.", 1),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "Documentation is located in the plugin repository.", 1)
                ),
                20
            )
        );
    }

    @ParameterizedTest
    @MethodSource("probeResultsAndValue")
    public void shouldScorePluginBasedOnProbeResultMatrix(Map<String, ProbeResult> details, int value) {
        final Plugin plugin = mock(Plugin.class);
        final PluginMaintenanceScoring scoring = getSpy();

        when(plugin.getDetails()).thenReturn(details);

        final ScoreResult result = scoring.apply(plugin);

        assertThat(result)
            .isNotNull()
            .usingRecursiveComparison()
            .comparingOnlyFields("key", "value", "weight")
            .isEqualTo(new ScoreResult(KEY, value, COEFFICIENT, Set.of(), scoring.version()));
    }
}
