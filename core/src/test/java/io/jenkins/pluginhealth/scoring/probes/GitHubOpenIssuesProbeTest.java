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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

class GitHubOpenIssuesProbeTest extends AbstractProbeTest<GitHubOpenIssuesProbe> {
    @Override
    GitHubOpenIssuesProbe getSpy() {
        return spy(GitHubOpenIssuesProbe.class);
    }

    @Test
    void shouldNotRunWithInvalidProbeResultRequirement() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getDetails()).thenReturn(
            Map.of(),
            Map.of(
                IssueTrackerDetectionProbe.KEY, ProbeResult.success(IssueTrackerDetectionProbe.KEY, "")
            ),
            Map.of(
                SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, "")
            ),
            Map.of(
                SCMLinkValidationProbe.KEY, ProbeResult.failure(SCMLinkValidationProbe.KEY, ""),
                IssueTrackerDetectionProbe.KEY, ProbeResult.failure(IssueTrackerDetectionProbe.KEY, "")
            ),
            Map.of(
                SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
                IssueTrackerDetectionProbe.KEY, ProbeResult.failure(IssueTrackerDetectionProbe.KEY, "")
            ),
            Map.of(
                SCMLinkValidationProbe.KEY, ProbeResult.failure(SCMLinkValidationProbe.KEY, ""),
                IssueTrackerDetectionProbe.KEY, ProbeResult.success(IssueTrackerDetectionProbe.KEY, "")
            )
        );

        final GitHubOpenIssuesProbe probe = getSpy();
        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.error(GitHubOpenIssuesProbe.KEY, "github-open-issues does not meet the criteria to be executed on null"));
        verify(probe, never()).doApply(plugin, ctx);
    }

    @Test
    void shouldBeAbleToFindNumberOfOpenIssuesInGH() throws IOException {
        final String pluginName = "cloudevents";
        final String repository = "jenkinsci/" + pluginName + "-plugin";
        final String scmLink = "https://github.com/" + repository;

        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final GitHub gh = mock(GitHub.class);
        final GHRepository ghRepository = mock(GHRepository.class);
        final io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin.IssueTrackers issueTrackerGithub = new io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin.IssueTrackers("github", "https://github.com/" + repository + "/issues", "https://github.com/" + repository + "/issues/new/choose");

        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getScm()).thenReturn(scmLink);
        when(plugin.getDetails()).thenReturn(
            Map.of(
                SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
                IssueTrackerDetectionProbe.KEY, ProbeResult.success(IssueTrackerDetectionProbe.KEY, "")
            )
        );

        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of(pluginName, new io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin(
                pluginName, null, null, null, List.of(), 0, "2.361.1", "main",
                List.of(issueTrackerGithub)
            )),
            Map.of(),
            List.of()
        ));

        when(ctx.getIssueTrackerNameAndUrl()).thenReturn(Map.of("github", "https://github.com/" + repository + "/issues"));

        when(ctx.getGitHub()).thenReturn(gh);
        when(ctx.getRepositoryName(plugin.getScm())).thenReturn(Optional.of(repository));
        when(gh.getRepository(repository)).thenReturn(ghRepository);
        when(ghRepository.getOpenIssueCount()).thenReturn(6);

        final GitHubOpenIssuesProbe probe = getSpy();

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(GitHubOpenIssuesProbe.KEY, "6 open issues found in the cloudevents plugin."));

        verify(probe).doApply(plugin, ctx);

    }


    @Test
    void shouldFailWhereThereIsNoGitHubTracker() throws IOException {
        final String pluginName = "foo";
        final String repository = "jenkinsci/" + pluginName + "-plugin";
        final String scmLink = "https://github.com/" + repository;

        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final GitHub gh = mock(GitHub.class);
        final GHRepository ghRepository = mock(GHRepository.class);

        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getScm()).thenReturn(scmLink);
        when(plugin.getDetails()).thenReturn(
            Map.of(
                SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
                IssueTrackerDetectionProbe.KEY, ProbeResult.success(IssueTrackerDetectionProbe.KEY, "")
            )
        );

        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of(pluginName, new io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin(
                pluginName, null, null, null, List.of(), 0, "2.361.1", "main",
                List.of()
            )),
            Map.of(),
            List.of()
        ));

        when(ctx.getIssueTrackerNameAndUrl()).thenReturn(Map.of());

        when(ctx.getGitHub()).thenReturn(gh);
        when(ctx.getRepositoryName(plugin.getScm())).thenReturn(Optional.of(repository));
        when(gh.getRepository(repository)).thenReturn(ghRepository);

        final GitHubOpenIssuesProbe probe = getSpy();

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.failure(GitHubOpenIssuesProbe.KEY, "Could not find open issues in the foo plugin."));

        verify(probe).doApply(plugin, ctx);
    }

}
