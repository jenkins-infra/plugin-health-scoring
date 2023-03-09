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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;
import io.jenkins.pluginhealth.scoring.model.Score;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
import io.jenkins.pluginhealth.scoring.service.PluginService;
import io.jenkins.pluginhealth.scoring.service.ScoreService;
import io.jenkins.pluginhealth.scoring.service.ScoringService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScoringEngineTest {
    @Mock private PluginService pluginService;
    @Mock private ScoringService scoringService;
    @Mock private ScoreService scoreService;

    @Test
    void shouldBeAbleToScoreOnePlugin() {
        final Plugin plugin = mock(Plugin.class);
        final Scoring scoringA = mock(Scoring.class);
        final Scoring scoringB = mock(Scoring.class);

        when(plugin.getName()).thenReturn("foo-bar");
        when(scoringA.doApply(plugin)).thenReturn(new ScoreResult("scoring-a", 1, .5f));
        when(scoringB.doApply(plugin)).thenReturn(new ScoreResult("scoring-b", 0, 1));

        when(scoringService.getScoringList()).thenReturn(List.of(scoringA, scoringB));
        when(scoreService.save(any(Score.class))).then(AdditionalAnswers.returnsFirstArg());

        final ScoringEngine ScoringEngine = new ScoringEngine(scoringService, pluginService, scoreService);
        final Score score = ScoringEngine.runOn(plugin);

        verify(scoringA).apply(plugin);
        verify(scoringB).apply(plugin);

        assertThat(score.getPlugin()).isEqualTo(plugin);
        assertThat(score.getDetails()).hasSize(2);
        assertThat(score.getValue()).isEqualTo(33);
    }

    @Test
    void shouldBeAbleToScoreMultiplePlugins() {
        final Plugin pluginA = mock(Plugin.class);
        final Plugin pluginB = mock(Plugin.class);
        final Plugin pluginC = mock(Plugin.class);

        final Scoring scoringA = mock(Scoring.class);
        final Scoring scoringB = mock(Scoring.class);

        when(pluginA.getName()).thenReturn("plugin-a");
        when(pluginB.getName()).thenReturn("plugin-b");
        when(pluginC.getName()).thenReturn("plugin-c");

        when(scoringA.doApply(any(Plugin.class))).thenReturn(new ScoreResult("scoring-a", 1, 0.5f));
        when(scoringB.doApply(any(Plugin.class))).thenReturn(new ScoreResult("scoring-b", .75f, .75f));

        when(scoringService.getScoringList()).thenReturn(List.of(scoringA, scoringB));
        when(pluginService.streamAll()).thenReturn(Stream.of(pluginA, pluginB, pluginC));

        final ScoringEngine ScoringEngine = new ScoringEngine(scoringService, pluginService, scoreService);
        ScoringEngine.run();

        final ArgumentCaptor<Plugin> pluginArgumentScoringA = ArgumentCaptor.forClass(Plugin.class);
        verify(scoringA, times(3)).apply(pluginArgumentScoringA.capture());
        assertThat(pluginArgumentScoringA.getAllValues()).containsExactlyInAnyOrder(pluginA, pluginB, pluginC);

        final ArgumentCaptor<Plugin> pluginArgumentScoringB = ArgumentCaptor.forClass(Plugin.class);
        verify(scoringB, times(3)).apply(pluginArgumentScoringB.capture());
        assertThat(pluginArgumentScoringB.getAllValues()).containsExactlyInAnyOrder(pluginA, pluginB, pluginC);

        final ArgumentCaptor<Score> scoreArgument = ArgumentCaptor.forClass(Score.class);
        verify(scoreService, times(3)).save(scoreArgument.capture());
        assertThat(scoreArgument.getAllValues())
            .filteredOn(score -> Objects.nonNull(score) && score.getDetails().size() == 2 && score.getValue() == 85)
            .hasSize(3);
    }

    @Test
    void shouldOnlyScorePluginsWithNewerResultThanLatestScore() {
        final Plugin pluginA = mock(Plugin.class);
        final String pluginName = "plugin-a";
        when(pluginA.getName()).thenReturn(pluginName);
        when(pluginA.getDetails()).thenReturn(Map.of(
            "foo-bar", new ProbeResult("foo-bar", "", ResultStatus.FAILURE, ZonedDateTime.now().minusMinutes(15))
        ));

        final Scoring scoringA = mock(Scoring.class);

        final Score oldPluginAScore = mock(Score.class);
        when(oldPluginAScore.getComputedAt()).thenReturn(ZonedDateTime.now().minusMinutes(5));

        when(scoreService.latestScoreFor(pluginName)).thenReturn(Optional.of(oldPluginAScore));

        final ScoringEngine ScoringEngine = new ScoringEngine(scoringService, pluginService, scoreService);
        final Score score = ScoringEngine.runOn(pluginA);

        verify(scoringA, times(0)).apply(any(Plugin.class));

        verify(scoreService, never()).save(any(Score.class));
        assertThat(score).isEqualTo(oldPluginAScore);
    }

    @Test
    void shouldNotScorePluginsWithLatestScoreAndEmptyDetailsMap() {
        final Plugin pluginA = mock(Plugin.class);
        final String pluginName = "plugin-a";
        when(pluginA.getName()).thenReturn(pluginName);
        when(pluginA.getDetails()).thenReturn(Map.of());

        final Scoring scoringA = mock(Scoring.class);
        final Score oldPluginAScore = mock(Score.class);
        when(oldPluginAScore.getComputedAt()).thenReturn(ZonedDateTime.now().minusMinutes(5));
        when(scoreService.latestScoreFor(pluginName)).thenReturn(Optional.of(oldPluginAScore));

        final ScoringEngine ScoringEngine = new ScoringEngine(scoringService, pluginService, scoreService);
        final Score score = ScoringEngine.runOn(pluginA);

        verify(scoringA, times(0)).apply(any(Plugin.class));
        verify(scoreService, never()).save(any(Score.class));
        assertThat(score).isEqualTo(oldPluginAScore);
    }
}
