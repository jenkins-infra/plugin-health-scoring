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

package io.jenkins.pluginhealth.scoring.probes;

import java.io.IOException;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

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
    public static final int ORDER = SCMLinkValidationProbe.ORDER + 100;
    public static final String KEY = "last-commit-date";
    private static final Logger LOGGER = LoggerFactory.getLogger(LastCommitDateProbe.class);

    @Override
    public ProbeResult doApply(Plugin plugin, ProbeContext context) {
        if (context.getScmRepository().isEmpty()) {
            return ProbeResult.error(key(), "There is no local repository for plugin " + plugin.getName() + ".", this.getVersion());
        }
        final Path scmRepository = context.getScmRepository().get();
        final Optional<Path> folder = context.getScmFolderPath();
        try (Git git = Git.open(scmRepository.toFile())) {
            final LogCommand logCommand = git.log().setMaxCount(1);
            if (folder.isPresent() && !folder.get().toString().isBlank()) {
                logCommand.addPath(folder.get().toString());
            }

            final RevCommit commit = logCommand.call().iterator().next();
            if (commit == null) {
                return ProbeResult.error(key(), "Last commit cannot be extracted. Please validate sub-folder if any.", this.getVersion());
            }
            final ZonedDateTime commitDate = ZonedDateTime.ofInstant(
                    commit.getAuthorIdent().getWhenAsInstant(),
                    commit.getAuthorIdent().getZoneId()
                ).withZoneSameInstant(ZoneId.of("UTC"))
                .truncatedTo(ChronoUnit.SECONDS);
            context.setLastCommitDate(commitDate);
            return ProbeResult.success(key(), commitDate.format(DateTimeFormatter.ISO_DATE_TIME), this.getVersion());
        } catch (IOException | GitAPIException ex) {
            LOGGER.error("There was an issue while accessing the plugin repository", ex);
            return ProbeResult.error(key(), "Could not access the plugin repository.", this.getVersion());
        }
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Register the last commit date on the official plugin repository";
    }

    @Override
    protected boolean isSourceCodeRelated() {
        /*
         * This is counter intuitive, but this probe needs to be executed all the time.
         * So even if the probe seems to be related to code, in order to not be skipped by the
         * ProbeEngine, is must be `false`.
         */
        return false;
    }

    @Override
    public long getVersion() {
        return 1;
    }
}
