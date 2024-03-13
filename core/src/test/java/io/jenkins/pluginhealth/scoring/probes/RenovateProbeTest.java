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
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.junit.jupiter.api.Test;

public class RenovateProbeTest extends AbstractProbeTest<RenovateProbe> {
    @Override
    RenovateProbe getSpy() {
        return spy(RenovateProbe.class);
    }

    @Test
    void shouldNotRequireRelease() {
        assertThat(getSpy().requiresRelease()).isFalse();
    }

    @Test
    void shouldBeRelatedToCode() {
        assertThat(getSpy().isSourceCodeRelated()).isTrue();
    }

    @Test
    void shouldSurvivePluginWithoutLocalRepository() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getName()).thenReturn("foo");
        when(ctx.getScmRepository()).thenReturn(Optional.empty());

        final RenovateProbe probe = getSpy();
        assertThat(probe.apply(plugin, ctx))
                .isNotNull()
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.error(
                        RenovateProbe.KEY,
                        "There is no local repository for plugin " + plugin.getName() + ".",
                        probe.getVersion()));
    }

    @Test
    void shouldDetectMissingGitHubActionFolder() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final RenovateProbe probe = getSpy();

        final Path repo = Files.createTempDirectory("foo");
        when(ctx.getScmRepository()).thenReturn(Optional.of(repo));

        assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.success(
                        RenovateProbe.KEY, "No GitHub configuration folder found.", probe.getVersion()));
    }

    @Test
    void shouldDetectMissingRenovateFile() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final RenovateProbe probe = getSpy();

        final Path repo = Files.createTempDirectory("foo");
        Files.createDirectory(repo.resolve(".github"));
        when(ctx.getScmRepository()).thenReturn(Optional.of(repo));

        assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.success(RenovateProbe.KEY, "Renovate is not configured.", probe.getVersion()));
    }

    @Test
    void shouldDetectRenovateFile() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final RenovateProbe probe = getSpy();

        final Path repo = Files.createTempDirectory("foo");
        final Path github = Files.createDirectories(repo.resolve(".github"));

        Files.createFile(github.resolve("renovate.json"));
        when(ctx.getScmRepository()).thenReturn(Optional.of(repo));

        assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.success(RenovateProbe.KEY, "Renovate is configured.", probe.getVersion()));
    }
}
