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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
import io.jenkins.pluginhealth.scoring.probes.ContinuousDeliveryProbe;
import io.jenkins.pluginhealth.scoring.probes.DependabotProbe;
import io.jenkins.pluginhealth.scoring.probes.DependabotPullRequestProbe;
import io.jenkins.pluginhealth.scoring.probes.JenkinsfileProbe;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PluginMaintenanceScoringTest {
    public static final String KEY = "repository-configuration";
    public static final float COEFFICIENT = .5f;

    @Test
    void shouldScoreZeroWhenNoJenkinsfileProbeResult() {
        final Plugin plugin = mock(Plugin.class);
        final PluginMaintenanceScoring scoring = spy(PluginMaintenanceScoring.class);

        when(plugin.getDetails()).thenReturn(Map.of());

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.key()).withFailMessage(() -> "Score key should be '%s'".formatted(KEY)).isEqualTo(KEY);
        assertThat(result.coefficient()).withFailMessage(() -> "Score coefficient should be '%f'".formatted(COEFFICIENT)).isEqualTo(COEFFICIENT);
        assertThat(result.value()).isEqualTo(0f);
    }

    @Test
    void shouldScoreZeroForPluginsWithNoJenkinsfile() {
        final Plugin plugin = mock(Plugin.class);
        final PluginMaintenanceScoring scoring = spy(PluginMaintenanceScoring.class);

        when(plugin.getDetails()).thenReturn(Map.of(
            JenkinsfileProbe.KEY, new ProbeResult(JenkinsfileProbe.KEY, "", ResultStatus.FAILURE)
        ));

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.key()).withFailMessage(() -> "Score key should be '%s'".formatted(KEY)).isEqualTo(KEY);
        assertThat(result.coefficient()).withFailMessage(() -> "Score coefficient should be '%f'".formatted(COEFFICIENT)).isEqualTo(COEFFICIENT);
        assertThat(result.value()).isEqualTo(0f);
    }

    @Test
    void shouldScoreSeventyFiveForPluginsWithOnlyJenkinsfile() {
        final Plugin plugin = mock(Plugin.class);
        final PluginMaintenanceScoring scoring = spy(PluginMaintenanceScoring.class);

        when(plugin.getDetails()).thenReturn(Map.of(
            JenkinsfileProbe.KEY, new ProbeResult(JenkinsfileProbe.KEY, "", ResultStatus.SUCCESS)
        ));

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.key()).withFailMessage(() -> "Score key should be '%s'".formatted(KEY)).isEqualTo(KEY);
        assertThat(result.coefficient()).withFailMessage(() -> "Score coefficient should be '%f'".formatted(COEFFICIENT)).isEqualTo(COEFFICIENT);
        assertThat(result.value()).isEqualTo(.75f);
    }

    @Test
    void shouldScoreSeventyFiveForPluginsWithJenkinsfileAndNoDependabot() {
        final Plugin plugin = mock(Plugin.class);
        final PluginMaintenanceScoring scoring = spy(PluginMaintenanceScoring.class);

        when(plugin.getDetails()).thenReturn(Map.of(
            JenkinsfileProbe.KEY, new ProbeResult(JenkinsfileProbe.KEY, "", ResultStatus.SUCCESS),
            DependabotProbe.KEY, new ProbeResult(DependabotProbe.KEY, "", ResultStatus.FAILURE)
        ));

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.key()).withFailMessage(() -> "Score key should be '%s'".formatted(KEY)).isEqualTo(KEY);
        assertThat(result.coefficient()).withFailMessage(() -> "Score coefficient should be '%f'".formatted(COEFFICIENT)).isEqualTo(COEFFICIENT);
        assertThat(result.value()).isEqualTo(.75f);
    }

    @Test
    void shouldScoreSeventyFiveForPluginsWithJenkinsfileAndDependabotButOpenedPullRequests() {
        final Plugin plugin = mock(Plugin.class);
        final PluginMaintenanceScoring scoring = spy(PluginMaintenanceScoring.class);

        when(plugin.getDetails()).thenReturn(Map.of(
            JenkinsfileProbe.KEY, new ProbeResult(JenkinsfileProbe.KEY, "", ResultStatus.SUCCESS),
            DependabotProbe.KEY, new ProbeResult(DependabotProbe.KEY, "", ResultStatus.SUCCESS),
            DependabotPullRequestProbe.KEY, new ProbeResult(DependabotPullRequestProbe.KEY, "1", ResultStatus.SUCCESS)
        ));

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.key()).withFailMessage(() -> "Score key should be '%s'".formatted(KEY)).isEqualTo(KEY);
        assertThat(result.coefficient()).withFailMessage(() -> "Score coefficient should be '%f'".formatted(COEFFICIENT)).isEqualTo(COEFFICIENT);
        assertThat(result.value()).isEqualTo(.75f);
    }

    @Test
    void shouldScoreNinetyForPluginsWithOnlyJenkinsfileAndDependabot() {
        final Plugin plugin = mock(Plugin.class);
        final PluginMaintenanceScoring scoring = spy(PluginMaintenanceScoring.class);

        when(plugin.getDetails()).thenReturn(Map.of(
            JenkinsfileProbe.KEY, new ProbeResult(JenkinsfileProbe.KEY, "", ResultStatus.SUCCESS),
            DependabotProbe.KEY, new ProbeResult(DependabotProbe.KEY, "", ResultStatus.SUCCESS),
            DependabotPullRequestProbe.KEY, new ProbeResult(DependabotPullRequestProbe.KEY, "0", ResultStatus.SUCCESS)
        ));

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.key()).withFailMessage(() -> "Score key should be '%s'".formatted(KEY)).isEqualTo(KEY);
        assertThat(result.coefficient()).withFailMessage(() -> "Score coefficient should be '%f'".formatted(COEFFICIENT)).isEqualTo(COEFFICIENT);
        assertThat(result.value()).isEqualTo(.9f);
    }

    @Test
    void shouldScoreNinetyForPluginsWithJenkinsfileAndDependabotButNoJEP229() {
        final Plugin plugin = mock(Plugin.class);
        final PluginMaintenanceScoring scoring = spy(PluginMaintenanceScoring.class);

        when(plugin.getDetails()).thenReturn(Map.of(
            JenkinsfileProbe.KEY, new ProbeResult(JenkinsfileProbe.KEY, "", ResultStatus.SUCCESS),
            DependabotProbe.KEY, new ProbeResult(DependabotProbe.KEY, "", ResultStatus.SUCCESS),
            DependabotPullRequestProbe.KEY, new ProbeResult(DependabotPullRequestProbe.KEY, "0", ResultStatus.SUCCESS),
            ContinuousDeliveryProbe.KEY, new ProbeResult(ContinuousDeliveryProbe.KEY, "", ResultStatus.FAILURE)
        ));

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.key()).withFailMessage(() -> "Score key should be '%s'".formatted(KEY)).isEqualTo(KEY);
        assertThat(result.coefficient()).withFailMessage(() -> "Score coefficient should be '%f'".formatted(COEFFICIENT)).isEqualTo(COEFFICIENT);
        assertThat(result.value()).isEqualTo(.9f);
    }

    @Test
    void shouldScoreHundredForPluginsWithJenkinsfileAndDependabotAndJEP229() {
        final Plugin plugin = mock(Plugin.class);
        final PluginMaintenanceScoring scoring = spy(PluginMaintenanceScoring.class);

        when(plugin.getDetails()).thenReturn(Map.of(
            JenkinsfileProbe.KEY, new ProbeResult(JenkinsfileProbe.KEY, "", ResultStatus.SUCCESS),
            DependabotProbe.KEY, new ProbeResult(DependabotProbe.KEY, "", ResultStatus.SUCCESS),
            DependabotPullRequestProbe.KEY, new ProbeResult(DependabotPullRequestProbe.KEY, "0", ResultStatus.SUCCESS),
            ContinuousDeliveryProbe.KEY, new ProbeResult(ContinuousDeliveryProbe.KEY, "", ResultStatus.SUCCESS)
        ));

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.key()).withFailMessage(() -> "Score key should be '%s'".formatted(KEY)).isEqualTo(KEY);
        assertThat(result.coefficient()).withFailMessage(() -> "Score coefficient should be '%f'".formatted(COEFFICIENT)).isEqualTo(COEFFICIENT);
        assertThat(result.value()).isEqualTo(1f);
    }
}
