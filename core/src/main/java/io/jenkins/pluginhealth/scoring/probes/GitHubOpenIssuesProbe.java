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
import java.util.Optional;

import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(AbstractOpenIssuesProbe.ORDER)
class GitHubOpenIssuesProbe extends AbstractOpenIssuesProbe {
    public static final String KEY = "github-open-issues";
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubOpenIssuesProbe.class);

    /**
     * Get total number of open GitHub issues in a plugin.
     * @param context @see {@link ProbeContext}
     *
     * @return Optional an Integer value will give total count of open issues.
     */
    @Override
    Optional<Integer> getCountOfOpenIssues(ProbeContext context) {
        // Stores the GitHub URL to view all existing issues in the plugin. Ex: https://github.com/jenkinsci/cloudevents-plugin/issues
        String issueTrackerViewUrl = context.getIssueTrackerUrlsByNames().get("github");

        if (issueTrackerViewUrl == null) {
            LOGGER.info("The plugin does not use GitHub issues to tracker issues.");
            return Optional.empty();
        }

        try {
            final Optional<String> repositoryName = context.getRepositoryName(issueTrackerViewUrl.substring(0, issueTrackerViewUrl.lastIndexOf("/")));
            if (repositoryName.isPresent()) {
                final GHRepository ghRepository = context.getGitHub().getRepository(repositoryName.get());
                return Optional.of(ghRepository.getOpenIssueCount());
            }
        } catch (IOException ex) {
            LOGGER.error("Cannot read open issues on GitHub for the plugin. {}", ex);
        }
        return Optional.empty();
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Returns the total number of open issues in GitHub.";
    }
}
