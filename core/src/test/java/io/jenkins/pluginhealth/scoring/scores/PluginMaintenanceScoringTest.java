/*
 * MIT License
 *
 * Copyright (c) 2022-2025 Jenkins Infra
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
import io.jenkins.pluginhealth.scoring.probes.CodeOwnershipProbe;
import io.jenkins.pluginhealth.scoring.probes.ContinuousDeliveryProbe;
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
                arguments( // Nothing
                        Map.of(), 0),
                arguments( // All bad
                        Map.of(
                                JenkinsfileProbe.KEY,
                                ProbeResult.success(JenkinsfileProbe.KEY, "No Jenkinsfile found", 1),
                                ContinuousDeliveryProbe.KEY,
                                ProbeResult.success(
                                        ContinuousDeliveryProbe.KEY, "Could not find JEP-229 workflow definition.", 1),
                                CodeOwnershipProbe.KEY,
                                ProbeResult.success(
                                        CodeOwnershipProbe.KEY, "No CODEOWNERS file found in plugin repository.", 1)),
                        0),
                arguments( // All good
                        Map.of(
                                JenkinsfileProbe.KEY,
                                ProbeResult.success(JenkinsfileProbe.KEY, "Jenkinsfile found", 1),
                                ContinuousDeliveryProbe.KEY,
                                ProbeResult.success(
                                        ContinuousDeliveryProbe.KEY, "JEP-229 workflow definition found.", 1),
                                CodeOwnershipProbe.KEY,
                                ProbeResult.success(CodeOwnershipProbe.KEY, "CODEOWNERS file is valid.", 1)),
                        100),
                arguments( // Only Jenkinsfile
                        Map.of(
                                JenkinsfileProbe.KEY,
                                ProbeResult.success(JenkinsfileProbe.KEY, "Jenkinsfile found", 1),
                                ContinuousDeliveryProbe.KEY,
                                ProbeResult.success(
                                        ContinuousDeliveryProbe.KEY, "Could not find JEP-229 workflow definition.", 1),
                                CodeOwnershipProbe.KEY,
                                ProbeResult.success(
                                        CodeOwnershipProbe.KEY, "No CODEOWNERS file found in plugin repository.", 1)),
                        71),
                arguments( // Jenkinsfile and CD
                        Map.of(
                                JenkinsfileProbe.KEY,
                                ProbeResult.success(JenkinsfileProbe.KEY, "Jenkinsfile found", 1),
                                ContinuousDeliveryProbe.KEY,
                                ProbeResult.success(
                                        ContinuousDeliveryProbe.KEY, "JEP-229 workflow definition found.", 1),
                                CodeOwnershipProbe.KEY,
                                ProbeResult.success(
                                        CodeOwnershipProbe.KEY, "No CODEOWNERS file found in plugin repository.", 1)),
                        71),
                arguments( // CD only
                        Map.of(
                                JenkinsfileProbe.KEY,
                                ProbeResult.success(JenkinsfileProbe.KEY, "No Jenkinsfile found", 1),
                                ContinuousDeliveryProbe.KEY,
                                ProbeResult.success(
                                        ContinuousDeliveryProbe.KEY, "JEP-229 workflow definition found.", 1),
                                CodeOwnershipProbe.KEY,
                                ProbeResult.success(
                                        CodeOwnershipProbe.KEY, "No CODEOWNERS file found in plugin repository.", 1)),
                        0),
                arguments( // Codeownership only
                        Map.of(
                                JenkinsfileProbe.KEY,
                                ProbeResult.success(JenkinsfileProbe.KEY, "No Jenkinsfile found", 1),
                                ContinuousDeliveryProbe.KEY,
                                ProbeResult.success(
                                        ContinuousDeliveryProbe.KEY, "Could not find JEP-229 workflow definition.", 1),
                                CodeOwnershipProbe.KEY,
                                ProbeResult.success(CodeOwnershipProbe.KEY, "CODEOWNERS file is valid.", 1)),
                        29),
                arguments( // Codeownership + Jenkinsfile
                        Map.of(
                                JenkinsfileProbe.KEY,
                                ProbeResult.success(JenkinsfileProbe.KEY, "Jenkinsfile found", 1),
                                ContinuousDeliveryProbe.KEY,
                                ProbeResult.success(
                                        ContinuousDeliveryProbe.KEY, "Could not find JEP-229 workflow definition.", 1),
                                CodeOwnershipProbe.KEY,
                                ProbeResult.success(CodeOwnershipProbe.KEY, "CODEOWNERS file is valid.", 1)),
                        100),
                arguments( // Codeownership + CD
                        Map.of(
                                JenkinsfileProbe.KEY,
                                ProbeResult.success(JenkinsfileProbe.KEY, "Jenkinsfile not found", 1),
                                ContinuousDeliveryProbe.KEY,
                                ProbeResult.success(
                                        ContinuousDeliveryProbe.KEY, "JEP-229 workflow definition found.", 1),
                                CodeOwnershipProbe.KEY,
                                ProbeResult.success(CodeOwnershipProbe.KEY, "CODEOWNERS file is valid.", 1)),
                        29));
    }

    @ParameterizedTest
    @MethodSource("probeResultsAndValue")
    public void shouldScorePluginBasedOnProbeResultMatrix(Map<String, ProbeResult> details, int value) {
        final Plugin plugin = mock(Plugin.class);
        final PluginMaintenanceScoring scoring = getSpy();

        when(plugin.getDetails()).thenReturn(details);

        final ScoreResult result = scoring.apply(plugin);

        assertThat(result.componentsResults().size()).isEqualTo(3);
        assertThat(result.componentsResults().stream()
                        .noneMatch(cr -> cr.reasons().isEmpty()))
                .isTrue();
        assertThat(result)
                .isNotNull()
                .usingRecursiveComparison()
                .comparingOnlyFields("key", "value", "weight")
                .isEqualTo(new ScoreResult(KEY, value, COEFFICIENT, Set.of(), scoring.version()));
    }
}
