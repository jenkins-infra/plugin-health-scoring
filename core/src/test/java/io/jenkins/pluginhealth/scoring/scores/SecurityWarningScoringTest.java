/*
 * MIT License
 *
 * Copyright (c) 2023-2024 Jenkins Infra
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.Resolution;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
import io.jenkins.pluginhealth.scoring.probes.KnownSecurityVulnerabilityProbe;

import org.junit.jupiter.api.Test;

class SecurityWarningScoringTest extends AbstractScoringTest<SecurityWarningScoring> {
    @Override
    SecurityWarningScoring getSpy() {
        return spy(SecurityWarningScoring.class);
    }

    @Test
    void shouldBeAbleToDetectPluginWithSingleSecurityWarning() {
        final Plugin plugin = mock(Plugin.class);
        final SecurityWarningScoring scoring = getSpy();

        when(plugin.getDetails())
                .thenReturn(Map.of(
                        KnownSecurityVulnerabilityProbe.KEY,
                        ProbeResult.success(
                                KnownSecurityVulnerabilityProbe.KEY, "SECURITY-123:link-to-security-advisory", 2)));

        final ScoreResult result = scoring.apply(plugin);

        assertThat(result.key()).isEqualTo("security");
        assertThat(result.weight()).isEqualTo(1f);
        assertThat(result.value()).isEqualTo(0);
        result.componentsResults().forEach(ScoringComponentResult -> {
            ScoringComponentResult.resolutions().forEach(resolution -> {
                assertThat(resolution.link()).isEqualTo("link-to-security-advisory");
                assertThat(resolution.text()).isEqualTo("SECURITY-123");
            });
        });
    }

    @Test
    void shouldBeAbleToDetectPluginWithMultipleSecurityWarning() {
        final Plugin plugin = mock(Plugin.class);
        final SecurityWarningScoring scoring = getSpy();

        when(plugin.getDetails())
                .thenReturn(Map.of(
                        KnownSecurityVulnerabilityProbe.KEY,
                        ProbeResult.success(
                                KnownSecurityVulnerabilityProbe.KEY,
                                "SECURITY-123:link-to-security-advisory, SECURITY-123:link-to-security-advisory",
                                2)));

        final ScoreResult result = scoring.apply(plugin);

        assertThat(result.key()).isEqualTo("security");
        assertThat(result.weight()).isEqualTo(1f);
        assertThat(result.value()).isEqualTo(0);

        List<Resolution> resolutionList = new ArrayList<>();
        result.componentsResults().forEach(ScoringComponentResult -> {
            ScoringComponentResult.reasons().forEach(message -> {
                if (!message.equals("Plugin seem to have on-going security advisory.")) {
                    String[] res = message.split(",");
                    Arrays.stream(res).forEach(securityMessage -> {
                        String[] securityInfo = securityMessage.split(":");
                        resolutionList.add(new Resolution(securityInfo[0].trim(), securityInfo[1]));
                    });
                }
            });
            AtomicInteger count = new AtomicInteger();

            ScoringComponentResult.resolutions().forEach(resolution -> {
                assertThat(resolutionList.get(0).link())
                        .isEqualTo(resolution.link().trim());
                assertThat(resolutionList.get(1).text())
                        .isEqualTo(resolution.text().trim());
                count.incrementAndGet();
            });
        });
    }

    @Test
    void shouldBadlyScorePluginWithNoProbeResult() {
        final Plugin plugin = mock(Plugin.class);
        final SecurityWarningScoring scoring = getSpy();

        when(plugin.getDetails()).thenReturn(Map.of());

        final ScoreResult result = scoring.apply(plugin);

        assertThat(result.key()).isEqualTo("security");
        assertThat(result.weight()).isEqualTo(1f);
        assertThat(result.value()).isEqualTo(0);
    }

    @Test
    void shouldBeAbleToDetectPluginWithNoSecurityWarning() {
        final Plugin plugin = mock(Plugin.class);
        final SecurityWarningScoring scoring = getSpy();

        when(plugin.getDetails())
                .thenReturn(Map.of(
                        KnownSecurityVulnerabilityProbe.KEY,
                        ProbeResult.success(
                                KnownSecurityVulnerabilityProbe.KEY, "No known security vulnerabilities.", 2)));

        final ScoreResult result = scoring.apply(plugin);

        assertThat(result.key()).isEqualTo("security");
        assertThat(result.weight()).isEqualTo(1f);
        assertThat(result.value()).isEqualTo(100);
    }
}
