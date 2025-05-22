/*
 * MIT License
 *
 * Copyright (c) 2023-2025 Jenkins Infra
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
import java.time.LocalDate;
import java.time.ZoneOffset;
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
        if (context.getRepositoryName().isEmpty()) {
            return this.error("There is no repository for " + plugin.getName() + ".");
        }
        try {
            final LocalDate ninetyDaysAgo = LocalDate.now()
                    .minusDays(90)
                    .atStartOfDay()
                    .atOffset(ZoneOffset.UTC)
                    .toLocalDate();
            final GitHub gh = context.getGitHub();
            final GHRepository repository =
                    gh.getRepository(context.getRepositoryName().get());
            final List<GHPullRequest> pullRequests = repository.getPullRequests(GHIssueState.OPEN);

            final long count = pullRequests.stream()
                    .filter(pr -> pr.getLabels().stream().anyMatch(label -> "dependencies".equals(label.getName())))
                    .filter(pr -> {
                        try {
                            return pr.getCreatedAt()
                                    .atOffset(ZoneOffset.UTC)
                                    .toLocalDate()
                                    .atStartOfDay()
                                    .toLocalDate()
                                    .isBefore(ninetyDaysAgo);
                        } catch (IOException e) {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug(e.getMessage());
                            }
                            return false;
                        }
                    })
                    .count();

            return this.success("%d".formatted(count));
        } catch (NoSuchElementException | IOException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e.getMessage());
            }

            return this.error("Could not count dependabot pull requests.");
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
    public long getVersion() {
        return 2;
    }
}
