package io.jenkins.pluginhealth.scoring.probes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
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
        if (plugin.getDetails().get(SCMLinkValidationProbe.KEY) == null) {
            LOGGER.error("Couldn't run {} on {} because previous SCMLinkValidationProbe has null value in database", key(), plugin.getName());
            return ProbeResult.error(key(), "SCM link has not been validated yet");
        }

        if (plugin.getDetails().get(SCMLinkValidationProbe.KEY).status() == ResultStatus.SUCCESS) {
            try {
                Path tempDirectory = Files.createTempDirectory(plugin.getName());
                final Matcher matcher = SCMLinkValidationProbe.GH_PATTERN.matcher(plugin.getScm());
                if (!matcher.find()) {
                    return ProbeResult.failure(key(), "The SCM link is not valid");
                }
                final String repo = String.format("https://%s/%s", matcher.group("server"), matcher.group("repo"));
                final String folder = matcher.group("folder");

                try (Git git = Git.cloneRepository().setURI(repo).setDirectory(tempDirectory.toFile()).call()) {
                    final LogCommand logCommand = git.log().setMaxCount(1);
                    if (folder != null) {
                        logCommand.addPath(folder);
                    }
                    final RevCommit commit = logCommand.call().iterator().next();
                    if (commit == null) {
                        return ProbeResult.failure(key(), "Last commit cannot be found");
                    }
                    final ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(
                        commit.getAuthorIdent().getWhenAsInstant(),
                        commit.getAuthorIdent().getZoneId()
                    );
                    return ProbeResult.success(key(), zonedDateTime.toString());
                } catch (GitAPIException ex) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("There was an issue while cloning the plugin repository", ex);
                    }
                    return ProbeResult.failure(key(), "Could not clone the plugin repository");
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

    @Override
    public String key() {
        return "last-commit-date";
    }

    @Override
    public String getDescription() {
        return "Register the last commit date on the official plugin repository";
    }
}
