package io.jenkins.pluginhealth.scoring.probes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Using the analysis done by {@link SCMLinkValidationProbe}, this probe determines the last commit date of the plugin's repository.
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

            if (plugin.getDetails().get(SCMLinkValidationProbe.KEY).status() == ResultStatus.SUCCESS) {
                try {
                    final Path tempDirectory = Files.createTempDirectory(plugin.getName());
                    try (Git git = Git.cloneRepository().setURI(plugin.getScm()).setDirectory(tempDirectory.toFile()).call()) {
                        final ObjectId head = git.getRepository().resolve(Constants.HEAD);
                        final RevCommit commit = new RevWalk(git.getRepository()).parseCommit(head);
                        final ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(
                            commit.getAuthorIdent().getWhenAsInstant(),
                            commit.getAuthorIdent().getZoneId()
                        );
                        return ProbeResult.success(key(), zonedDateTime.toString());
                    } finally {
                        try (Stream<Path> paths = Files.walk(tempDirectory)) {
                            paths.sorted(Comparator.reverseOrder())
                                .map(Path::toFile)
                                .forEach(File::delete);
                        }
                    }
                } catch (IOException ex) {
                    return ProbeResult.failure(key(), "Error during probe execution on " + plugin.getName());
                }
            } else {
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

    @Override
    public String key() {
        return "last-commit-date";
    }

    @Override
    public String getDescription() {
        return "Register the last commit date on the official plugin repository";
    }
}
