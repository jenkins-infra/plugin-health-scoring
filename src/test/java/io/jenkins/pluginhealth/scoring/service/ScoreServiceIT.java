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

package io.jenkins.pluginhealth.scoring.service;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;
import java.util.List;

import io.jenkins.pluginhealth.scoring.AbstractDBContainerTest;
import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.Score;
import io.jenkins.pluginhealth.scoring.model.ScoreDTO;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
import io.jenkins.pluginhealth.scoring.repository.ScoreRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class ScoreServiceIT extends AbstractDBContainerTest {
    @Autowired private TestEntityManager entityManager;
    @Autowired private ScoreRepository scoreRepository;

    @Test
    public void shouldBeEmpty() {
        assertThat(scoreRepository.count()).isZero();
    }

    @Test
    public void shouldBeAbleToSaveScoreForPlugin() {
        final Plugin p1 = entityManager.persist(
            new Plugin("plugin-1", null, null, ZonedDateTime.now().minusMinutes(5))
        );

        final Score score = new Score(p1, ZonedDateTime.now());
        final ScoreResult result = new ScoreResult("foo", 1, 1);
        score.addDetail(result);

        final Score saved = scoreRepository.save(score);
        assertThat(saved)
            .extracting(Score::getPlugin, Score::getValue)
            .contains(p1, 100L);
        assertThat(saved.getDetails())
            .hasSize(1)
            .contains(result);
    }

    @Test
    public void shouldBeAbleToExtractScoreSummary() {
        final Plugin p1 = entityManager.persist(
            new Plugin("plugin-1", null, null, ZonedDateTime.now().minusMinutes(5))
        );
        final Plugin p2 = entityManager.persist(
            new Plugin("plugin-2", null, "scm", ZonedDateTime.now().minusMinutes(10))
        );

        final Score p1s = new Score(p1, ZonedDateTime.now());
        p1s.addDetail(new ScoreResult("foo", 1, 1));
        p1s.addDetail(new ScoreResult("bar", 0, .5f));

        final Score p2s = new Score(p2, ZonedDateTime.now());
        p2s.addDetail(new ScoreResult("foo", 0, 1));

        scoreRepository.saveAll(List.of(p1s, p2s));
        assertThat(scoreRepository.count()).isEqualTo(2);

        final List<ScoreDTO> summary = scoreRepository.getLatestScoreForPlugins();
        assertThat(summary).hasSize(2)
            .extracting(ScoreDTO::getName, ScoreDTO::getValue)
            .contains(
                tuple("plugin-1", 67L),
                tuple("plugin-2", 0L)
            );
    }
}
