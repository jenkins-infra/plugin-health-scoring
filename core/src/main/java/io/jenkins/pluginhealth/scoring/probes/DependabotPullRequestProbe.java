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

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(DependabotPullRequestProbe.ORDER)
public class DependabotPullRequestProbe extends Probe {
    private static final Logger LOGGER = LoggerFactory.getLogger(DependabotPullRequestProbe.class);

    public static final String KEY = "dependabot-pull-requests";
    public static final int ORDER = DependabotProbe.ORDER + 10;

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        try {
            final GitHub gh = context.getGitHub();
            final GHRepository repository = gh.getRepository(context.getRepositoryName(plugin.getScm()).orElseThrow());
            final List<GHPullRequest> pullRequests = repository.getPullRequests(GHIssueState.OPEN);

            final long count = pullRequests.stream()
                .filter(pr -> pr.getLabels().stream().anyMatch(label -> "dependencies".equals(label.getName())))
                .count();

            return count > 0 ?
                ProbeResult.failure(key(), "%d open pull requests from Dependabot".formatted(count)) :
                ProbeResult.success(key(), "No open pull request from dependabot");
        } catch (NoSuchElementException | IOException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage());
            }

            return ProbeResult.error(key(), "Could not count dependabot pull requests");
        }
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Reports the number of pull request currently opened by Dependabot";
    }

    @Override
    protected String[] getProbeResultRequirement() {
        return new String[]{DependabotProbe.KEY};
    }
}
