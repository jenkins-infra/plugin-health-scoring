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
import io.jenkins.pluginhealth.scoring.probes.KnownSecurityVulnerabilityProbe;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecurityWarningScoringTest {
    @Test
    void shouldBeAbleToDetectPluginWithSecurityWarning() {
        final Plugin plugin = mock(Plugin.class);
        final SecurityWarningScoring scoring = spy(SecurityWarningScoring.class);

        when(plugin.getDetails()).thenReturn(Map.of(
            KnownSecurityVulnerabilityProbe.KEY, new ProbeResult(KnownSecurityVulnerabilityProbe.KEY, "", ResultStatus.FAILURE)
        ));

        final ScoreResult result = scoring.apply(plugin);

        assertThat(result.key()).isEqualTo("security");
        assertThat(result.coefficient()).isEqualTo(1f);
        assertThat(result.value()).isEqualTo(0f);
    }

    @Test
    void shouldBadlyScorePluginWithNoProbeResult() {
        final Plugin plugin = mock(Plugin.class);
        final SecurityWarningScoring scoring = spy(SecurityWarningScoring.class);

        when(plugin.getDetails()).thenReturn(Map.of());

        final ScoreResult result = scoring.apply(plugin);

        assertThat(result.key()).isEqualTo("security");
        assertThat(result.coefficient()).isEqualTo(1f);
        assertThat(result.value()).isEqualTo(0f);
    }

    @Test
    void shouldBeAbleToDetectPluginWithNoSecurityWarning() {
        final Plugin plugin = mock(Plugin.class);
        final SecurityWarningScoring scoring = spy(SecurityWarningScoring.class);

        when(plugin.getDetails()).thenReturn(Map.of(
            KnownSecurityVulnerabilityProbe.KEY, new ProbeResult(KnownSecurityVulnerabilityProbe.KEY, "", ResultStatus.SUCCESS)
        ));

        final ScoreResult result = scoring.apply(plugin);

        assertThat(result.key()).isEqualTo("security");
        assertThat(result.coefficient()).isEqualTo(1f);
        assertThat(result.value()).isEqualTo(1f);
    }
}
