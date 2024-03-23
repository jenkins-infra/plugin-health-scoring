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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JSR305ProbeTest extends AbstractProbeTest<JSR305Probe> {
    private Plugin plugin;
    private ProbeContext ctx;
    private JSR305Probe probe;

    @BeforeEach
    public void init() {
        plugin = mock(Plugin.class);
        ctx = mock(ProbeContext.class);
        probe = getSpy();
    }

    @Override
    JSR305Probe getSpy() {
        return spy(JSR305Probe.class);
    }

    @Test
    void shouldReturnPluginsThatUseDeprecatedAnnotations() throws IOException {
        final Path repo = Files.createTempDirectory("foo");
        Path directory = Files.createDirectories(repo.resolve("src/main/java"));
        final Path javaFileWithAllDeprecatedAnnotation = Files.createFile(directory.resolve("test-class-1.java"));
        final Path javaFileWithNonnullDeprecatedAnnotation = Files.createFile(directory.resolve("test-class-2.java"));
        final Path javaFileWithCheckForNullDeprecatedAnnotation =
                Files.createFile(directory.resolve("test-class-3.java"));
        final Path javaFileWithNoDeprecatedAnnotation = Files.createFile(directory.resolve("test-class-4.java"));
        final Path txtFileShouldNotBeFound = Files.createFile(directory.resolve("file.txt"));
        Files.createFile(directory.resolve("test-dummy-class-should-not-be-returned.java"));

        Files.write(
                javaFileWithAllDeprecatedAnnotation,
                List.of(
                        "package test;",
                        "",
                        "import javax.annotation.Nonnull;",
                        "import javax.annotation.CheckForNull;",
                        "",
                        "import java.util.HashMap;",
                        "import java.util.Map;"));

        Files.write(
                javaFileWithNonnullDeprecatedAnnotation,
                List.of("package test;", "", "import javax.annotation.Nonnull;"));

        Files.write(
                javaFileWithCheckForNullDeprecatedAnnotation,
                List.of("package test;", "", "import javax.annotation.CheckForNull;"));

        Files.write(javaFileWithNoDeprecatedAnnotation, List.of("package test;"));

        Files.write(txtFileShouldNotBeFound, List.of("This-file-should-not-returned."));

        when(ctx.getScmRepository()).thenReturn(Optional.of(repo));
        when(plugin.getName()).thenReturn("foo");

        assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.success(
                        JSR305Probe.KEY,
                        "Deprecated imports found at foo plugin for test-class-1.java, test-class-2.java, test-class-3.java",
                        probe.getVersion()));
        verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));
    }

    @Test
    void shouldNotReturnPluginsWithNoDeprecatedImports() throws IOException {
        final Path repo = Files.createTempDirectory("foo");
        Path directory = Files.createDirectories(repo.resolve("src/main/java"));
        final Path javaFileWithUpdatedAnnotation = Files.createFile(directory.resolve("test-class-3.java"));
        Files.createFile(directory.resolve("test-dummy-class-should-not-be-returned.java"));

        Files.write(
                javaFileWithUpdatedAnnotation,
                List.of(
                        "package test;",
                        "",
                        "import edu.umd.cs.findbugs.annotations.NonNull;",
                        "import edu.umd.cs.findbugs.annotations.CheckForNull;",
                        "",
                        "import java.util.HashMap;",
                        "import java.util.Map;"));

        when(ctx.getScmRepository()).thenReturn(Optional.of(repo));
        when(plugin.getName()).thenReturn("foo");

        assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.success(
                        JSR305Probe.KEY, "Latest version of imports found at foo plugin.", probe.getVersion()));
        verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));
    }
}
