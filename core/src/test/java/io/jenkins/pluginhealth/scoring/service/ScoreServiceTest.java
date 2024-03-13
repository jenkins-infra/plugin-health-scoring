/*
 * MIT License
 *
 * Copyright (c) 2022-2023 Jenkins Infra
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

    @Mock
    private PluginService pluginService;

    private ScoreService scoreService;

    @BeforeEach
    public void setup() {
        scoreService = new ScoreService(scoreRepository, pluginService);
    }

    @Test
    void shouldBeAbleToComputeScoreStatisticCorrectly() {
        when(scoreRepository.getLatestScoreValueOfEveryPlugin()).thenReturn(new int[] {50, 0, 100, 75, 80, 42, 0});

        final ScoreService.ScoreStatistics scoresStatistics = scoreService.getScoresStatistics();
        assertThat(scoresStatistics).isEqualTo(new ScoreService.ScoreStatistics(50, 0, 100, 0, 50, 80));
    }
}
