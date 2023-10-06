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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
import io.jenkins.pluginhealth.scoring.probes.UpdateCenterPluginPublicationProbe;

import org.junit.jupiter.api.Test;

class UpdateCenterPublishedPluginDetectionScoringTest extends AbstractScoringTest<UpdateCenterPublishedPluginDetectionScoring> {
    @Override
    UpdateCenterPublishedPluginDetectionScoring getSpy() {
        return spy(UpdateCenterPublishedPluginDetectionScoring.class);
    }

    @Test
    public void shouldScoreCorrectlyWhenPluginInUpdateCenter() {
        final Plugin plugin = mock(Plugin.class);
        final UpdateCenterPublishedPluginDetectionScoring scoring = spy(UpdateCenterPublishedPluginDetectionScoring.class);

        when(plugin.getDetails()).thenReturn(Map.of(
            UpdateCenterPluginPublicationProbe.KEY, ProbeResult.success(UpdateCenterPluginPublicationProbe.KEY, "This plugin is still actively published by the update-center.", 1)
        ));

        final ScoreResult result = scoring.apply(plugin);

        assertThat(result.key()).isEqualTo("update-center-plugin-publication");
        assertThat(result.weight()).isEqualTo(1f);
        assertThat(result.value()).isEqualTo(100);
    }

    @Test
    public void shouldBadlyScorePluginWithNoProbe() {
        final Plugin plugin = mock(Plugin.class);
        final UpdateCenterPublishedPluginDetectionScoring scoring = getSpy();

        when(plugin.getDetails()).thenReturn(Map.of());

        final ScoreResult result = scoring.apply(plugin);

        assertThat(result.key()).isEqualTo("update-center-plugin-publication");
        assertThat(result.weight()).isEqualTo(1f);
        assertThat(result.value()).isEqualTo(0);
    }

    @Test
    public void shouldScoreBadlyWhenPluginNotInUpdateCenter() {
        final Plugin plugin = mock(Plugin.class);
        final UpdateCenterPublishedPluginDetectionScoring scoring = getSpy();

        when(plugin.getDetails()).thenReturn(Map.of(
            UpdateCenterPluginPublicationProbe.KEY, ProbeResult.success(UpdateCenterPluginPublicationProbe.KEY, "", 1)
        ));

        final ScoreResult result = scoring.apply(plugin);

        assertThat(result.key()).isEqualTo("update-center-plugin-publication");
        assertThat(result.weight()).isEqualTo(1f);
        assertThat(result.value()).isEqualTo(0);
    }
}
