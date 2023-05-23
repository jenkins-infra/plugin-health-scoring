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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.regex.Matcher;

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
 * Using the analysis done by {@link SCMLinkValidationProbe},
 * this probe will find number of unreleased commits in a repository
 */
@Component
@Order(value = HasUnreleasedProductionChangesProbe.ORDER)
public class HasUnreleasedProductionChangesProbe  extends Probe {
    private static final Logger LOGGER = LoggerFactory.getLogger(HasUnreleasedProductionChangesProbe.class);

    public static final int ORDER = LastCommitDateProbe.ORDER + 100;
    public static final String KEY = "unreleased-production-changes";

    @Override
    public ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final Matcher matcher = SCMLinkValidationProbe.GH_PATTERN.matcher(plugin.getScm());
        if (!matcher.find()) {
            return ProbeResult.failure(key(), "The SCM link is not valid");
        }
        final String folder = matcher.group("folder");

        try (Git git = Git.init().setDirectory(context.getScmRepository().toFile()).call()) {
            final LogCommand logCommand = git.log().setMaxCount(1);
            if (folder != null) {
                logCommand.addPath(folder);
            }
            logCommand.addPath("pom.xml");
            logCommand.addPath("src/");

            final RevCommit commit = logCommand.call().iterator().next();

            if (commit == null) {
                return ProbeResult.success(key(), "All the commits have been released successfully for the plugin.");
            }

            Instant instant = Instant.ofEpochSecond(commit.getCommitTime());
            ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.of("UTC"));

            final ZonedDateTime commitDate = ZonedDateTime.ofInstant(
                commit.getAuthorIdent().getWhenAsInstant(),
                commit.getAuthorIdent().getZoneId()
            );

            if (zonedDateTime.isAfter(plugin.getReleaseTimestamp())) {
                return ProbeResult.failure(key(), "Unreleased commits exists in the plugin");
            }
            context.setLastCommitDate(commitDate);
            return ProbeResult.success(key(), "All the commits have been released successfully for the plugin.");
        } catch (GitAPIException ex) {
            LOGGER.error("There was an issue while cloning the plugin repository", ex);
            return ProbeResult.error(key(), "Could not clone the plugin repository");
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
    public String[] getProbeResultRequirement() {
        return new String[]{SCMLinkValidationProbe.KEY, LastCommitDateProbe.KEY};
    }
}