/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Jenkins Infra
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

class RepositoryArchivedStatusProbeTest extends AbstractProbeTest<RepositoryArchivedStatusProbe> {

    @Override
    RepositoryArchivedStatusProbe getSpy() {
        return spy(RepositoryArchivedStatusProbe.class);
    }

    @Test
    void shouldNotRequireRelease() {
        assertFalse(getSpy().requiresRelease());
    }

    @Test
    void shouldNotBeRelatedToSourceCode() {
        assertFalse(getSpy().requiresRelease());
    }

    @Test
    void shouldFailWithNoSCM() {
        final Plugin pl = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        final RepositoryArchivedStatusProbe probe = getSpy();

        assertThat(probe.apply(pl, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.error(
                        RepositoryArchivedStatusProbe.KEY,
                        "Plugin SCM is unknown, cannot fetch the number of open pull requests.",
                        1));
    }

    @Test
    void shouldFailWithNoRepositoryName() {
        final Plugin pl = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(pl.getName()).thenReturn("_test_");
        when(pl.getScm()).thenReturn("valid-url");
        when(ctx.getRepositoryName()).thenReturn(Optional.empty());

        final RepositoryArchivedStatusProbe probe = getSpy();

        assertThat(probe.apply(pl, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(
                        ProbeResult.error(RepositoryArchivedStatusProbe.KEY, "Cannot find repository for _test_", 1));
    }

    @Test
    void shouldSucceedToFindArchivedRepository() throws Exception {
        final Plugin pl = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final GitHub gh = mock(GitHub.class);
        final GHRepository ghRepository = mock(GHRepository.class);

        when(pl.getName()).thenReturn("_test_");
        when(pl.getScm()).thenReturn("valid-url");
        when(ctx.getRepositoryName()).thenReturn(Optional.of("jenkinsci/_test_"));

        when(ctx.getGitHub()).thenReturn(gh);
        when(gh.getRepository(anyString())).thenReturn(ghRepository);

        when(ghRepository.isArchived()).thenReturn(true);

        final RepositoryArchivedStatusProbe probe = getSpy();

        assertThat(probe.apply(pl, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.success(RepositoryArchivedStatusProbe.KEY, true, 1));
    }

    @Test
    void shouldSucceedToFindNotArchivedRepository() throws Exception {
        final Plugin pl = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final GitHub gh = mock(GitHub.class);
        final GHRepository ghRepository = mock(GHRepository.class);

        when(pl.getName()).thenReturn("_test_");
        when(pl.getScm()).thenReturn("valid-url");
        when(ctx.getRepositoryName()).thenReturn(Optional.of("jenkinsci/_test_"));

        when(ctx.getGitHub()).thenReturn(gh);
        when(gh.getRepository(anyString())).thenReturn(ghRepository);

        when(ghRepository.isArchived()).thenReturn(false);

        final RepositoryArchivedStatusProbe probe = getSpy();

        assertThat(probe.apply(pl, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.success(RepositoryArchivedStatusProbe.KEY, false, 1));
    }
}
