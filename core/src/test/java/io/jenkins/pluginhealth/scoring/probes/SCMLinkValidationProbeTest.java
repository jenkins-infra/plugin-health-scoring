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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

class SCMLinkValidationProbeTest extends AbstractProbeTest<SCMLinkValidationProbe> {
    @Override
    SCMLinkValidationProbe getSpy() {
        return spy(SCMLinkValidationProbe.class);
    }

    @Test
    void shouldRequireRelease() {
        assertThat(getSpy().requiresRelease()).isTrue();
    }

    @Test
    void shouldNotAcceptNullNorEmptyScm() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getScm()).thenReturn(
            null, ""
        );
        final SCMLinkValidationProbe probe = getSpy();

        assertThat(probe.apply(plugin, ctx))
            .isNotNull()
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.error(SCMLinkValidationProbe.KEY, "The plugin SCM link is empty."));

        assertThat(probe.apply(plugin, ctx))
            .isNotNull()
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.error(SCMLinkValidationProbe.KEY, "The plugin SCM link is empty."));
    }

    @Test
    void shouldRecognizeIncorrectSCMUrl() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final SCMLinkValidationProbe probe = getSpy();

        when(plugin.getScm()).thenReturn("this-is-not-correct");

        assertThat(probe.apply(plugin, ctx))
            .isNotNull()
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.error(SCMLinkValidationProbe.KEY, "SCM link doesn't match GitHub plugin repositories."));
    }

    @Test
    void shouldRecognizeCorrectGitHubUrl() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final GitHub gh = mock(GitHub.class);
        final String repositoryName = "jenkinsci/test-repo";

        when(plugin.getName()).thenReturn("test-repo");
        when(plugin.getScm()).thenReturn("https://github.com/" + repositoryName);
        when(ctx.getScmRepository()).thenReturn(Optional.of(
            Path.of("src/test/resources/jenkinsci/test-repo/test-nested-dir-1/test-nested-dir-2")
        ));
        when(ctx.getGitHub()).thenReturn(gh);
        when(gh.getRepository(repositoryName)).thenReturn(new GHRepository());

        final SCMLinkValidationProbe probe = getSpy();

        assertThat(probe.apply(plugin, ctx))
            .isNotNull()
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(SCMLinkValidationProbe.KEY, "The plugin SCM link is valid."));
    }

    @Test
    void shouldRecognizeInvalidGitHubUrl() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final GitHub gh = mock(GitHub.class);
        final String repositoryName = "jenkinsci/this-is-not-going-to-work";

        when(plugin.getScm()).thenReturn("https://github.com/" + repositoryName);
        when(plugin.getName()).thenReturn("foo");

        when(ctx.getScmRepository()).thenReturn(Optional.of(Files.createTempDirectory("foo")));
        when(ctx.getGitHub()).thenReturn(gh);
        when(gh.getRepository(repositoryName)).thenThrow(IOException.class);

        final SCMLinkValidationProbe probe = getSpy();

        assertThat(probe.apply(plugin, ctx))
            .isNotNull()
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success("scm", "The plugin SCM link is invalid."));
    }

    @Test
    void shouldBeAbleToFindPluginInModule() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        final GitHub gh = mock(GitHub.class);
        final GHRepository ghRepo = mock(GHRepository.class);

        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/this-is-fine");
        when(plugin.getName()).thenReturn("test-repo");

        when(ctx.getScmRepository()).thenReturn(Optional.of(
            Path.of("src/test/resources/jenkinsci/test-repo/test-nested-dir-1")
        ));
        when(ctx.getGitHub()).thenReturn(gh);
        when(gh.getRepository("jenkinsci/this-is-fine")).thenReturn(ghRepo);

        final SCMLinkValidationProbe probe = getSpy();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result)
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.success(probe.key(), "The plugin SCM link is valid."));

        verify(ctx).setScmFolderPath(Path.of("test-nested-dir-2"));
    }
}
