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

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

class PullRequestProbeTest extends AbstractProbeTest<PullRequestProbe> {
    @Override
    PullRequestProbe getSpy() {
        return spy(PullRequestProbe.class);
    }

    @Test
    void shouldUsePullRequestKey() {
        assertThat(getSpy().key()).isEqualTo("pull-request");
    }

    @Test
    void shouldNotRequireNewRelease() {
        assertThat(getSpy().requiresRelease()).isFalse();
    }

    @Test
    void shouldNotBeRelatedToSourceCode() {
        assertThat(getSpy().isSourceCodeRelated()).isFalse();
    }

    @Test
    void shouldHaveDescription() {
        assertThat(getSpy().getDescription()).isNotBlank();
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldNotRunWithInvalidSCMLink() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getDetails()).thenReturn(
            Map.of(),
            Map.of(
                SCMLinkValidationProbe.KEY, ProbeResult.failure(SCMLinkValidationProbe.KEY, "not valid")
            )
        );

        final PullRequestProbe probe = getSpy();
        for (int i = 0; i < 2; i++) {
            assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status")
                .isEqualTo(ProbeResult.error(PullRequestProbe.KEY, ""));
            verify(probe, never()).doApply(plugin, ctx);
        }
    }

    @Test
    void shouldBeAbleToCountOpenPullRequest() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        final GitHub gh = mock(GitHub.class);
        final GHRepository ghRepository = mock(GHRepository.class);

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, "valid")
        ));
        when(ctx.getGitHub()).thenReturn(gh);
        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/mailer-plugin");
        when(ctx.getRepositoryName(plugin.getScm())).thenReturn(Optional.of("jenkinsci/mailer-plugin"));
        when(gh.getRepository(anyString())).thenReturn(ghRepository);
        final List<GHPullRequest> ghPullRequests = List.of(
            new GHPullRequest(),
            new GHPullRequest(),
            new GHPullRequest()
        );
        when(ghRepository.getPullRequests(GHIssueState.OPEN)).thenReturn(ghPullRequests);

        final PullRequestProbe probe = getSpy();
        final ProbeResult result = probe.apply(plugin, ctx);

        verify(ctx).getGitHub();
        verify(gh.getRepository(anyString())).getPullRequests(GHIssueState.OPEN);

        assertThat(result)
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(PullRequestProbe.KEY, "%d".formatted(ghPullRequests.size())));
    }

    @Test
    void shouldFailIfCommunicationWithGitHubIsImpossible() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        final GitHub gh = mock(GitHub.class);

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, "valid")
        ));
        when(ctx.getGitHub()).thenReturn(gh);
        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/mailer-plugin");
        when(ctx.getRepositoryName(plugin.getScm())).thenReturn(Optional.of("jenkinsci/mailer-plugin"));
        when(gh.getRepository(anyString())).thenThrow(IOException.class);

        final PullRequestProbe probe = getSpy();
        final ProbeResult result = probe.apply(plugin, ctx);

        verify(ctx).getGitHub();

        assertThat(result)
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status")
            .isEqualTo(ProbeResult.failure(PullRequestProbe.KEY, ""));

    }
}
