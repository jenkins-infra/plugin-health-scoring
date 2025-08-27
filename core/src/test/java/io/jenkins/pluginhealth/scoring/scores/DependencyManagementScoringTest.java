/*
 * MIT License
 *
 * Copyright (c) 2025 Jenkins Infra
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
import io.jenkins.pluginhealth.scoring.probes.DependabotProbe;
import io.jenkins.pluginhealth.scoring.probes.DependabotPullRequestProbe;
import io.jenkins.pluginhealth.scoring.probes.MavenDependenciesProbe;
import io.jenkins.pluginhealth.scoring.probes.RenovateProbe;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DependencyManagementScoringTest extends AbstractScoringTest<DependencyManagementScoring> {

    @Override
    DependencyManagementScoring getSpy() {
        return spy(DependencyManagementScoring.class);
    }

    static Stream<Arguments> probeResultsAndValue() {
        return Stream.of(
                Arguments.arguments(Map.of(), 0),
                Arguments.arguments(
                        Map.of(
                                MavenDependenciesProbe.KEY,
                                ProbeResult.success(MavenDependenciesProbe.KEY, List.of(), 1)),
                        100),
                Arguments.arguments(
                        Map.of(
                                MavenDependenciesProbe.KEY,
                                ProbeResult.success(MavenDependenciesProbe.KEY, List.of("this-is-a-dependency"), 1)),
                        0),
                Arguments.arguments(
                        Map.of(
                                MavenDependenciesProbe.KEY,
                                ProbeResult.success(MavenDependenciesProbe.KEY, List.of("this-is-a-dependency"), 1),
                                RenovateProbe.KEY,
                                ProbeResult.success(RenovateProbe.KEY, "Renovate is configured.", 1)),
                        0),
                Arguments.arguments(
                        Map.of(
                                MavenDependenciesProbe.KEY,
                                ProbeResult.success(MavenDependenciesProbe.KEY, List.of("this-is-a-dependency"), 1),
                                DependabotProbe.KEY,
                                ProbeResult.success(DependabotProbe.KEY, "Dependabot is configured.", 1)),
                        0),
                Arguments.arguments(
                        Map.of(
                                MavenDependenciesProbe.KEY,
                                ProbeResult.success(MavenDependenciesProbe.KEY, List.of("this-is-a-dependency"), 1),
                                RenovateProbe.KEY,
                                ProbeResult.success(RenovateProbe.KEY, "Renovate is configured.", 1),
                                DependabotPullRequestProbe.KEY,
                                ProbeResult.success(DependabotPullRequestProbe.KEY, 1, 1)),
                        50),
                Arguments.arguments(
                        Map.of(
                                MavenDependenciesProbe.KEY,
                                ProbeResult.success(MavenDependenciesProbe.KEY, List.of("this-is-a-dependency"), 1),
                                DependabotProbe.KEY,
                                ProbeResult.success(DependabotProbe.KEY, "Dependabot is configured.", 1),
                                DependabotPullRequestProbe.KEY,
                                ProbeResult.success(DependabotPullRequestProbe.KEY, 1, 1)),
                        50),
                Arguments.arguments(
                        Map.of(
                                MavenDependenciesProbe.KEY,
                                ProbeResult.success(MavenDependenciesProbe.KEY, List.of("this-is-a-dependency"), 1),
                                RenovateProbe.KEY,
                                ProbeResult.success(RenovateProbe.KEY, "Renovate is configured.", 1),
                                DependabotPullRequestProbe.KEY,
                                ProbeResult.success(DependabotPullRequestProbe.KEY, 0, 1)),
                        100),
                Arguments.arguments(
                        Map.of(
                                MavenDependenciesProbe.KEY,
                                ProbeResult.success(MavenDependenciesProbe.KEY, List.of("this-is-a-dependency"), 1),
                                DependabotProbe.KEY,
                                ProbeResult.success(DependabotProbe.KEY, "Dependabot is configured.", 1),
                                DependabotPullRequestProbe.KEY,
                                ProbeResult.success(DependabotPullRequestProbe.KEY, 0, 1)),
                        100));
    }

    @ParameterizedTest
    @MethodSource("probeResultsAndValue")
    public void shouldScorePluginBasedOnProbeResultMatrix(Map<String, ProbeResult> details, int value) {
        final Plugin plugin = mock(Plugin.class);
        final DependencyManagementScoring scoring = getSpy();

        when(plugin.getDetails()).thenReturn(details);

        final ScoreResult result = scoring.apply(plugin);

        assertThat(result.componentsResults().size()).isEqualTo(1);
        assertThat(result.componentsResults().stream()
                        .noneMatch(cr -> cr.reasons().isEmpty()))
                .isTrue();
        assertThat(result)
                .isNotNull()
                .usingRecursiveComparison()
                .comparingOnlyFields("key", "value", "weight")
                .isEqualTo(new ScoreResult(DependencyManagementScoring.KEY, value, .2f, Set.of(), scoring.version()));
    }
}
