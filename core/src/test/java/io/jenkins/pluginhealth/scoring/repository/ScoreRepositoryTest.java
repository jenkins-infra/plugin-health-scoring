package io.jenkins.pluginhealth.scoring.repository;

import static org.assertj.core.api.Assertions.assertThat;

import hudson.util.VersionNumber;
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
public class ScoreRepositoryTest {

    @Autowired
    private ScoreRepository repository;

    @Autowired private TestEntityManager entityManager;

    @Test
    void shouldBeAbleToDeleteOldScores() {
        final Plugin plugin1 = new Plugin("plugin-1", new VersionNumber("1.0"), "scm", ZonedDateTime.now());
        entityManager.persist(plugin1);
        final Plugin plugin2 = new Plugin("plugin-2", new VersionNumber("1.0"), "scm", ZonedDateTime.now());
        entityManager.persist(plugin2);
        final Plugin plugin3 = new Plugin("plugin-3", new VersionNumber("1.1"), "scm", ZonedDateTime.now());
        entityManager.persist(plugin3);

        final Score score1 = new Score(plugin1, ZonedDateTime.now());
        entityManager.persist(score1);
        final Score score2 = new Score(plugin1, ZonedDateTime.now());
        entityManager.persist(score2);
        final Score score3 = new Score(plugin1, ZonedDateTime.now());
        entityManager.persist(score3);
        final Score score4 = new Score(plugin1, ZonedDateTime.now());
        entityManager.persist(score4);
        final Score score5 = new Score(plugin1, ZonedDateTime.now());
        entityManager.persist(score5);
        final Score score6 = new Score(plugin1, ZonedDateTime.now());
        entityManager.persist(score6);
        final Score score7 = new Score(plugin1, ZonedDateTime.now());
        entityManager.persist(score7);

        final Score score21 = new Score(plugin2, ZonedDateTime.now());
        entityManager.persist(score21);
        final Score score22 = new Score(plugin2, ZonedDateTime.now());
        entityManager.persist(score22);
        final Score score23 = new Score(plugin2, ZonedDateTime.now());
        entityManager.persist(score23);
        final Score score24 = new Score(plugin2, ZonedDateTime.now());
        entityManager.persist(score24);
        final Score score25 = new Score(plugin2, ZonedDateTime.now());
        entityManager.persist(score25);
        final Score score26 = new Score(plugin2, ZonedDateTime.now());
        entityManager.persist(score26);
        final Score score27 = new Score(plugin2, ZonedDateTime.now());
        entityManager.persist(score27);

        final Score score31 = new Score(plugin3, ZonedDateTime.now());
        entityManager.persist(score31);
        final Score score32 = new Score(plugin3, ZonedDateTime.now());
        entityManager.persist(score32);
        final Score score33 = new Score(plugin3, ZonedDateTime.now());
        entityManager.persist(score33);
        final Score score34 = new Score(plugin3, ZonedDateTime.now());
        entityManager.persist(score34);
        final Score score35 = new Score(plugin3, ZonedDateTime.now());
        entityManager.persist(score35);
        final Score score36 = new Score(plugin3, ZonedDateTime.now());
        entityManager.persist(score36);
        final Score score37 = new Score(plugin3, ZonedDateTime.now());
        entityManager.persist(score37);

        long noOfRowsDeleted = repository.deleteOldScoreFromPlugin();
        assertThat(noOfRowsDeleted).isEqualTo(6);
    }
}


