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

package io.jenkins.pluginhealth.scoring.model;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScoreTest {
    @Test
    void shouldBeAbleToAdjustScoreValueWithNewDetails() {
        final Plugin plugin = mock(Plugin.class);
        final Score score = new Score(plugin, ZonedDateTime.now());

        assertThat(score.getValue()).isEqualTo(0);

        score.addDetail(new ScoreResult("foo", 1, .4f, List.of()));
        assertThat(score.getDetails().size()).isEqualTo(1);
        assertThat(score.getValue()).isEqualTo(100);

        score.addDetail(new ScoreResult("bar", 0, .2f, List.of()));
        assertThat(score.getDetails().size()).isEqualTo(2);
        assertThat(score.getValue()).isEqualTo(67);

        score.addDetail(new ScoreResult("wiz", 1, .3f, List.of()));
        assertThat(score.getDetails().size()).isEqualTo(3);
        assertThat(score.getValue()).isEqualTo(78);
    }
}
