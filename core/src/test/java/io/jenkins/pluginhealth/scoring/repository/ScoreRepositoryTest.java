package io.jenkins.pluginhealth.scoring.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class ScoreRepositoryTest {

    @Autowired
    private ScoreRepository repository;

    @Test
    void shouldBeAbleToDeleteOldScores() {
        long noOfRowsDeleted = repository.deleteOldScoreFromPlugin();
    }
}


