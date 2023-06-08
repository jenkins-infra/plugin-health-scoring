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
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

public abstract class AbstractScoringTest<T extends Scoring> {
    abstract T getSpy();

    @Test
    void shouldHaveValidName() {
        assertThat(getSpy().name()).isNotBlank();
    }

    @Test
    void shouldHaveDescription() {
        assertThat(getSpy().description()).isNotBlank();
    }

    @Test
    void shouldNotHaveNullNotEmptyRequirements() {
        assertThat(getSpy().getScoreComponents()).isNotEmpty();
    }

    @Test
    void shouldReturnNonNullScoreResult() {
        final Plugin plugin = mock(Plugin.class);
        final T scoring = getSpy();

        when(scoring.getScoreComponents()).thenReturn(
            Set.of(new Scoring.ScoreComponent(new Scoring.Key("foo"), 1f))
        );
        final ScoreResult score = scoring.apply(plugin);

        assertThat(score.key()).isEqualTo(scoring.key());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(score.key()).isEqualTo(scoring.key());
            softly.assertThat(score.coefficient()).isEqualTo(scoring.coefficient());
            softly.assertThat(score.value()).isEqualTo(0f);
        });
    }
}
