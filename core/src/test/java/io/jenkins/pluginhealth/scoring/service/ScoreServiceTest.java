/*
 * MIT License
 *
 * Copyright (c) 2023-2025 Jenkins Infra
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
package io.jenkins.pluginhealth.scoring.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.repository.ScoreRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ScoreServiceTest {
    @Mock
    private ScoreRepository scoreRepository;

    private ScoreService scoreService;

    @BeforeEach
    public void setup() {
        scoreService = new ScoreService(scoreRepository);
    }

    @Test
    void shouldBeAbleToComputeScoreStatisticCorrectly() {
        when(scoreRepository.getLatestScoreValueOfEveryPlugin()).thenReturn(new int[] {50, 0, 100, 75, 80, 42, 0});

        final Optional<ScoreService.ScoreStatistics> scoresStatistics = scoreService.getScoresStatistics();
        assertThat(scoresStatistics).contains(new ScoreService.ScoreStatistics(50, 0, 100, 0, 50, 80));
    }

    @Test
    void shouldBeAbleToProvideScoreDistribution() {
        final int[] scores = {50, 0, 100, 75, 80, 42, 0, 12, 43, 87, 98};
        final Map<Integer, Long> expectedDistribution = new HashMap<>(100);
        for (int index = 0; index < 100; index += 1) {
            expectedDistribution.put(index, 0L);
        }
        for (int score : scores) {
            expectedDistribution.merge(score, 1L, Long::sum);
        }

        when(scoreRepository.getLatestScoreValueOfEveryPlugin()).thenReturn(scores);

        final Map<Integer, Long> scoresDistribution = scoreService.getScoresDistribution();
        assertThat(scoresDistribution)
                .hasSize(101) // this to make sure the expectedDistribution filling is not totally incorrect
                .isEqualTo(expectedDistribution);
        assertThat(scoresDistribution.values().stream().reduce(0L, Long::sum)).isEqualTo(scores.length);
    }

    @Test
    void shouldBeAbleToSurviveEmptyScores() {
        when(scoreRepository.getLatestScoreValueOfEveryPlugin()).thenReturn(new int[] {});
        final Optional<ScoreService.ScoreStatistics> scoresStatistics = scoreService.getScoresStatistics();
        assertThat(scoresStatistics).isEmpty();
    }
}
