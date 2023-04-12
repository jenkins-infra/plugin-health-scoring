package io.jenkins.pluginhealth.scoring.schedule;

import io.jenkins.pluginhealth.scoring.service.ScoreService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeleteOldScoreScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteOldScoreScheduler.class);
    private final ScoreService scoreService;

    public DeleteOldScoreScheduler(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @Scheduled(cron = "@midnight", zone = "UTC")
    public void deleteOldScores() {
        LOGGER.info("Deleting old scores");

        int numberOfRowsDeleted  = scoreService.deleteOldScores();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Deleted {} rows when deleting old scores", numberOfRowsDeleted);
        }
    }
}
