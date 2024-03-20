/*
 * MIT License
 *
 * Copyright (c) 2022-2024 Jenkins Infra
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.junit.jupiter.api.Test;

public class ContributingGuidelinesProbeTest extends AbstractProbeTest<ContributingGuidelinesProbe> {
    @Override
    ContributingGuidelinesProbe getSpy() {
        return spy(ContributingGuidelinesProbe.class);
    }

    @Test
    public void shouldNotRequireRelease() {
        assertThat(getSpy().requiresRelease()).isFalse();
    }

    @Test
    public void shouldExecuteOnSourceCodeChange() {
        assertThat(getSpy().isSourceCodeRelated()).isTrue();
    }

    @Test
    public void shouldCorrectlyDetectMissingContributingGuidelines() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final ContributingGuidelinesProbe probe = getSpy();

        when(plugin.getName()).thenReturn("foo");
        final Path repository = Files.createTempDirectory(plugin.getName());
        when(ctx.getScmRepository()).thenReturn(Optional.of(repository));

        assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.success(
                        ContributingGuidelinesProbe.KEY,
                        "Inherit from organization contributing guide.",
                        probe.getVersion()));
    }

    @Test
    public void shouldCorrectlyDetectContributingGuidelinesInRootLevelOfRepository() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final ContributingGuidelinesProbe probe = getSpy();

        when(plugin.getName()).thenReturn("foo");
        final Path repository = Files.createTempDirectory(plugin.getName());
        final Path guide = Files.createFile(repository.resolve("CONTRIBUTING.md"));
        Files.writeString(guide, """
            Everything is awesome.
            """);
        when(ctx.getScmRepository()).thenReturn(Optional.of(repository));

        assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.success(
                        ContributingGuidelinesProbe.KEY, "Contributing guidelines found.", probe.getVersion()));
    }

    @Test
    public void shouldCorrectlyDetectContributingGuidelinesInDocsFolder() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final ContributingGuidelinesProbe probe = getSpy();

        when(plugin.getName()).thenReturn("foo");
        final Path repository = Files.createTempDirectory(plugin.getName());
        final Path guide = Files.createFile(
                Files.createDirectory(repository.resolve("docs")).resolve("CONTRIBUTING.md"));
        Files.writeString(guide, """
            Everything is awesome.
            """);
        when(ctx.getScmRepository()).thenReturn(Optional.of(repository));

        assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.success(
                        ContributingGuidelinesProbe.KEY, "Contributing guidelines found.", probe.getVersion()));
    }

    @Test
    public void shouldDetectEmptyContributingGuidelines() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final ContributingGuidelinesProbe probe = getSpy();

        when(plugin.getName()).thenReturn("foo");
        final Path repository = Files.createTempDirectory(plugin.getName());
        Files.createFile(Files.createDirectory(repository.resolve("docs")).resolve("CONTRIBUTING.md"));
        when(ctx.getScmRepository()).thenReturn(Optional.of(repository));

        assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.success(
                        ContributingGuidelinesProbe.KEY, "Contributing guide seems to be empty.", probe.getVersion()));
    }
}
