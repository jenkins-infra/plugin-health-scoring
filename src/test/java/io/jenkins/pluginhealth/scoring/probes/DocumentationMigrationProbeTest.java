/*
 * MIT License
 *
 * Copyright (c) 2022 Jenkins Infra
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentationMigrationProbeTest {
    @Test
    public void shouldRequireRelease() {
        final DocumentationMigrationProbe probe = spy(DocumentationMigrationProbe.class);
        assertThat(probe.requiresRelease()).isTrue();
    }

    @Test
    public void shouldUseDocumentationKey() {
        final DocumentationMigrationProbe probe = spy(DocumentationMigrationProbe.class);
        assertThat(probe.key()).isEqualTo("documentation");
    }

    @Test
    public void shouldHaveDescription() {
        final DocumentationMigrationProbe probe = spy(DocumentationMigrationProbe.class);
        assertThat(probe.getDescription()).isNotBlank();
    }

    @Test
    public void shouldValidateCompletedMigrationOnMaven() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final DocumentationMigrationProbe probe = spy(DocumentationMigrationProbe.class);

        final String pluginRepositoryUrl = "this-is-the-url";
        when(plugin.getScm()).thenReturn(pluginRepositoryUrl);
        final Path repository = Files.createTempDirectory("boo");
        Files.createFile(repository.resolve("README.md"));
        final Path pom = Files.createFile(repository.resolve("pom.xml"));
        Files.write(pom, List.of(
            "<project>", "<url>", pluginRepositoryUrl, "</url>", "</project>"
        ), StandardCharsets.UTF_8);

        when(ctx.getScmRepository()).thenReturn(repository);

        final ProbeResult result = probe.apply(plugin, ctx);
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(ResultStatus.SUCCESS);
    }

    @Test
    public void shouldValidateCompletedMigrationOnGradle() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final DocumentationMigrationProbe probe = spy(DocumentationMigrationProbe.class);

        final String pluginRepositoryUrl = "this-is-the-url";
        when(plugin.getScm()).thenReturn(pluginRepositoryUrl);
        final Path repository = Files.createTempDirectory("boo");
        Files.createFile(repository.resolve("README.md"));
        final Path build = Files.createFile(repository.resolve("build.gradle"));
        Files.write(build, List.of(
            "jenkinsPlugin {", "url: ", pluginRepositoryUrl, "}"
        ), StandardCharsets.UTF_8);

        when(ctx.getScmRepository()).thenReturn(repository);

        final ProbeResult result = probe.apply(plugin, ctx);
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(ResultStatus.SUCCESS);
    }

    @Test
    public void shouldInvalidateRepositoryWithMissingREADME() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final DocumentationMigrationProbe probe = new DocumentationMigrationProbe();

        final String pluginRepositoryUrl = "this-is-the-url";
        final Path repository = Files.createTempDirectory("boo");
        final Path pom = Files.createFile(repository.resolve("pom.xml"));
        Files.write(pom, List.of(
            "<project>", "<url>", pluginRepositoryUrl, "</url>", "</project>"
        ), StandardCharsets.UTF_8);

        when(ctx.getScmRepository()).thenReturn(repository);

        final ProbeResult result = probe.apply(plugin, ctx);
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(ResultStatus.FAILURE);
        assertThat(result.message()).isEqualTo("The plugin has no README");
    }

    @Test
    public void shouldInvalidateRepositoryWithMissingUrl() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final DocumentationMigrationProbe probe = spy(DocumentationMigrationProbe.class);

        final String pluginRepositoryUrl = "this-is-the-url";
        when(plugin.getScm()).thenReturn(pluginRepositoryUrl);

        final Path repository = Files.createTempDirectory("boo");
        Files.createFile(repository.resolve("README.md"));
        final Path pom = Files.createFile(repository.resolve("pom.xml"));
        Files.write(pom, List.of(
            "<project>", "</project>"
        ), StandardCharsets.UTF_8);
        when(ctx.getScmRepository()).thenReturn(repository);

        final ProbeResult result = probe.apply(plugin, ctx);
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(ResultStatus.FAILURE);
        assertThat(result.message()).isEqualTo("The plugin documentation was not migrated");
    }

    @Test
    public void shouldValidateRepositoryWithURLDirectedToBranch() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final DocumentationMigrationProbe probe = spy(DocumentationMigrationProbe.class);

        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/one-plugin");

        final Path repository = Files.createTempDirectory("one-plugin");
        Files.createFile(repository.resolve("README.md"));
        final Path pom = Files.createFile(repository.resolve("pom.xml"));
        Files.write(pom, List.of(
            "<project>", "<url>", "https://github.com/jenkinsci/one-plugin/tree/main", "</url>", "</project>"
        ), StandardCharsets.UTF_8);
        when(ctx.getScmRepository()).thenReturn(repository);

        final ProbeResult result = probe.apply(plugin, ctx);
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(ResultStatus.SUCCESS);
    }

    @Test
    public void shouldValidateRepositoryWithURLDirectedToReadmeFile() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final DocumentationMigrationProbe probe = spy(DocumentationMigrationProbe.class);

        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/one-plugin");

        final Path repository = Files.createTempDirectory("one-plugin");
        Files.createFile(repository.resolve("README.md"));
        final Path pom = Files.createFile(repository.resolve("pom.xml"));
        Files.write(pom, List.of(
            "<project>", "<url>", "https://github.com/jenkinsci/one-plugin/blob/main/README.md", "</url>", "</project>"
        ), StandardCharsets.UTF_8);
        when(ctx.getScmRepository()).thenReturn(repository);

        final ProbeResult result = probe.apply(plugin, ctx);
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(ResultStatus.SUCCESS);
    }
}
