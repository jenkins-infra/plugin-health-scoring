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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
import io.jenkins.pluginhealth.scoring.probes.LastCommitDateProbe;
import io.jenkins.pluginhealth.scoring.probes.UpForAdoptionProbe;

import org.junit.jupiter.api.Test;

class AdoptionScoringTest extends AbstractScoringTest<AdoptionScoring> {
    @Override
    AdoptionScoring getSpy() {
        return spy(AdoptionScoring.class);
    }

    @Test
    void shouldScoreZeroForPluginsUpForAdoption() {
        final AdoptionScoring scoring = getSpy();
        final Plugin plugin = mock(Plugin.class);

        when(plugin.getDetails())
                .thenReturn(Map.of(
                        UpForAdoptionProbe.KEY,
                        ProbeResult.success(UpForAdoptionProbe.KEY, "This plugin is up for adoption.", 1)));

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.key()).isEqualTo("adoption");
        assertThat(result.weight()).isEqualTo(.8f);
        assertThat(result.value()).isEqualTo(0);
        assertThat(result.componentsResults().stream().flatMap(scr -> scr.reasons().stream()))
                .contains("Cannot determine the last commit date.");
    }

    @Test
    void shouldScoreZeroForPluginsUpForAdoptionEvenWithRecentCommit() {
        final AdoptionScoring scoring = getSpy();
        final Plugin plugin = mock(Plugin.class);

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusHours(4));
        when(plugin.getDetails())
                .thenReturn(Map.of(
                        UpForAdoptionProbe.KEY,
                                ProbeResult.success(UpForAdoptionProbe.KEY, "This plugin is up for adoption.", 1),
                        LastCommitDateProbe.KEY,
                                ProbeResult.success(
                                        LastCommitDateProbe.KEY,
                                        ZonedDateTime.now().minusHours(3).format(DateTimeFormatter.ISO_DATE_TIME),
                                        1)));

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.key()).isEqualTo("adoption");
        assertThat(result.weight()).isEqualTo(.8f);
        assertThat(result.value()).isEqualTo(0);
    }

    @Test
    void shouldScoreZeroForPluginsWithNoLastCommit() {
        final AdoptionScoring scoring = getSpy();
        final Plugin plugin = mock(Plugin.class);

        when(plugin.getDetails())
                .thenReturn(Map.of(
                        UpForAdoptionProbe.KEY,
                        ProbeResult.success(UpForAdoptionProbe.KEY, "This plugin is not up for adoption.", 1)));

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.value()).isEqualTo(0);
        assertThat(result.componentsResults().stream().flatMap(scr -> scr.reasons().stream()))
                .contains("Cannot determine the last commit date.");
    }

    @Test
    void shouldScoreHundredForPluginsWithCommitsLessThanSixMonthsOld() {
        final AdoptionScoring scoring = getSpy();
        final Plugin plugin = mock(Plugin.class);

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusMonths(2));
        when(plugin.getDetails())
                .thenReturn(Map.of(
                        UpForAdoptionProbe.KEY,
                                ProbeResult.success(UpForAdoptionProbe.KEY, "This plugin is not up for adoption.", 1),
                        LastCommitDateProbe.KEY,
                                ProbeResult.success(
                                        LastCommitDateProbe.KEY,
                                        ZonedDateTime.now().minusHours(3).format(DateTimeFormatter.ISO_DATE_TIME),
                                        1)));

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.value()).isEqualTo(100);
        assertThat(result.componentsResults().stream().flatMap(scr -> scr.reasons().stream()))
                .contains("Less than 6 months gap between last release and last commit.");
    }

    @Test
    void shouldScoreEightyForPluginsWithCommitsLessThanOneYearOld() {
        final AdoptionScoring scoring = getSpy();
        final Plugin plugin = mock(Plugin.class);

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusMonths(8));
        when(plugin.getDetails())
                .thenReturn(Map.of(
                        UpForAdoptionProbe.KEY,
                                ProbeResult.success(UpForAdoptionProbe.KEY, "This plugin is not up for adoption.", 1),
                        LastCommitDateProbe.KEY,
                                ProbeResult.success(
                                        LastCommitDateProbe.KEY,
                                        ZonedDateTime.now().minusHours(3).format(DateTimeFormatter.ISO_DATE_TIME),
                                        1)));

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.value()).isEqualTo(80);
        assertThat(result.componentsResults().stream().flatMap(scr -> scr.reasons().stream()))
                .contains("Less than a year between last release and last commit.");
    }

    @Test
    void shouldScoreSixtyForPluginsWithCommitsLessThanTwoYearsOld() {
        final AdoptionScoring scoring = getSpy();
        final Plugin plugin = mock(Plugin.class);

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusMonths(18));
        when(plugin.getDetails())
                .thenReturn(Map.of(
                        UpForAdoptionProbe.KEY,
                                ProbeResult.success(UpForAdoptionProbe.KEY, "This plugin is not up for adoption.", 1),
                        LastCommitDateProbe.KEY,
                                ProbeResult.success(
                                        LastCommitDateProbe.KEY,
                                        ZonedDateTime.now().minusHours(3).format(DateTimeFormatter.ISO_DATE_TIME),
                                        1)));

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.value()).isEqualTo(60);
        assertThat(result.componentsResults().stream().flatMap(scr -> scr.reasons().stream()))
                .contains("Less than 2 years between last release and last commit.");
    }

    @Test
    void shouldScoreFortyForPluginsWithCommitsLessThanFourYearsOld() {
        final AdoptionScoring scoring = getSpy();
        final Plugin plugin = mock(Plugin.class);

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusYears(3));
        when(plugin.getDetails())
                .thenReturn(Map.of(
                        UpForAdoptionProbe.KEY,
                                ProbeResult.success(UpForAdoptionProbe.KEY, "This plugin is not up for adoption.", 1),
                        LastCommitDateProbe.KEY,
                                ProbeResult.success(
                                        LastCommitDateProbe.KEY,
                                        ZonedDateTime.now().minusHours(3).format(DateTimeFormatter.ISO_DATE_TIME),
                                        1)));

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.value()).isEqualTo(40);
        assertThat(result.componentsResults().stream().flatMap(scr -> scr.reasons().stream()))
                .contains("Less than 4 years between last release and last commit.");
    }

    @Test
    void shouldScoreZeroForPluginsWithCommitsMoreThanFourYearsOld() {
        final AdoptionScoring scoring = getSpy();
        final Plugin plugin = mock(Plugin.class);

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusYears(5));
        when(plugin.getDetails())
                .thenReturn(Map.of(
                        UpForAdoptionProbe.KEY,
                                ProbeResult.success(UpForAdoptionProbe.KEY, "This plugin is not up for adoption.", 1),
                        LastCommitDateProbe.KEY,
                                ProbeResult.success(
                                        LastCommitDateProbe.KEY,
                                        ZonedDateTime.now().minusHours(3).format(DateTimeFormatter.ISO_DATE_TIME),
                                        1)));

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.value()).isEqualTo(0);
        assertThat(result.componentsResults().stream().flatMap(scr -> scr.reasons().stream()))
                .contains("There is more than 4 years between the last release and the last commit.");
    }

    @Test
    void shouldNotHaveProblemWithMoreRecentReleaseThanCommit() throws Exception {
        final ZonedDateTime commitDateTime = ZonedDateTime.now().minusYears(2);
        final ZonedDateTime releaseDateTime = ZonedDateTime.now().minusMinutes(10);

        final AdoptionScoring scoring = getSpy();
        final Plugin plugin = mock(Plugin.class);

        when(plugin.getReleaseTimestamp()).thenReturn(releaseDateTime);
        when(plugin.getDetails())
                .thenReturn(Map.of(
                        UpForAdoptionProbe.KEY,
                                ProbeResult.success(UpForAdoptionProbe.KEY, "This plugin is not up for adoption.", 1),
                        LastCommitDateProbe.KEY,
                                ProbeResult.success(
                                        LastCommitDateProbe.KEY,
                                        commitDateTime.format(DateTimeFormatter.ISO_DATE_TIME),
                                        1)));

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.value()).isEqualTo(100);
        assertThat(result.componentsResults().stream().flatMap(scr -> scr.reasons().stream()))
                .contains("The latest release is more recent than the latest commit on the plugin.");
    }
}
