package io.jenkins.pluginhealth.scoring.repository;

import static org.assertj.core.api.Assertions.assertThat;

import hudson.util.VersionNumber;
import io.jenkins.pluginhealth.scoring.AbstractDBContainerTest;
import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.Score;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.ZonedDateTime;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class ScoreRepositoryIT extends AbstractDBContainerTest {

    @Autowired
    private ScoreRepository repository;

    @Autowired private TestEntityManager entityManager;

    @Test
    void shouldBeAbleToDeleteOldScores() {
        final Plugin plugin1 = entityManager.persist(new Plugin("plugin-1", new VersionNumber("1.0"), "scm", ZonedDateTime.now()));
        final Plugin plugin2 = entityManager.persist(new Plugin("plugin-2", new VersionNumber("1.0"), "scm", ZonedDateTime.now()));
        final Plugin plugin3 = entityManager.persist(new Plugin("plugin-3", new VersionNumber("1.1"), "scm", ZonedDateTime.now()));

        final Score score1 = entityManager.persist(new Score(plugin1, ZonedDateTime.now().minusDays(1)));
        final Score score2 = entityManager.persist(new Score(plugin1, ZonedDateTime.now().minusHours(8)));
        final Score score3 = entityManager.persist(new Score(plugin1, ZonedDateTime.now().minusMonths(1)));
        final Score score4 = entityManager.persist(new Score(plugin1, ZonedDateTime.now()));
        final Score score5 = entityManager.persist(new Score(plugin1, ZonedDateTime.now()));
        final Score score6 = entityManager.persist(new Score(plugin1, ZonedDateTime.now()));
        final Score score7 = entityManager.persist(new Score(plugin1, ZonedDateTime.now()));

        final Score score21 = entityManager.persist(new Score(plugin2, ZonedDateTime.now().minusDays(5)));
        final Score score22 = entityManager.persist(new Score(plugin2, ZonedDateTime.now().minusDays(4)));
        final Score score23 = entityManager.persist(new Score(plugin2, ZonedDateTime.now().minusDays(3)));
        final Score score24 = entityManager.persist(new Score(plugin2, ZonedDateTime.now().minusDays(2)));
        final Score score25 = entityManager.persist(new Score(plugin2, ZonedDateTime.now().minusDays(1)));
        final Score score26 = entityManager.persist(new Score(plugin2, ZonedDateTime.now().minusHours(15)));
        final Score score27 = entityManager.persist(new Score(plugin2, ZonedDateTime.now().minusMonths(1)));

        final Score score31 = entityManager.persist(new Score(plugin3, ZonedDateTime.now().minusDays(7)));
        final Score score32 = entityManager.persist(new Score(plugin3, ZonedDateTime.now().minusDays(6)));
        final Score score33 = entityManager.persist(new Score(plugin3, ZonedDateTime.now().minusDays(5)));
        final Score score34 = entityManager.persist(new Score(plugin3, ZonedDateTime.now().minusDays(4)));
        final Score score35 = entityManager.persist(new Score(plugin3, ZonedDateTime.now().minusDays(3)));
        final Score score36 = entityManager.persist(new Score(plugin3, ZonedDateTime.now().minusDays(2)));
        final Score score37 = entityManager.persist(new Score(plugin3, ZonedDateTime.now().minusDays(1)));

        long noOfRowsDeleted = repository.deleteOldScoreFromPlugin();
        assertThat(noOfRowsDeleted).isEqualTo(6);
    }
}


