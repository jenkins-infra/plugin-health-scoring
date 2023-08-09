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

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.kohsuke.github.GHRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(AbstractOpenIssuesProbe.ORDER)
class GitHubOpenIssuesProbe extends AbstractOpenIssuesProbe {
    public static final String KEY = "github-open-issues";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        return getNumberOfOpenIssues(plugin, context);
    }

    /**
     * Get total number of open GitHub issues in a plugin
     */
    @Override
    ProbeResult getNumberOfOpenIssues(Plugin plugin, ProbeContext context) {
        try {
            final Optional<String> repositoryName = context.getRepositoryName(plugin.getScm());
            if (repositoryName.isPresent()) {
                final GHRepository ghRepository = context.getGitHub().getRepository(repositoryName.get());
                int openGitHubIssues =  ghRepository.getOpenIssueCount();
                return ProbeResult.success(key(), String.format("%d open issues found in GitHub.", openGitHubIssues));
            }
        } catch (IOException ex) {
            return ProbeResult.error(key(), String.format("Cannot not read open issues on GitHub for plugin %s.", plugin.getName()));
        }
        return ProbeResult.failure(key(), String.format("Cannot find GitHub repository for plugin %s.", plugin.getName()));

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
