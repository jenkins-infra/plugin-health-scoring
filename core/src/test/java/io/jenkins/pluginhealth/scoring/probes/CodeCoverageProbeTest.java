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
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;

class CodeCoverageProbeTest extends AbstractProbeTest<CodeCoverageProbe> {
    @Override
    CodeCoverageProbe getSpy() {
        return spy(CodeCoverageProbe.class);
    }

    @Test
    public void shouldNotRequireRelease() {
        assertThat(getSpy().requiresRelease()).isFalse();
    }

    @Test
    public void shouldBeRelatedToCode() {
        assertThat(getSpy().isSourceCodeRelated()).isTrue();
    }

    @Test
    public void shouldBeInErrorWhenRepositoryIsNotInOrganization() {
        final String pluginName = "foo";
        final String scmLink = "foo-bar";

        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getScm()).thenReturn(scmLink);

        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of(
                pluginName, new io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin(
                    pluginName, new VersionNumber("1.0"), scmLink, ZonedDateTime.now(), List.of(), 0,
                    "42", "main"
                )
            ),
            Map.of(),
            List.of()
        ));
        when(ctx.getScmRepository()).thenReturn(Optional.empty());
        when(ctx.getRepositoryName()).thenReturn(Optional.empty());

        final CodeCoverageProbe probe = getSpy();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result)
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.error(CodeCoverageProbe.KEY, "Cannot determine plugin repository."));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSucceedToRetrieveCoverageData() throws IOException {
        final String pluginName = "mailer";
        final String pluginRepo = "jenkinsci/" + pluginName + "-plugin";
        final String scmLink = "https://github.com/" + pluginRepo;
        final String defaultBranch = "main";

        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        final GitHub gh = mock(GitHub.class);
        final GHRepository ghRepository = mock(GHRepository.class);

        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getScm()).thenReturn(scmLink);
        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of(
                pluginName, new io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin(
                    pluginName, new VersionNumber("1.0"), scmLink, ZonedDateTime.now(), List.of(), 0,
                    "42", defaultBranch
                )
            ),
            Map.of(),
            List.of()
        ));
        when(ctx.getGitHub()).thenReturn(gh);
        when(ctx.getRepositoryName()).thenReturn(Optional.of(pluginRepo));

        when(gh.getRepository(pluginRepo)).thenReturn(ghRepository);

        final PagedIterable<GHCheckRun> checkRuns = (PagedIterable<GHCheckRun>) mock(PagedIterable.class);
        final GHCheckRun checkRun = mock(GHCheckRun.class);
        final GHCheckRun.Output output = mock(GHCheckRun.Output.class);
        when(output.getTitle()).thenReturn("Line Coverage: 70.56% (+0.00%), Branch Coverage: 63.37% (+0.00%)");
        when(checkRun.getOutput()).thenReturn(output);
        when(checkRuns.toList()).thenReturn(
            List.of(checkRun)
        );
        when(ghRepository.getCheckRuns(defaultBranch, Map.of("check_name", "Code Coverage"))).thenReturn(checkRuns);

        final CodeCoverageProbe probe = getSpy();
        final ProbeResult result = probe.apply(plugin, ctx);

        verify(probe).doApply(plugin, ctx);
        assertThat(result)
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(CodeCoverageProbe.KEY, "Line coverage: 70.56%. Branch coverage: 63.37%."));
    }
}
