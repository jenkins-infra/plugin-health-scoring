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
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;

import hudson.util.VersionNumber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CodeCoverageProbeTest {
    @Test
    public void shouldHaveSpecificKey() {
        assertThat(spy(CodeCoverageProbe.class).key()).isEqualTo(CodeCoverageProbe.KEY);
    }

    @Test
    public void shouldHaveDescription() {
        assertThat(spy(CodeCoverageProbe.class).getDescription()).isNotBlank();
    }

    @Test
    public void shouldNotRequireRelease() {
        assertThat(spy(CodeCoverageProbe.class).requiresRelease()).isFalse();
    }

    @Test
    public void shouldBeRelatedToCode() {
        assertThat(spy(CodeCoverageProbe.class).isSourceCodeRelated()).isTrue();
    }

    @Test
    public void shouldRequireJenkinsfile() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getDetails()).thenReturn(Map.of(
            JenkinsfileProbe.KEY, ProbeResult.failure(JenkinsfileProbe.KEY, "")
        ));

        final CodeCoverageProbe probe = new CodeCoverageProbe();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result)
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.error(CodeCoverageProbe.KEY, "Requires Jenkinsfile"));
    }

    @Test
    public void shouldBeAbleToRetrieveDetailsFromGitHubChecks() throws IOException {
        final String pluginName = "mailer";
        final String scmLink = "https://github.com/jenkinsci/mailer-plugin";
        final String defaultBranch = "main";

        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        final GitHub gh = mock(GitHub.class);
        final GHRepository ghRepository = mock(GHRepository.class);

        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getDetails()).thenReturn(Map.of(
            JenkinsfileProbe.KEY, ProbeResult.success(JenkinsfileProbe.KEY, "")
        ));
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
        when(ctx.getRepositoryName(plugin.getScm())).thenReturn(Optional.of("jenkinsci/mailer-plugin"));

        when(gh.getRepository(anyString())).thenReturn(ghRepository);
        when(ghRepository.getCheckRuns(defaultBranch)).thenReturn(null);

        final CodeCoverageProbe probe = new CodeCoverageProbe();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result)
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.error(CodeCoverageProbe.KEY, "Requires Jenkinsfile"));
    }
}
