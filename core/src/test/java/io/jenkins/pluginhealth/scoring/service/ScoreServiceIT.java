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
import static org.assertj.core.api.Assertions.tuple;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class ScoreServiceIT extends AbstractDBContainerTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ScoreRepository scoreRepository;

    private ScoreService scoreService;

    @BeforeEach
    void setup() {
        scoreService = new ScoreService(scoreRepository);
    }

    @Test
    void shouldBeAbleToSaveScoreForPlugin() {
        final Plugin p1 = entityManager.persist(new Plugin(
                "plugin-1", new VersionNumber("1.0"), null, ZonedDateTime.now().minusMinutes(5)));

        final Score score = new Score(p1, ZonedDateTime.now());
        final ScoreResult result = new ScoreResult("foo", 100, 1, Set.of(), 1);
        score.addDetail(result);

        final Score saved = scoreService.save(score);
        assertThat(saved).extracting(Score::getPlugin, Score::getValue).contains(p1, 100L);
        assertThat(saved.getDetails()).hasSize(1).contains(result);
    }

    @Test
    void shouldBeAbleToExtractScoreSummary() {
        final Plugin p1 = entityManager.persist(new Plugin(
                "plugin-1", new VersionNumber("1.0"), null, ZonedDateTime.now().minusMinutes(5)));
        final Plugin p2 = entityManager.persist(new Plugin(
                "plugin-2", new VersionNumber("2.0"), "scm", ZonedDateTime.now().minusMinutes(10)));

        final Score p1s = new Score(p1, ZonedDateTime.now());
        p1s.addDetail(new ScoreResult("foo", 100, 1, Set.of(), 1));
        p1s.addDetail(new ScoreResult("bar", 0, .5f, Set.of(), 1));

        final Score p2s = new Score(p2, ZonedDateTime.now());
        p2s.addDetail(new ScoreResult("foo", 0, 1, Set.of(), 1));

        Set.of(p1s, p2s).forEach(scoreService::save);
        assertThat(scoreRepository.count()).isEqualTo(2);

        final Map<String, Score> summary = scoreService.getLatestScoresSummaryMap();

        assertThat(summary)
                .extractingFromEntries(Map.Entry::getKey, Map.Entry::getValue)
                .containsExactlyInAnyOrder(tuple(p1.getName(), p1s), tuple(p2.getName(), p2s));
    }

    @Test
    void shouldOnlyRetrieveLatestScoreForPlugins() {
        final Plugin p1 = entityManager.persist(new Plugin(
                "plugin-1", new VersionNumber("1.0"), null, ZonedDateTime.now().minusMinutes(5)));
        final Plugin p2 = entityManager.persist(new Plugin(
                "plugin-2", new VersionNumber("2.0"), "scm", ZonedDateTime.now().minusMinutes(10)));

        final Score p1s = new Score(p1, ZonedDateTime.now());
        p1s.addDetail(new ScoreResult("foo", 1, 1, Set.of(), 1));
        p1s.addDetail(new ScoreResult("bar", 0, .5f, Set.of(), 1));

        final Score p2s = new Score(p2, ZonedDateTime.now());
        p2s.addDetail(new ScoreResult("foo", 0, 1, Set.of(), 1));

        final Score p1sOld = new Score(p1, ZonedDateTime.now().minusMinutes(10));
        p1sOld.addDetail(new ScoreResult("foo", 1, 1, Set.of(), 1));
        p1sOld.addDetail(new ScoreResult("bar", 0, .5f, Set.of(), 1));

        final Score p1sOld2 = new Score(p1, ZonedDateTime.now().minusMinutes(15));
        p1sOld2.addDetail(new ScoreResult("foo", 1, 1, Set.of(), 1));
        p1sOld2.addDetail(new ScoreResult("bar", 0, .5f, Set.of(), 1));

        final Score p2sOld = new Score(p2, ZonedDateTime.now().minusMinutes(10));
        p2sOld.addDetail(new ScoreResult("foo", 0, 1, Set.of(), 1));

        Set.of(p1s, p2s, p1sOld, p2sOld, p1sOld2).forEach(scoreService::save);
        assertThat(scoreRepository.count()).isEqualTo(5);

        final Map<String, Score> summary = scoreService.getLatestScoresSummaryMap();

        assertThat(summary)
                .extractingFromEntries(Map.Entry::getKey, Map.Entry::getValue)
                .containsExactlyInAnyOrder(tuple(p1.getName(), p1s), tuple(p2.getName(), p2s));
    }

    @Test
    void shouldBeAbeToRetrieveScoreStatisticsAndIgnoreOldScores() {
        final String s1Key = "foo";

        final Plugin p1 = entityManager.persist(new Plugin(
                "plugin-1", new VersionNumber("1.0"), null, ZonedDateTime.now().minusMinutes(5)));
        final Plugin p2 = entityManager.persist(new Plugin(
                "plugin-2", new VersionNumber("1.3"), null, ZonedDateTime.now().minusMinutes(3)));
        final Plugin p3 = entityManager.persist(new Plugin(
                "plugin-3", new VersionNumber("1.3"), null, ZonedDateTime.now().minusMinutes(2)));
        final Plugin p4 = entityManager.persist(new Plugin(
                "plugin-4", new VersionNumber("1.3"), null, ZonedDateTime.now().minusMinutes(8)));
        final Plugin p5 = entityManager.persist(new Plugin(
                "plugin-5", new VersionNumber("1.3"), null, ZonedDateTime.now().minusMinutes(13)));
        final Plugin p6 = entityManager.persist(new Plugin(
                "plugin-6", new VersionNumber("1.3"), null, ZonedDateTime.now().minusMinutes(34)));
        final Plugin p7 = entityManager.persist(new Plugin(
                "plugin-7", new VersionNumber("1.3"), null, ZonedDateTime.now().minusMinutes(21)));

        final Score p1s = new Score(p1, ZonedDateTime.now());
        p1s.addDetail(new ScoreResult(s1Key, 50, .5f, Set.of(), 1));

        final Score p1sOld = new Score(p1, ZonedDateTime.now().minusMinutes(10));
        p1sOld.addDetail(new ScoreResult(s1Key, 100, 1, Set.of(), 1));

        final Score p2s = new Score(p2, ZonedDateTime.now());
        p2s.addDetail(new ScoreResult(s1Key, 0, 1, Set.of(), 1));

        final Score p2sOld = new Score(p2, ZonedDateTime.now().minusMinutes(5));
        p2sOld.addDetail(new ScoreResult(s1Key, 90, 1, Set.of(), 1));

        final Score p3s = new Score(p3, ZonedDateTime.now());
        p3s.addDetail(new ScoreResult(s1Key, 100, 1, Set.of(), 1));

        final Score p4s = new Score(p4, ZonedDateTime.now());
        p4s.addDetail(new ScoreResult(s1Key, 75, 1, Set.of(), 1));

        final Score p5s = new Score(p5, ZonedDateTime.now());
        p5s.addDetail(new ScoreResult(s1Key, 80, 1, Set.of(), 1));

        final Score p6s = new Score(p6, ZonedDateTime.now());
        p6s.addDetail(new ScoreResult(s1Key, 42, 1, Set.of(), 1));

        final Score p7s = new Score(p7, ZonedDateTime.now());
        p7s.addDetail(new ScoreResult(s1Key, 0, 1, Set.of(), 1));

        Set.of(p1s, p1sOld, p2s, p2sOld, p3s, p4s, p5s, p6s, p7s).forEach(scoreService::save);
        assertThat(scoreRepository.count()).isEqualTo(9);

        final Optional<ScoreService.ScoreStatistics> scoresStatistics = scoreService.getScoresStatistics();

        assertThat(scoresStatistics).contains(new ScoreService.ScoreStatistics(50, 0, 100, 0, 50, 80));
    }

    @Test
    void shouldBeAbleToFindLatestScoreForPluginByName() {
        final String name = "foo";
        final Plugin plugin = entityManager.persist(new Plugin(
                name, new VersionNumber("1.0"), "scm", ZonedDateTime.now().minusMinutes(5)));
        final Score score = entityManager.persist(new Score(plugin, ZonedDateTime.now()));

        assertThat(scoreService.latestScoreFor(plugin)).contains(score);
    }

    @Test
    void shouldBeAbleToRetrieveAllPluginsWithSpecificScore() {
        final Plugin p1 =
                entityManager.persist(new Plugin("foo", new VersionNumber("1.0"), "scm", ZonedDateTime.now()));
        final Plugin p2 =
                entityManager.persist(new Plugin("bar", new VersionNumber("1.1"), "scm", ZonedDateTime.now()));
        final Plugin p3 =
                entityManager.persist(new Plugin("zoo", new VersionNumber("1.1"), "scm", ZonedDateTime.now()));

        final Score s1 = new Score(p1, ZonedDateTime.now().minusDays(1));
        s1.addDetail(new ScoreResult("key-1", 100, 1, Set.of(), 1));
        final Score s2 = new Score(p1, ZonedDateTime.now());
        s2.addDetail(new ScoreResult("key-1", 50, 1, Set.of(), 1));
        final Score s3 = new Score(p2, ZonedDateTime.now());
        s3.addDetail(new ScoreResult("key-1", 50, 1, Set.of(), 1));
        s3.addDetail(new ScoreResult("key-2", 100, 1, Set.of(), 1));
        final Score s4 = new Score(p3, ZonedDateTime.now());
        s4.addDetail(new ScoreResult("key-1", 75, 1, Set.of(), 1));

        entityManager.persist(s1);
        entityManager.persist(s2);
        entityManager.persist(s3);
        entityManager.persist(s4);

        assertThat(scoreService.getAllLatestScoresWithValue(100)).isEmpty();
        assertThat(scoreService.getAllLatestScoresWithValue(50)).containsExactly(s2);
        assertThat(scoreService.getAllLatestScoresWithValue(75)).containsExactlyInAnyOrder(s3, s4);
    }

    @Test
    void shouldBeAbleToRetrieveScoresWithIncompleteSections() {
        final Plugin p1 =
                entityManager.persist(new Plugin("foo", new VersionNumber("1.0"), "scm", ZonedDateTime.now()));
        final Plugin p2 =
                entityManager.persist(new Plugin("bar", new VersionNumber("1.1"), "scm", ZonedDateTime.now()));
        final Plugin p3 =
                entityManager.persist(new Plugin("zoo", new VersionNumber("1.1"), "scm", ZonedDateTime.now()));

        final Score s1 = new Score(p1, ZonedDateTime.now().minusDays(1));
        s1.addDetail(new ScoreResult("key-1", 90, 1, Set.of(), 1));
        s1.addDetail(new ScoreResult("key-2", 80, 1, Set.of(), 1));
        final Score s2 = new Score(p1, ZonedDateTime.now());
        s2.addDetail(new ScoreResult("key-1", 50, 1, Set.of(), 1));
        s2.addDetail(new ScoreResult("key-2", 80, 1, Set.of(), 1));
        final Score s3 = new Score(p2, ZonedDateTime.now());
        s3.addDetail(new ScoreResult("key-1", 100, 1, Set.of(), 1));
        s3.addDetail(new ScoreResult("key-2", 100, 1, Set.of(), 1));
        final Score s4 = new Score(p3, ZonedDateTime.now());
        s4.addDetail(new ScoreResult("key-1", 75, 1, Set.of(), 1));
        s4.addDetail(new ScoreResult("key-2", 100, 1, Set.of(), 1));

        entityManager.persist(s1);
        entityManager.persist(s2);
        entityManager.persist(s3);
        entityManager.persist(s4);

        assertThat(scoreService.getAllLatestScoresWithIncompleteScoring("key-1"))
                .containsExactlyInAnyOrder(s2, s4);
        assertThat(scoreService.getAllLatestScoresWithIncompleteScoring("key-2"))
                .containsOnly(s2);
        assertThat(scoreService.getAllLatestScoresWithIncompleteScoring("key-3"))
                .containsExactlyInAnyOrder(s2, s3, s4);
    }
}
