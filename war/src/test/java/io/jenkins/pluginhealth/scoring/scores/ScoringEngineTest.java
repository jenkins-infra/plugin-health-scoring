/*
 * MIT License
 *
 * Copyright (c) 2022-2024 Jenkins Infra
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
import java.util.Set;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
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
    @Mock
    private PluginService pluginService;

    @Mock
    private ScoringService scoringService;

    @Mock
    private ScoreService scoreService;

    @Test
    void shouldBeAbleToScoreOnePlugin() {
        final Plugin plugin = mock(Plugin.class);
        final Scoring scoringA = mock(Scoring.class);
        final Scoring scoringB = mock(Scoring.class);

        when(plugin.getName()).thenReturn("foo-bar");
        when(scoringA.apply(plugin)).thenReturn(new ScoreResult("scoring-a", 100, .5f, Set.of(), 1));
        when(scoringB.apply(plugin)).thenReturn(new ScoreResult("scoring-b", 0, 1, Set.of(), 1));

        when(scoringService.getScoringList()).thenReturn(List.of(scoringA, scoringB));
        when(scoreService.save(any(Score.class))).then(AdditionalAnswers.returnsFirstArg());

        final ScoringEngine scoringEngine = new ScoringEngine(scoringService, pluginService, scoreService);
        final Score score = scoringEngine.runOn(plugin);

        verify(scoringA).apply(plugin);
        verify(scoringB).apply(plugin);

        assertThat(score).isNotNull();
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

        when(scoringA.apply(any(Plugin.class))).thenReturn(new ScoreResult("scoring-a", 100, 0.5f, Set.of(), 1));
        when(scoringB.apply(any(Plugin.class))).thenReturn(new ScoreResult("scoring-b", 75, .75f, Set.of(), 1));

        when(scoringService.getScoringList()).thenReturn(List.of(scoringA, scoringB));
        when(pluginService.streamAll()).thenReturn(Stream.of(pluginA, pluginB, pluginC));

        final ScoringEngine scoringEngine = new ScoringEngine(scoringService, pluginService, scoreService);
        scoringEngine.run();

        final ArgumentCaptor<Plugin> pluginArgumentScoringA = ArgumentCaptor.forClass(Plugin.class);
        verify(scoringA, times(3)).apply(pluginArgumentScoringA.capture());
        assertThat(pluginArgumentScoringA.getAllValues()).containsExactlyInAnyOrder(pluginA, pluginB, pluginC);

        final ArgumentCaptor<Plugin> pluginArgumentScoringB = ArgumentCaptor.forClass(Plugin.class);
        verify(scoringB, times(3)).apply(pluginArgumentScoringB.capture());
        assertThat(pluginArgumentScoringB.getAllValues()).containsExactlyInAnyOrder(pluginA, pluginB, pluginC);

        final ArgumentCaptor<Score> scoreArgument = ArgumentCaptor.forClass(Score.class);
        verify(scoreService, times(3)).save(scoreArgument.capture());
        assertThat(scoreArgument.getAllValues())
                .filteredOn(
                        score -> Objects.nonNull(score) && score.getDetails().size() == 2 && score.getValue() == 85)
                .hasSize(3);
    }

    @Test
    void shouldOnlyScorePluginsWithNewerResultThanLatestScore() {
        final Plugin pluginA = mock(Plugin.class);
        final String pluginName = "plugin-a";
        when(pluginA.getName()).thenReturn(pluginName);
        when(pluginA.getDetails())
                .thenReturn(Map.of(
                        "foo-bar",
                        new ProbeResult(
                                "foo-bar",
                                "",
                                ProbeResult.Status.SUCCESS,
                                ZonedDateTime.now().minusMinutes(15),
                                1)));

        final String scoringAKey = "scoring-A";
        final Scoring scoringA = mock(Scoring.class);
        when(scoringA.version()).thenReturn(1);
        when(scoringA.key()).thenReturn(scoringAKey);

        final Score oldPluginAScore = mock(Score.class);
        when(oldPluginAScore.getComputedAt()).thenReturn(ZonedDateTime.now().minusMinutes(5));
        when(oldPluginAScore.getDetails()).thenReturn(Set.of(new ScoreResult(scoringAKey, 100, 1, Set.of(), 1)));

        when(scoringService.getScoringList()).thenReturn(List.of(scoringA));
        when(scoreService.latestScoreFor(pluginName)).thenReturn(Optional.of(oldPluginAScore));

        final ScoringEngine scoringEngine = new ScoringEngine(scoringService, pluginService, scoreService);
        final Score score = scoringEngine.runOn(pluginA);

        verify(scoringA, times(0)).apply(any(Plugin.class));

        verify(scoreService, never()).save(any(Score.class));
        assertThat(score).isEqualTo(oldPluginAScore);
    }

    @Test
    void shouldReRunScoringWhenVersionChanged() {
        final Plugin plugin = mock(Plugin.class);
        final Scoring scoringA = mock(Scoring.class);
        final Score previousScore = mock(Score.class);

        when(plugin.getName()).thenReturn("foo-bar");
        when(plugin.getDetails())
                .thenReturn(Map.of(
                        "foo-bar",
                        new ProbeResult(
                                "foo-bar",
                                "",
                                ProbeResult.Status.SUCCESS,
                                ZonedDateTime.now().minusDays(1),
                                1)));

        when(scoringA.key()).thenReturn("scoring-a");
        when(scoringA.version()).thenReturn(2);
        final ScoreResult expectedNewScoreResult = new ScoreResult("scoring-a", 100, .5f, Set.of(), 2);
        when(scoringA.apply(plugin)).thenReturn(expectedNewScoreResult);

        when(previousScore.getDetails()).thenReturn(Set.of(new ScoreResult("scoring-a", 1, 1, Set.of(), 1)));
        when(previousScore.getComputedAt()).thenReturn(ZonedDateTime.now().minusHours(1));

        when(scoringService.getScoringList()).thenReturn(List.of(scoringA));
        when(scoreService.latestScoreFor("foo-bar")).thenReturn(Optional.of(previousScore));
        when(scoreService.save(any(Score.class))).then(AdditionalAnswers.returnsFirstArg());

        final ScoringEngine scoringEngine = new ScoringEngine(scoringService, pluginService, scoreService);
        final Score score = scoringEngine.runOn(plugin);

        verify(scoringA).apply(plugin);

        assertThat(score).isNotNull();
        assertThat(score.getPlugin()).isEqualTo(plugin);
        assertThat(score.getDetails()).hasSize(1).contains(expectedNewScoreResult);
        assertThat(score.getValue()).isEqualTo(100);
    }

    @Test
    void shouldComputeScoreWhenNewScoringAvailable() {
        final ZonedDateTime now = ZonedDateTime.now();

        final Plugin plugin = mock(Plugin.class);
        final Scoring scoringA = mock(Scoring.class);
        final Scoring scoringB = mock(Scoring.class);

        String pluginName = "plugin";
        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getDetails())
                .thenReturn(Map.of(
                        "probe-a",
                        new ProbeResult("probe-a", "", ProbeResult.Status.SUCCESS, now.minusMinutes(10), 1)));

        when(scoringA.version()).thenReturn(1);
        when(scoringA.key()).thenReturn("scoring-a");
        when(scoringB.key()).thenReturn("scoring-b");

        Score previousScore = mock(Score.class);
        when(previousScore.getDetails()).thenReturn(Set.of(new ScoreResult("scoring-a", 1, 1, Set.of(), 1)));
        when(previousScore.getComputedAt()).thenReturn(now.minusMinutes(5));
        when(scoreService.latestScoreFor(pluginName)).thenReturn(Optional.of(previousScore));

        when(scoringService.getScoringList()).thenReturn(List.of(scoringA, scoringB));

        final ScoringEngine scoringEngine = new ScoringEngine(scoringService, pluginService, scoreService);
        scoringEngine.runOn(plugin);

        verify(scoringA).apply(plugin);
        verify(scoringB).apply(plugin);
    }

    @Test
    void shouldComputeScoreWhenNewScoringVersion() {
        final ZonedDateTime now = ZonedDateTime.now();

        final Plugin plugin = mock(Plugin.class);
        final Scoring scoringA = mock(Scoring.class);
        final Scoring scoringB = mock(Scoring.class);

        String pluginName = "plugin";
        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getDetails())
                .thenReturn(Map.of(
                        "probe-a",
                        new ProbeResult("probe-a", "", ProbeResult.Status.SUCCESS, now.minusMinutes(10), 1)));

        when(scoringA.version()).thenReturn(1);
        when(scoringA.key()).thenReturn("scoring-a");
        when(scoringB.version()).thenReturn(2);
        when(scoringB.key()).thenReturn("scoring-b");

        Score previousScore = mock(Score.class);
        when(previousScore.getDetails())
                .thenReturn(Set.of(
                        new ScoreResult("scoring-a", 1, 1, Set.of(), 1),
                        new ScoreResult("scoring-b", 1, 1, Set.of(), 1)));
        when(previousScore.getComputedAt()).thenReturn(now.minusMinutes(5));
        when(scoreService.latestScoreFor(pluginName)).thenReturn(Optional.of(previousScore));

        when(scoringService.getScoringList()).thenReturn(List.of(scoringA, scoringB));

        final ScoringEngine scoringEngine = new ScoringEngine(scoringService, pluginService, scoreService);
        scoringEngine.runOn(plugin);

        verify(scoringA).apply(plugin);
        verify(scoringB).apply(plugin);
    }
}
