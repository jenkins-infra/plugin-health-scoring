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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

class DependabotPullRequestProbeTest extends AbstractProbeTest<DependabotPullRequestProbe> {
    @Override
    DependabotPullRequestProbe getSpy() {
        return spy(DependabotPullRequestProbe.class);
    }

    @Test
    void shouldNotRequireRelease() {
        assertThat(getSpy().requiresRelease()).isFalse();
    }

    @Test
    void shouldNotBeRelatedToSourceCode() {
        assertThat(getSpy().isSourceCodeRelated()).isFalse();
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldBeSkippedWhenNoDependabot() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getDetails()).thenReturn(
            Map.of(),
            Map.of(DependabotProbe.KEY, ProbeResult.failure(DependabotProbe.KEY, ""))
        );

        final DependabotPullRequestProbe probe = getSpy();

        assertThat(probe.apply(plugin, ctx)).usingRecursiveComparison()
            .comparingOnlyFields("id", "status")
            .isEqualTo(ProbeResult.error(DependabotPullRequestProbe.KEY, ""));
        assertThat(probe.apply(plugin, ctx)).usingRecursiveComparison()
            .comparingOnlyFields("id", "status")
            .isEqualTo(ProbeResult.error(DependabotPullRequestProbe.KEY, ""));
    }

    @Test
    void shouldAccessGitHubAPIAndSeeNoPullRequest() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        final GitHub gh = mock(GitHub.class);
        final GHRepository ghRepository = mock(GHRepository.class);

        when(plugin.getDetails()).thenReturn(Map.of(
            DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, "")
        ));
        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/mailer-plugin");

        when(ctx.getGitHub()).thenReturn(gh);
        when(ctx.getRepositoryName(plugin.getScm())).thenReturn(Optional.of("jenkinsci/mailer-plugin"));
        when(gh.getRepository(anyString())).thenReturn(ghRepository);

        final GHLabel dependenciesLabel = mock(GHLabel.class);
        when(dependenciesLabel.getName()).thenReturn("dependencies");

        final GHPullRequest pr_1 = mock(GHPullRequest.class);
        final GHPullRequest pr_2 = mock(GHPullRequest.class);
        final GHPullRequest pr_3 = mock(GHPullRequest.class);
        when(ghRepository.getPullRequests(GHIssueState.OPEN)).thenReturn(
            List.of(pr_1, pr_2, pr_3)
        );

        final DependabotPullRequestProbe probe = getSpy();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result).usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(DependabotPullRequestProbe.KEY, "No open pull request from dependabot"));
    }

    @Test
    void shouldAccessGitHubAPIWhenDependabotActivatedWithOpenedPR() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        final GitHub gh = mock(GitHub.class);
        final GHRepository ghRepository = mock(GHRepository.class);

        when(plugin.getDetails()).thenReturn(Map.of(
            DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, "")
        ));
        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/mailer-plugin");

        when(ctx.getGitHub()).thenReturn(gh);
        when(ctx.getRepositoryName(plugin.getScm())).thenReturn(Optional.of("jenkinsci/mailer-plugin"));
        when(gh.getRepository(anyString())).thenReturn(ghRepository);

        final GHLabel dependenciesLabel = mock(GHLabel.class);
        when(dependenciesLabel.getName()).thenReturn("dependencies");

        final GHPullRequest pr_1 = mock(GHPullRequest.class);
        when(pr_1.getLabels()).thenReturn(List.of(dependenciesLabel));
        final GHPullRequest pr_2 = mock(GHPullRequest.class);
        final GHPullRequest pr_3 = mock(GHPullRequest.class);
        when(pr_3.getLabels()).thenReturn(List.of(dependenciesLabel));
        final GHPullRequest pr_4 = mock(GHPullRequest.class);
        final GHPullRequest pr_5 = mock(GHPullRequest.class);
        when(ghRepository.getPullRequests(GHIssueState.OPEN)).thenReturn(
            List.of(pr_1, pr_2, pr_3, pr_4, pr_5)
        );

        final DependabotPullRequestProbe probe = getSpy();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result).usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.failure(DependabotPullRequestProbe.KEY, "2 open pull requests from Dependabot"));
    }

    @Test
    void shouldFailProperlyWhenIssueCommunicatingWithGitHub() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        final GitHub gh = mock(GitHub.class);

        when(plugin.getDetails()).thenReturn(Map.of(
            DependabotProbe.KEY, ProbeResult.success(DependabotProbe.KEY, "")
        ));
        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/mailer-plugin");
        when(ctx.getRepositoryName(plugin.getScm())).thenReturn(Optional.of("foo-bar"));

        when(ctx.getGitHub()).thenReturn(gh);
        when(gh.getRepository(anyString())).thenThrow(IOException.class);

        final DependabotPullRequestProbe probe = getSpy();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result)
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.error(DependabotPullRequestProbe.KEY, "Could not count dependabot pull requests"));
    }
}
