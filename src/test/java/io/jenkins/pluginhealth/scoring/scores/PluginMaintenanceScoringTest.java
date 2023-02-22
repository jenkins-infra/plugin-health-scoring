/*
 * MIT License
 *
 * Copyright (c) 2022 Jenkins Infra
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
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
import io.jenkins.pluginhealth.scoring.probes.ContinuousDeliveryProbe;
import io.jenkins.pluginhealth.scoring.probes.ContributingGuidelinesProbe;
import io.jenkins.pluginhealth.scoring.probes.DependabotProbe;
import io.jenkins.pluginhealth.scoring.probes.DependabotPullRequestProbe;
import io.jenkins.pluginhealth.scoring.probes.JenkinsfileProbe;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PluginMaintenanceScoringTest {
    public static final String KEY = "repository-configuration";
    public static final float COEFFICIENT = .5f;

    static Stream<Arguments> probeResultsAndValue() {
        return Stream.of(
            arguments(// Nothing
                Map.of(),
                0f
            ),
            arguments(// All bad
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, "1"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    ContributingGuidelinesProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, "")
                ),
                0f
            ),
            arguments(// All bad with open dependabot pull request
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "1"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    ContributingGuidelinesProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, "")
                ),
                0f
            ),
            arguments(// All good
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "0"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    ContributingGuidelinesProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "")
                ),
                1f
            ),
            arguments(// Only Jenkinsfile
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    ContinuousDeliveryProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    ContributingGuidelinesProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, "")
                ),
                .65f
            ),
            arguments(// Jenkinsfile and dependabot but with open pull request
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "1"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    ContributingGuidelinesProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, "")
                ),
                .65f
            ),
            arguments(// Jenkinsfile and dependabot with no open pull request
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "0"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    ContributingGuidelinesProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, "")
                ),
                .8f
            ),
            arguments(// Jenkinsfile and CD
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, "0"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    ContributingGuidelinesProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, "")
                ),
                .7f
            ),
            arguments(// Jenkinsfile and Contributing guide
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    ContinuousDeliveryProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    ContributingGuidelinesProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "")
                ),
                .8f
            ),
            arguments(// Jenkinsfile and CD and dependabot but with open pull request
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "1"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    ContributingGuidelinesProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, "")
                ),
                .7f
            ),
            arguments(// Jenkinsfile and CD and dependabot with no open pull request
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "0"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    ContributingGuidelinesProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, "")
                ),
                .85f
            ),
            arguments(// Jenkinsfile and Contributing guide and dependabot and with no open pull request
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "0"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    ContributingGuidelinesProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "")
                ),
                .95f
            ),
            arguments(// Jenkinfile and CD and Contributing guild and dependabot but with open pull request
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "1"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    ContributingGuidelinesProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "")
                ),
                .85f
            ),
            arguments(// Contributing guide only
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    ContinuousDeliveryProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    ContributingGuidelinesProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "")
                ),
                .15f
            ),
            arguments(// Dependabot only with no open pull requests
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "0"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    ContributingGuidelinesProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, "")
                ),
                .15f
            ),
            arguments(// Contributing guide and Dependabot with no open pull requests
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "0"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    ContributingGuidelinesProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "")
                ),
                .3f
            ),
            arguments(// Dependabot with no open pull request and CD and Contributing guide
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "0"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    ContributingGuidelinesProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "")
                ),
                .35f
            ),
            arguments(// Dependabot with no open pull request and CD
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "0"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    ContributingGuidelinesProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, "")
                ),
                .2f
            ),
            arguments(// Dependabot but with open pull request and Contributing guide and CD
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "1"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    ContributingGuidelinesProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "")
                ),
                .2f
            ),
            arguments(// Contributing guide and CD
                Map.of(
                    JenkinsfileProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    DependabotProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, ""),
                    DependabotPullRequestProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, "0"),
                    ContinuousDeliveryProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, ""),
                    ContributingGuidelinesProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "")
                ),
                .2f
            )
        );
    }

    @ParameterizedTest
    @MethodSource("probeResultsAndValue")
    public void shouldScorePluginBasedOnProbeResultMatrix(Map<String, ProbeResult> details, float value) {
        final Plugin plugin = mock(Plugin.class);
        final PluginMaintenanceScoring scoring = spy(PluginMaintenanceScoring.class);

        when(plugin.getDetails()).thenReturn(details);

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.key())
            .withFailMessage(() -> "Score key should be '%s'".formatted(KEY))
            .isEqualTo(KEY);
        assertThat(result.coefficient())
            .withFailMessage(() -> "Score coefficient should be '%f'".formatted(COEFFICIENT))
            .isEqualTo(COEFFICIENT);
        assertThat(result.value())
            .isEqualTo(value);
    }
}
