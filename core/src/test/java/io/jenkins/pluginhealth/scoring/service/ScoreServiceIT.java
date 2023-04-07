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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.AbstractDBContainerTest;
import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.Score;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
import io.jenkins.pluginhealth.scoring.repository.ScoreRepository;

import hudson.util.VersionNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class ScoreServiceIT extends AbstractDBContainerTest {
    @Autowired private TestEntityManager entityManager;
    @Autowired private ScoreRepository scoreRepository;
    @MockBean private PluginService pluginService;

    private ScoreService scoreService;

    @BeforeEach
    void setup() {
        scoreService = new ScoreService(scoreRepository, pluginService);
    }

    @Test
    void shouldBeEmpty() {
        assertThat(scoreRepository.count()).isZero();
    }

    @Test
    void shouldBeAbleToSaveScoreForPlugin() {
        final Plugin p1 = entityManager.persist(
            new Plugin("plugin-1", new VersionNumber("1.0"), null, ZonedDateTime.now().minusMinutes(5))
        );

        final Score score = new Score(p1, ZonedDateTime.now());
        final ScoreResult result = new ScoreResult("foo", 1, 1);
        score.addDetail(result);

        final Score saved = scoreService.save(score);
        assertThat(saved)
            .extracting(Score::getPlugin, Score::getValue)
            .contains(p1, 100L);
        assertThat(saved.getDetails())
            .hasSize(1)
            .contains(result);
    }

    @Test
    void shouldBeAbleToExtractScoreSummary() {
        final Plugin p1 = entityManager.persist(
            new Plugin("plugin-1", new VersionNumber("1.0"), null, ZonedDateTime.now().minusMinutes(5))
        );
        final Plugin p2 = entityManager.persist(
            new Plugin("plugin-2", new VersionNumber("2.0"), "scm", ZonedDateTime.now().minusMinutes(10))
        );

        final Score p1s = new Score(p1, ZonedDateTime.now());
        p1s.addDetail(new ScoreResult("foo", 1, 1));
        p1s.addDetail(new ScoreResult("bar", 0, .5f));

        final Score p2s = new Score(p2, ZonedDateTime.now());
        p2s.addDetail(new ScoreResult("foo", 0, 1));

        List.of(p1s, p2s).forEach(scoreService::save);
        assertThat(scoreRepository.count()).isEqualTo(2);

        final Map<String, ScoreService.ScoreSummary> summary = scoreService.getLatestScoresSummaryMap();

        assertThat(summary)
            .extractingFromEntries(
                Map.Entry::getKey,
                Map.Entry::getValue
            )
            .containsExactlyInAnyOrder(
                tuple(
                    p1.getName(),
                    new ScoreService.ScoreSummary(p1s.getValue(), p1.getVersion().toString(), p1s.getDetails(), p1s.getComputedAt())
                ),
                tuple(
                    p2.getName(),
                    new ScoreService.ScoreSummary(p2s.getValue(), p2.getVersion().toString(), p2s.getDetails(), p2s.getComputedAt())
                )
            );
    }

    @Test
    void shouldOnlyRetrieveLatestScoreForPlugins() {
        final Plugin p1 = entityManager.persist(
            new Plugin("plugin-1", new VersionNumber("1.0"), null, ZonedDateTime.now().minusMinutes(5))
        );
        final Plugin p2 = entityManager.persist(
            new Plugin("plugin-2", new VersionNumber("2.0"), "scm", ZonedDateTime.now().minusMinutes(10))
        );

        final Score p1s = new Score(p1, ZonedDateTime.now());
        p1s.addDetail(new ScoreResult("foo", 1, 1));
        p1s.addDetail(new ScoreResult("bar", 0, .5f));

        final Score p2s = new Score(p2, ZonedDateTime.now());
        p2s.addDetail(new ScoreResult("foo", 0, 1));

        final Score p1sOld = new Score(p1, ZonedDateTime.now().minusMinutes(10));
        p1sOld.addDetail(new ScoreResult("foo", 1, 1));
        p1sOld.addDetail(new ScoreResult("bar", 0, .5f));

        final Score p1sOld2 = new Score(p1, ZonedDateTime.now().minusMinutes(15));
        p1sOld2.addDetail(new ScoreResult("foo", 1, 1));
        p1sOld2.addDetail(new ScoreResult("bar", 0, .5f));

        final Score p2sOld = new Score(p2, ZonedDateTime.now().minusMinutes(10));
        p2sOld.addDetail(new ScoreResult("foo", 0, 1));

        List.of(p1s, p2s, p1sOld, p2sOld, p1sOld2).forEach(scoreService::save);
        assertThat(scoreRepository.count()).isEqualTo(5);

        final Map<String, ScoreService.ScoreSummary> summary = scoreService.getLatestScoresSummaryMap();

        assertThat(summary)
            .extractingFromEntries(
                Map.Entry::getKey,
                Map.Entry::getValue
            )
            .containsExactlyInAnyOrder(
                tuple(
                    p1.getName(),
                    new ScoreService.ScoreSummary(p1s.getValue(), p1.getVersion().toString(), p1s.getDetails(), p1s.getComputedAt())
                ),
                tuple(
                    p2.getName(),
                    new ScoreService.ScoreSummary(p2s.getValue(), p2.getVersion().toString(), p2s.getDetails(), p2s.getComputedAt())
                )
            );
    }

    @Test
    void shouldBeAbeToRetrieveScoreStatisticsAndIgnoreOldScores() {
        final String s1Key = "foo";

        final Plugin p1 = entityManager.persist(new Plugin(
            "plugin-1", new VersionNumber("1.0"), null, ZonedDateTime.now().minusMinutes(5)
        ));
        final Plugin p2 = entityManager.persist(new Plugin(
            "plugin-2", new VersionNumber("1.3"), null, ZonedDateTime.now().minusMinutes(3)
        ));
        final Plugin p3 = entityManager.persist(new Plugin(
            "plugin-3", new VersionNumber("1.3"), null, ZonedDateTime.now().minusMinutes(2)
        ));
        final Plugin p4 = entityManager.persist(new Plugin(
            "plugin-4", new VersionNumber("1.3"), null, ZonedDateTime.now().minusMinutes(8)
        ));
        final Plugin p5 = entityManager.persist(new Plugin(
            "plugin-5", new VersionNumber("1.3"), null, ZonedDateTime.now().minusMinutes(13)
        ));
        final Plugin p6 = entityManager.persist(new Plugin(
            "plugin-6", new VersionNumber("1.3"), null, ZonedDateTime.now().minusMinutes(34)
        ));
        final Plugin p7 = entityManager.persist(new Plugin(
            "plugin-7", new VersionNumber("1.3"), null, ZonedDateTime.now().minusMinutes(21)
        ));

        final Score p1s = new Score(p1, ZonedDateTime.now());
        p1s.addDetail(new ScoreResult(s1Key, .5f, .5f));

        final Score p1sOld = new Score(p1, ZonedDateTime.now().minusMinutes(10));
        p1sOld.addDetail(new ScoreResult(s1Key, 1, 1));

        final Score p2s = new Score(p2, ZonedDateTime.now());
        p2s.addDetail(new ScoreResult(s1Key, 0, 1));

        final Score p2sOld = new Score(p2, ZonedDateTime.now().minusMinutes(5));
        p2sOld.addDetail(new ScoreResult(s1Key, .9f, 1));

        final Score p3s = new Score(p3, ZonedDateTime.now());
        p3s.addDetail(new ScoreResult(s1Key, 1, 1));

        final Score p4s = new Score(p4, ZonedDateTime.now());
        p4s.addDetail(new ScoreResult(s1Key, .75f, 1));

        final Score p5s = new Score(p5, ZonedDateTime.now());
        p5s.addDetail(new ScoreResult(s1Key, .8f, 1));

        final Score p6s = new Score(p6, ZonedDateTime.now());
        p6s.addDetail(new ScoreResult(s1Key, .42f, 1));

        final Score p7s = new Score(p7, ZonedDateTime.now());
        p7s.addDetail(new ScoreResult(s1Key, 0, 1));

        List.of(p1s, p1sOld, p2s, p2sOld, p3s, p4s, p5s, p6s, p7s).forEach(scoreService::save);
        assertThat(scoreRepository.count()).isEqualTo(9);

        final ScoreService.ScoreStatistics scoresStatistics = scoreService.getScoresStatistics();

        assertThat(scoresStatistics)
            .isEqualTo(new ScoreService.ScoreStatistics(
                50, 0, 100, 0, 50, 80
            ));
    }
}
