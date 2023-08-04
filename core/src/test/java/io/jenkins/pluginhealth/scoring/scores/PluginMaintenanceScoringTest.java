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
                0f
            ),
            arguments(// All bad
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "1"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, ""),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "")
                ),
                0f
            ),
            arguments(// All bad with open dependabot pull request
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "1"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, ""),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "")
                ),
                0f
            ),
            arguments(// All good
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "0"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, ""),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "")
                ),
                1f
            ),
            arguments(// Only Jenkinsfile
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, ""),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, ""),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "")
                ),
                .65f
            ),
            arguments(// Jenkinsfile and dependabot but with open pull request
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "1"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, ""),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "")
                ),
                .65f
            ),
            arguments(// Jenkinsfile and dependabot with no open pull request
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "0"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, ""),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "")
                ),
                .8f
            ),
            arguments(// Jenkinsfile and CD
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, ""),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, ""),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "")
                ),
                .7f
            ),
            arguments(// Jenkinsfile and documentation
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, ""),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, ""),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "")
                ),
                .8f
            ),
            arguments(// Jenkinsfile and CD and dependabot but with open pull request
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "1"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, ""),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "")
                ),
                .7f
            ),
            arguments(// Jenkinsfile and CD and dependabot with no open pull request
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "0"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, ""),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "")
                ),
                .85f
            ),
            arguments(// Dependabot only with no open pull requests
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "0"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, ""),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "")
                ),
                .15f
            ),
            arguments(// CD only
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, ""),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, ""),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "")
                ),
                .05f
            ),
            arguments(// Dependabot with no open pull request and CD
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "0"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, ""),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "")
                ),
                .2f
            ),
            arguments(// Dependabot with no open pull request and documentation
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "0"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, ""),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "")
                ),
                .3f
            ),
            arguments(// Documentation migration only
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, ""),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, ""),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "")
                ),
                .15f
            ),
            arguments(// Documentation migration and Dependabot but with open pull requests
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "1"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, ""),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "")
                ),
                .15f
            ),
            arguments(// Documentation migration and Dependabot with no open pull requests
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(DependabotPullRequestProbe.KEY, "0"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, ""),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "")
                ),
                .3f
            ),
            arguments(// Documentation migration and CD
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, ""),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(ContinuousDeliveryProbe.KEY, ""),
                    DocumentationMigrationProbe.KEY, ProbeResult.success(DocumentationMigrationProbe.KEY, "")
                ),
                .2f
            )
        );
    }

    @ParameterizedTest
    @MethodSource("probeResultsAndValue")
    public void shouldScorePluginBasedOnProbeResultMatrix(Map<String, ProbeResult> details, float value) {
        final Plugin plugin = mock(Plugin.class);
        final PluginMaintenanceScoring scoring = getSpy();

        when(plugin.getDetails()).thenReturn(details);

        final ScoreResult result = scoring.apply(plugin);

        assertThat(result)
            .isNotNull()
            .usingRecursiveComparison()
            .comparingOnlyFields("key", "value", "weight")
            .isEqualTo(new ScoreResult(KEY, value, COEFFICIENT, Set.of()));
    }
}
