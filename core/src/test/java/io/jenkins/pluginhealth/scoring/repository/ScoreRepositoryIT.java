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

package io.jenkins.pluginhealth.scoring.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;
import java.util.List;

import io.jenkins.pluginhealth.scoring.AbstractDBContainerTest;
import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.Score;

import hudson.util.VersionNumber;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class ScoreRepositoryIT extends AbstractDBContainerTest {
    @Autowired private ScoreRepository repository;
    @Autowired private TestEntityManager entityManager;

    @Test
    void shouldBeAbleToDeleteOldScores() {
        final Plugin plugin1 = entityManager.persist(new Plugin("plugin-1", new VersionNumber("1.0"), "scm", ZonedDateTime.now()));
        final Plugin plugin2 = entityManager.persist(new Plugin("plugin-2", new VersionNumber("1.0"), "scm", ZonedDateTime.now()));
        final Plugin plugin3 = entityManager.persist(new Plugin("plugin-3", new VersionNumber("1.1"), "scm", ZonedDateTime.now()));

        final Score oldScore1 = entityManager.persist(new Score(plugin1, ZonedDateTime.now().minusDays(1)));
        final Score oldScore2 = entityManager.persist(new Score(plugin1, ZonedDateTime.now().minusMonths(1)));
        final Score recentScore1 = entityManager.persist(new Score(plugin1, ZonedDateTime.now().minusHours(8)));
        final Score recentScore2 = entityManager.persist(new Score(plugin1, ZonedDateTime.now()));
        final Score recentScore3 = entityManager.persist(new Score(plugin1, ZonedDateTime.now()));
        final Score recentScore4 = entityManager.persist(new Score(plugin1, ZonedDateTime.now()));
        final Score recentScore5 = entityManager.persist(new Score(plugin1, ZonedDateTime.now()));

        final Score oldScore21 = entityManager.persist(new Score(plugin2, ZonedDateTime.now().minusMonths(1)));
        final Score oldScore22 = entityManager.persist(new Score(plugin2, ZonedDateTime.now().minusDays(5)));
        final Score recentScore01 = entityManager.persist(new Score(plugin2, ZonedDateTime.now().minusDays(3)));
        final Score recentScore02 = entityManager.persist(new Score(plugin2, ZonedDateTime.now().minusDays(2)));
        final Score recentScore03 = entityManager.persist(new Score(plugin2, ZonedDateTime.now().minusDays(1)));
        final Score recentScore04 = entityManager.persist(new Score(plugin2, ZonedDateTime.now().minusHours(15)));
        final Score recentScore05 = entityManager.persist(new Score(plugin2, ZonedDateTime.now().minusDays(4)));

        final Score oldScore31 = entityManager.persist(new Score(plugin3, ZonedDateTime.now().minusDays(7)));
        final Score oldScore32 = entityManager.persist(new Score(plugin3, ZonedDateTime.now().minusDays(6)));
        final Score recentScore001 = entityManager.persist(new Score(plugin3, ZonedDateTime.now().minusDays(5)));
        final Score recentScore002 = entityManager.persist(new Score(plugin3, ZonedDateTime.now().minusDays(4)));
        final Score recentScore003 = entityManager.persist(new Score(plugin3, ZonedDateTime.now().minusDays(3)));
        final Score recentScore004 = entityManager.persist(new Score(plugin3, ZonedDateTime.now().minusDays(2)));
        final Score recentScore005 = entityManager.persist(new Score(plugin3, ZonedDateTime.now().minusDays(1)));

        long noOfRowsDeleted = repository.deleteOldScoreFromPlugin();
        List<Score> remainingScores = repository.findAll();

        assertThat(noOfRowsDeleted).isEqualTo(6);
        assertThat(remainingScores)
            .hasSize(15)
            .doesNotContain(oldScore1, oldScore2, oldScore21, oldScore22, oldScore31, oldScore32)
            .containsExactlyInAnyOrder(
                 recentScore1, recentScore2, recentScore3, recentScore4, recentScore5,
                 recentScore01, recentScore02, recentScore03, recentScore04, recentScore05,
                 recentScore001, recentScore002, recentScore003, recentScore004, recentScore005
            );
    }
}


