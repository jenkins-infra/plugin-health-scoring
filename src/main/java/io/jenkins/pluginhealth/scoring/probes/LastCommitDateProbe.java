package io.jenkins.pluginhealth.scoring.probes;

import java.io.File;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;

import org.slf4j.Logger;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Using the analysis done by {@link SCMLinkValidationProbe#}, this probe determines the last commit date of the plugin's repository.
 */
@Component
@Order(value = LastCommitDateProbe.ORDER)
public class LastCommitDateProbe extends Probe {
    private static final Logger LOGGER = LoggerFactory.getLogger(LastCommitDateProbe.class);

    public static final int ORDER = SCMLinkValidationProbe.ORDER + 1;

    @Override
    public ProbeResult doApply(Plugin plugin) {
        try {
            if (plugin.getDetails().get(SCMLinkValidationProbe.KEY) == null) {
                LOGGER.error("Couldn't run {} on {} because previous SCMLinkValidationProbe has null value in database", key(), plugin.getName());
                return ProbeResult.error(key(), "SCM link has not been probed yet");
            }

            if (plugin.getDetails().get(scmLinkValidationProbeKey).status().equals(ResultStatus.SUCCESS)) {
                File dir = new File(System.getProperty("java.io.tmpdir") + "/" + plugin.getName());

                Git git = Git.cloneRepository()
                    .setURI(plugin.getScm())
                    .setDirectory(dir)
                    .call();

                final RevCommit lastCommit = git.log().setMaxCount(1).call().iterator().next();
                final ZonedDateTime lastCommitDateTime = ZonedDateTime.ofInstant(lastCommit.getAuthorIdent().getWhen().toInstant(), ZoneId.systemDefault());

                deleteDirectory(dir);
                return ProbeResult.success(key(), String.valueOf(lastCommitDateTime));
            }
            else {
                return ProbeResult.failure(key(), "Due to invalid SCM, latest commit date cannot be found");
            }
        }
        catch (GitAPIException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Invalid SCM link", e);
            }
            return ProbeResult.failure(key(), "Due to invalid SCM, latest commit date cannot be found");
        }
    }

    public void deleteDirectory(File dir) {
        for (File file: dir.listFiles()) {
            if (file.isDirectory())
                deleteDirectory(file);
            file.delete();
        }
        dir.delete();
    }

    @Override
    public String key() {
        return "last-commit-date";
    }
}
