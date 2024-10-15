/*
 * MIT License
 *
 * Copyright (c) 2023-2024 Jenkins Infra
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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;

import hudson.util.VersionNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;
import org.kohsuke.github.PagedIterator;

class DefaultBranchBuildStatusProbeTest extends AbstractProbeTest<DefaultBranchBuildStatusProbe> {

    private DefaultBranchBuildStatusProbe probe;

    @Override
    DefaultBranchBuildStatusProbe getSpy() {
        return spy(DefaultBranchBuildStatusProbe.class);
    }

    @BeforeEach
    public void init() {

        probe = getSpy();
    }

    @Test
    public void shouldReturnBuildSucessOntheDefaultBranch() throws IOException {

        final String pluginName = "mailer";
        final String pluginRepo = "jenkinsci/" + pluginName + "-plugin";
        final String scmLink = "https://github.com/" + pluginRepo;
        final String defaultBranch = "main";
        final GitHub gitHub = mock(GitHub.class);
        final GHRepository ghRepository = mock(GHRepository.class);
        final GHCommit ghCommit = mock(GHCommit.class);
        final GHCheckRun checkRun = mock(GHCheckRun.class);
        final PagedIterable<GHCheckRun> checkrun = mock(PagedIterable.class);
        final PagedIterator<GHCheckRun> checkIterator = mock(PagedIterator.class);
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        final GitHub gh = mock(GitHub.class);

        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getScm()).thenReturn(scmLink);
        when(ctx.getUpdateCenter())
                .thenReturn(new UpdateCenter(
                        Map.of(
                                pluginName,
                                new io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin(
                                        pluginName,
                                        new VersionNumber("1.0"),
                                        scmLink,
                                        ZonedDateTime.now(),
                                        List.of(),
                                        0,
                                        "42",
                                        defaultBranch)),
                        Map.of(),
                        List.of()));
        when(ctx.getGitHub()).thenReturn(gh);
        when(ctx.getRepositoryName()).thenReturn(Optional.of(pluginRepo));
        when(ctx.getGitHub()).thenReturn(gitHub);
        when(ctx.getRepositoryName()).thenReturn(Optional.of("org/repo"));
        when(gitHub.getRepository("org/repo")).thenReturn(ghRepository);
        when(ghRepository.getCommit(defaultBranch)).thenReturn(ghCommit);
        when(gh.getRepository(pluginRepo)).thenReturn(ghRepository);
        when(ghCommit.getCheckRuns()).thenReturn(checkrun);
        when(ghCommit.getCheckRuns().iterator()).thenReturn(checkIterator);
        when(ghCommit.getCheckRuns().iterator().hasNext()).thenReturn /**/(true);
        when(ghCommit.getCheckRuns().iterator().next()).thenReturn(checkRun);
        assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.success(
                        DefaultBranchBuildStatusProbe.KEY, "Build is Success in Default Branch", probe.getVersion()));
        verify(probe).doApply(plugin, ctx);
    }

    @Test
    public void shouldReturnErrorMessageCommitsNotFound() throws IOException {

        final String pluginName = "mailer";
        final String pluginRepo = "jenkinsci/" + pluginName + "-plugin";
        final String scmLink = "https://github.com/" + pluginRepo;
        final String defaultBranch = "main";
        final GitHub gitHub = mock(GitHub.class);
        final GHRepository ghRepository = mock(GHRepository.class);
        final GHCommit ghCommit = mock(GHCommit.class);
        final PagedIterable<GHCheckRun> checkrun = mock(PagedIterable.class);
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        final GitHub gh = mock(GitHub.class);

        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getScm()).thenReturn(scmLink);
        when(ctx.getUpdateCenter())
                .thenReturn(new UpdateCenter(
                        Map.of(
                                pluginName,
                                new io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin(
                                        pluginName,
                                        new VersionNumber("1.0"),
                                        scmLink,
                                        ZonedDateTime.now(),
                                        List.of(),
                                        0,
                                        "42",
                                        defaultBranch)),
                        Map.of(),
                        List.of()));
        when(ctx.getGitHub()).thenReturn(gh);
        when(ctx.getRepositoryName()).thenReturn(Optional.of(pluginRepo));
        when(ctx.getGitHub()).thenReturn(gitHub);
        when(ctx.getRepositoryName()).thenReturn(Optional.of("org/repo"));
        when(gitHub.getRepository("org/repo")).thenReturn(ghRepository);
        when(ghRepository.getCommit(defaultBranch)).thenReturn(ghCommit);
        when(gh.getRepository(pluginRepo)).thenReturn(ghRepository);
        when(ghCommit.getCheckRuns()).thenReturn(checkrun);
        assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.error(
                        DefaultBranchBuildStatusProbe.KEY,
                        "Failed to obtain the status of the default Branch.",
                        probe.getVersion()));
        verify(probe).doApply(plugin, ctx);
    }
}
