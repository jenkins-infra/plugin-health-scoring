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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHCommitStatus;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

class DefaultBranchBuildStatusProbeTest extends AbstractProbeTest<DefaultBranchBuildStatusProbe> {
    @Override
    DefaultBranchBuildStatusProbe getSpy() {
        return spy(DefaultBranchBuildStatusProbe.class);
    }

    @Test
    public void shouldReturnBuildSuccessOnTheDefaultBranch() throws IOException {
        final DefaultBranchBuildStatusProbe probe = getSpy();

        final String pluginName = "mailer";
        final String pluginRepo = "jenkinsci/" + pluginName + "-plugin";
        final String scmLink = "https://github.com/" + pluginRepo;
        final String defaultBranch = "main";

        final GitHub gitHub = mock(GitHub.class);
        final GHRepository ghRepository = mock(GHRepository.class);
        final GHCommitStatus commitStatus = mock(GHCommitStatus.class);
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

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
        when(ctx.getGitHub()).thenReturn(gitHub);
        when(ctx.getRepositoryName()).thenReturn(Optional.of(pluginRepo));
        when(gitHub.getRepository(pluginRepo)).thenReturn(ghRepository);
        when(ghRepository.getLastCommitStatus(defaultBranch)).thenReturn(commitStatus);
        when(commitStatus.getState()).thenReturn(GHCommitState.SUCCESS);

        assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.success(DefaultBranchBuildStatusProbe.KEY, "SUCCESS", probe.getVersion()));
    }

    @Test
    public void shouldCorrectlyDetectsPendingBuild() throws IOException {
        final DefaultBranchBuildStatusProbe probe = getSpy();

        final String pluginName = "mailer";
        final String pluginRepo = "jenkinsci/" + pluginName + "-plugin";
        final String scmLink = "https://github.com/" + pluginRepo;
        final String defaultBranch = "main";

        final GitHub gitHub = mock(GitHub.class);
        final GHRepository ghRepository = mock(GHRepository.class);
        final GHCommitStatus commitStatus = mock(GHCommitStatus.class);
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

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
        when(ctx.getGitHub()).thenReturn(gitHub);
        when(ctx.getRepositoryName()).thenReturn(Optional.of(pluginRepo));
        when(gitHub.getRepository(pluginRepo)).thenReturn(ghRepository);
        when(ghRepository.getLastCommitStatus(defaultBranch)).thenReturn(commitStatus);
        when(commitStatus.getState()).thenReturn(GHCommitState.PENDING);

        assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.success(DefaultBranchBuildStatusProbe.KEY, "PENDING", probe.getVersion()));
    }

    @Test
    public void shouldReturnErrorWhenNotStatus() throws IOException {
        final DefaultBranchBuildStatusProbe probe = getSpy();

        final String pluginName = "mailer";
        final String pluginRepo = "jenkinsci/" + pluginName + "-plugin";
        final String scmLink = "https://github.com/" + pluginRepo;
        final String defaultBranch = "main";

        final GitHub gitHub = mock(GitHub.class);
        final GHRepository ghRepository = mock(GHRepository.class);
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

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
        when(ctx.getGitHub()).thenReturn(gitHub);
        when(ctx.getRepositoryName()).thenReturn(Optional.of(pluginRepo));
        when(gitHub.getRepository(pluginRepo)).thenReturn(ghRepository);
        when(ghRepository.getLastCommitStatus(defaultBranch)).thenReturn(null);

        assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.error(
                        DefaultBranchBuildStatusProbe.KEY,
                        "There is no last commit status found for the plugin.",
                        probe.getVersion()));
    }
}
