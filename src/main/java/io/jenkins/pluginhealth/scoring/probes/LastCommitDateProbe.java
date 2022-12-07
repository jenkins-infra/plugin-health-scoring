/*
 * MIT License
 *
 * Copyright (c) 2022 Jenkins Infra
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

import java.time.ZonedDateTime;
import java.util.regex.Matcher;

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
    public static final String KEY = "last-commit-date";

    @Override
    public ProbeResult doApply(Plugin plugin, ProbeContext context) {
        if (plugin.getDetails().get(SCMLinkValidationProbe.KEY) == null) {
            LOGGER.error("Couldn't run {} on {} because previous SCMLinkValidationProbe has null value in database", key(), plugin.getName());
            return ProbeResult.error(key(), "SCM link has not been validated yet");
        }

        if (plugin.getDetails().get(SCMLinkValidationProbe.KEY).status() == ResultStatus.SUCCESS) {
            final Matcher matcher = SCMLinkValidationProbe.GH_PATTERN.matcher(plugin.getScm());
            if (!matcher.find()) {
                return ProbeResult.failure(key(), "The SCM link is not valid");
            }
            final String repo = String.format("https://%s/%s", matcher.group("server"), matcher.group("repo"));
            final String folder = matcher.group("folder");

            try (Git git = Git.cloneRepository().setURI(repo).setDirectory(context.getScmRepository().toFile()).call()) {
                final LogCommand logCommand = git.log().setMaxCount(1);
                if (folder != null) {
                    logCommand.addPath(folder);
                }
                final RevCommit commit = logCommand.call().iterator().next();
                if (commit == null) {
                    return ProbeResult.failure(key(), "Last commit cannot be found");
                }
                final ZonedDateTime commitDate = ZonedDateTime.ofInstant(
                    commit.getAuthorIdent().getWhenAsInstant(),
                    commit.getAuthorIdent().getZoneId()
                );
                context.setLastCommitDate(commitDate);
                return ProbeResult.success(key(), commitDate.toString());
            } catch (GitAPIException ex) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("There was an issue while cloning the plugin repository", ex);
                }
                return ProbeResult.failure(key(), "Could not clone the plugin repository");
            }
        } else {
            return ProbeResult.failure(key(), "Due to invalid SCM, latest commit date cannot be found");
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
        return true;
    }
}
