/*
 * MIT License
 *
 * Copyright (c) 2024 Jenkins Infra
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

public class CodeOwnershipProbeTest extends AbstractProbeTest<CodeOwnershipProbe> {
    @Override
    CodeOwnershipProbe getSpy() {
        return spy(CodeOwnershipProbe.class);
    }

    @Test
    void shouldDetectMissingCodeOwnershipFile() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        final Path repo = Files.createTempDirectory(getClass().getName());
        when(ctx.getScmRepository()).thenReturn(Optional.of(repo));

        final CodeOwnershipProbe probe = getSpy();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result)
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.success(probe.key(), "No CODEOWNERS file found in plugin repository.", 0));
    }

    @Test
    void shouldDetectCodeOwnershipFileWithInvalidContent() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getName()).thenReturn("new-super");

        {
            final Path repo = Files.createTempDirectory(getClass().getName());
            final Path codeowners = Files.createFile(repo.resolve("CODEOWNERS"));
            Files.writeString(
                    codeowners, """
                * @jenkinsci/sample-plugin-developers
                """);
            when(ctx.getScmRepository()).thenReturn(Optional.of(repo));
        }
        {
            final Path repo = Files.createTempDirectory(getClass().getName());
            final Path github = Files.createDirectory(repo.resolve(".github"));
            final Path codeowners = Files.createFile(github.resolve("CODEOWNERS"));
            Files.writeString(
                    codeowners, """
                * @jenkinsci/sample-plugin-developers
                """);
            when(ctx.getScmRepository()).thenReturn(Optional.of(repo));
        }
        {
            final Path repo = Files.createTempDirectory(getClass().getName());
            final Path docs = Files.createDirectory(repo.resolve("docs"));
            final Path codeowners = Files.createFile(docs.resolve("CODEOWNERS"));
            Files.writeString(
                    codeowners, """
                * @jenkinsci/sample-plugin-developers
                """);
            when(ctx.getScmRepository()).thenReturn(Optional.of(repo));
        }

        final CodeOwnershipProbe probe = getSpy();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result)
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.success(probe.key(), "CODEOWNERS file is not set correctly.", 0));
        assertThat(result)
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.success(probe.key(), "CODEOWNERS file is not set correctly.", 0));
        assertThat(result)
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.success(probe.key(), "CODEOWNERS file is not set correctly.", 0));
    }

    @Test
    void shouldDetectCodeOwnershipFileWithValidTeam() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getName()).thenReturn("sample");

        {
            final Path repo = Files.createTempDirectory(getClass().getName());
            final Path codeowners = Files.createFile(repo.resolve("CODEOWNERS"));
            Files.writeString(
                    codeowners, """
                * @jenkinsci/sample-plugin-developers
                """);
            when(ctx.getScmRepository()).thenReturn(Optional.of(repo));
        }
        {
            final Path repo = Files.createTempDirectory(getClass().getName());
            final Path github = Files.createDirectory(repo.resolve(".github"));
            final Path codeowners = Files.createFile(github.resolve("CODEOWNERS"));
            Files.writeString(
                    codeowners, """
                * @jenkinsci/sample-plugin-developers
                """);
            when(ctx.getScmRepository()).thenReturn(Optional.of(repo));
        }
        {
            final Path repo = Files.createTempDirectory(getClass().getName());
            final Path docs = Files.createDirectory(repo.resolve("docs"));
            final Path codeowners = Files.createFile(docs.resolve("CODEOWNERS"));
            Files.writeString(
                    codeowners, """
                * @jenkinsci/sample-plugin-developers
                """);
            when(ctx.getScmRepository()).thenReturn(Optional.of(repo));
        }

        final CodeOwnershipProbe probe = getSpy();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result)
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.success(probe.key(), "CODEOWNERS file is valid.", 0));
        assertThat(result)
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.success(probe.key(), "CODEOWNERS file is valid.", 0));
        assertThat(result)
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.success(probe.key(), "CODEOWNERS file is valid.", 0));
    }
}
