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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
import io.jenkins.pluginhealth.scoring.probes.LastCommitDateProbe;
import io.jenkins.pluginhealth.scoring.probes.UpForAdoptionProbe;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AdoptionScoringTest {
    @Test
    public void shouldScoreZeroForPluginsUpForAdoption() {
        final AdoptionScoring scoring = spy(AdoptionScoring.class);
        final Plugin plugin = mock(Plugin.class);

        when(plugin.getDetails()).thenReturn(
            Map.of(UpForAdoptionProbe.KEY, new ProbeResult(UpForAdoptionProbe.KEY, "", ResultStatus.FAILURE))
        );

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.key()).isEqualTo("adoption");
        assertThat(result.coefficient()).isEqualTo(.5f);
        assertThat(result.value()).isEqualTo(0);
    }

    @Test
    public void shouldScoreZeroForPluginsWithNoLastCommit() {
        final AdoptionScoring scoring = spy(AdoptionScoring.class);
        final Plugin plugin = mock(Plugin.class);

        when(plugin.getDetails()).thenReturn(
            Map.of(UpForAdoptionProbe.KEY, new ProbeResult(UpForAdoptionProbe.KEY, "", ResultStatus.SUCCESS))
        );

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.value()).isEqualTo(0);
    }

    @Test
    public void shouldScoreOneForPluginsWithCommitsLessThanSixMonthsOld() {
        final AdoptionScoring scoring = spy(AdoptionScoring.class);
        final Plugin plugin = mock(Plugin.class);

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusMonths(2));
        when(plugin.getDetails()).thenReturn(
            Map.of(
                UpForAdoptionProbe.KEY, new ProbeResult(UpForAdoptionProbe.KEY, "", ResultStatus.SUCCESS),
                LastCommitDateProbe.KEY, new ProbeResult(LastCommitDateProbe.KEY, ZonedDateTime.now().minusHours(3).toString(), ResultStatus.SUCCESS)
            )
        );

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.value()).isEqualTo(1f);
    }

    @Test
    public void shouldScoreSeventyFiveForPluginsWithCommitsLessThanOneYearOld() {
        final AdoptionScoring scoring = spy(AdoptionScoring.class);
        final Plugin plugin = mock(Plugin.class);

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusMonths(8));
        when(plugin.getDetails()).thenReturn(
            Map.of(
                UpForAdoptionProbe.KEY, new ProbeResult(UpForAdoptionProbe.KEY, "", ResultStatus.SUCCESS),
                LastCommitDateProbe.KEY, new ProbeResult(LastCommitDateProbe.KEY, ZonedDateTime.now().minusHours(3).toString(), ResultStatus.SUCCESS)
            )
        );

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.value()).isEqualTo(.75f);
    }

    @Test
    public void shouldScoreFiftyForPluginsWithCommitsLessThanTwoYearsOld() {
        final AdoptionScoring scoring = spy(AdoptionScoring.class);
        final Plugin plugin = mock(Plugin.class);

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusMonths(18));
        when(plugin.getDetails()).thenReturn(
            Map.of(
                UpForAdoptionProbe.KEY, new ProbeResult(UpForAdoptionProbe.KEY, "", ResultStatus.SUCCESS),
                LastCommitDateProbe.KEY, new ProbeResult(LastCommitDateProbe.KEY, ZonedDateTime.now().minusHours(3).toString(), ResultStatus.SUCCESS)
            )
        );

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.value()).isEqualTo(.5f);
    }

    @Test
    public void shouldScoreTwentyFiveForPluginsWithCommitsLessThanFourYearsOld() {
        final AdoptionScoring scoring = spy(AdoptionScoring.class);
        final Plugin plugin = mock(Plugin.class);

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusYears(3));
        when(plugin.getDetails()).thenReturn(
            Map.of(
                UpForAdoptionProbe.KEY, new ProbeResult(UpForAdoptionProbe.KEY, "", ResultStatus.SUCCESS),
                LastCommitDateProbe.KEY, new ProbeResult(LastCommitDateProbe.KEY, ZonedDateTime.now().minusHours(3).toString(), ResultStatus.SUCCESS)
            )
        );

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.value()).isEqualTo(.25f);
    }

    @Test
    public void shouldScoreZeroForPluginsWithCommitsMoreThanFourYearsOld() {
        final AdoptionScoring scoring = spy(AdoptionScoring.class);
        final Plugin plugin = mock(Plugin.class);

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusYears(4));
        when(plugin.getDetails()).thenReturn(
            Map.of(
                UpForAdoptionProbe.KEY, new ProbeResult(UpForAdoptionProbe.KEY, "", ResultStatus.SUCCESS),
                LastCommitDateProbe.KEY, new ProbeResult(LastCommitDateProbe.KEY, ZonedDateTime.now().minusHours(3).toString(), ResultStatus.SUCCESS)
            )
        );

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result.value()).isEqualTo(0f);
    }
}
