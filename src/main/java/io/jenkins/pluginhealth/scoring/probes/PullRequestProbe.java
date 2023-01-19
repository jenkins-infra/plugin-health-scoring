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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Matcher;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;

import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order
public class PullRequestProbe extends Probe {
    private static final Logger LOGGER = LoggerFactory.getLogger(PullRequestProbe.class);

    public static final int ORDER = LastCommitDateProbe.ORDER + 1;
    public static final String KEY = "pull-request";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final ProbeResult scmValidationResult = plugin.getDetails().get(SCMLinkValidationProbe.KEY);
        if (scmValidationResult == null || !ResultStatus.SUCCESS.equals(scmValidationResult.status())) {
            return ProbeResult.error(key(), "SCM link is not valid, cannot continue");
        }

        try {
            final GitHub gh = context.getGitHub();
            final GHRepository repository = gh.getRepository(getRepositoryName(plugin.getScm()).orElseThrow());
            final List<GHPullRequest> pullRequests = repository.getPullRequests(GHIssueState.OPEN);

            return ProbeResult.success(key(), "%d".formatted(pullRequests.size()));
        } catch (NoSuchElementException | IOException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage());
            }
            return ProbeResult.failure(key(), e.getMessage());
        }
    }

    private Optional<String> getRepositoryName(String scm) {
        final Matcher match = SCMLinkValidationProbe.GH_PATTERN.matcher(scm);
        return match.find() ? Optional.of(match.group("repo")) : Optional.empty();
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Count the number of open pull request on the plugin repository";
    }
}
