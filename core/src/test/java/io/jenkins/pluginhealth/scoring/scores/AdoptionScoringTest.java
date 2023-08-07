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

        when(plugin.getDetails()).thenReturn(Map.of(
            UpForAdoptionProbe.KEY, ProbeResult.success(UpForAdoptionProbe.KEY, "This plugin is up for adoption.")
        ));

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.key()).isEqualTo("adoption");
        assertThat(result.weight()).isEqualTo(.8f);
        assertThat(result.value()).isEqualTo(0);
    }

    @Test
    void shouldScoreZeroForPluginsUpForAdoptionEvenWithRecentCommit() {
        final AdoptionScoring scoring = getSpy();
        final Plugin plugin = mock(Plugin.class);

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusHours(4));
        when(plugin.getDetails()).thenReturn(Map.of(
            UpForAdoptionProbe.KEY, ProbeResult.success(UpForAdoptionProbe.KEY, "This plugin is up for adoption."),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, ZonedDateTime.now().minusHours(3).format(DateTimeFormatter.ISO_DATE_TIME))
        ));

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.key()).isEqualTo("adoption");
        assertThat(result.weight()).isEqualTo(.8f);
        assertThat(result.value()).isEqualTo(0);
    }

    @Test
    void shouldScoreZeroForPluginsWithNoLastCommit() {
        final AdoptionScoring scoring = getSpy();
        final Plugin plugin = mock(Plugin.class);

        when(plugin.getDetails()).thenReturn(
            Map.of(UpForAdoptionProbe.KEY, ProbeResult.success(UpForAdoptionProbe.KEY, "This plugin is not up for adoption."))
        );

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.value()).isEqualTo(0);
    }

    @Test
    void shouldScoreOneForPluginsWithCommitsLessThanSixMonthsOld() {
        final AdoptionScoring scoring = getSpy();
        final Plugin plugin = mock(Plugin.class);

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusMonths(2));
        when(plugin.getDetails()).thenReturn(
            Map.of(
                UpForAdoptionProbe.KEY, ProbeResult.success(UpForAdoptionProbe.KEY, "This plugin is not up for adoption."),
                LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, ZonedDateTime.now().minusHours(3).format(DateTimeFormatter.ISO_DATE_TIME))
            )
        );

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.value()).isEqualTo(100);
    }

    @Test
    void shouldScoreSeventyFiveForPluginsWithCommitsLessThanOneYearOld() {
        final AdoptionScoring scoring = getSpy();
        final Plugin plugin = mock(Plugin.class);

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusMonths(8));
        when(plugin.getDetails()).thenReturn(
            Map.of(
                UpForAdoptionProbe.KEY, ProbeResult.success(UpForAdoptionProbe.KEY, "This plugin is not up for adoption."),
                LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, ZonedDateTime.now().minusHours(3).format(DateTimeFormatter.ISO_DATE_TIME))
            )
        );

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.value()).isEqualTo(75);
    }

    @Test
    void shouldScoreFiftyForPluginsWithCommitsLessThanTwoYearsOld() {
        final AdoptionScoring scoring = getSpy();
        final Plugin plugin = mock(Plugin.class);

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusMonths(18));
        when(plugin.getDetails()).thenReturn(
            Map.of(
                UpForAdoptionProbe.KEY, ProbeResult.success(UpForAdoptionProbe.KEY, "This plugin is not up for adoption."),
                LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, ZonedDateTime.now().minusHours(3).format(DateTimeFormatter.ISO_DATE_TIME))
            )
        );

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.value()).isEqualTo(50);
    }

    @Test
    void shouldScoreTwentyFiveForPluginsWithCommitsLessThanFourYearsOld() {
        final AdoptionScoring scoring = getSpy();
        final Plugin plugin = mock(Plugin.class);

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusYears(3));
        when(plugin.getDetails()).thenReturn(
            Map.of(
                UpForAdoptionProbe.KEY, ProbeResult.success(UpForAdoptionProbe.KEY, "This plugin is not up for adoption."),
                LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, ZonedDateTime.now().minusHours(3).format(DateTimeFormatter.ISO_DATE_TIME))
            )
        );

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.value()).isEqualTo(25);
    }

    @Test
    void shouldScoreZeroForPluginsWithCommitsMoreThanFourYearsOld() {
        final AdoptionScoring scoring = getSpy();
        final Plugin plugin = mock(Plugin.class);

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusYears(5));
        when(plugin.getDetails()).thenReturn(
            Map.of(
                UpForAdoptionProbe.KEY, ProbeResult.success(UpForAdoptionProbe.KEY, "This plugin is not up for adoption."),
                LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, ZonedDateTime.now().minusHours(3).format(DateTimeFormatter.ISO_DATE_TIME))
            )
        );

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.value()).isEqualTo(0);
    }
}
