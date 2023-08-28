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
import java.util.Map;

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

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, "")
        ));
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
        final Path javaFileWithCheckForNullDeprecatedAnnotation = Files.createFile(directory.resolve("test-class-3.java"));
        final Path javaFileWithNoDeprecatedAnnotation = Files.createFile(directory.resolve("test-class-4.java"));
        final Path txtFileShouldNotBeFound = Files.createFile(directory.resolve("file.txt"));
        Files.createFile(directory.resolve("test-dummy-class-should-not-be-returned.java"));

        Files.write(javaFileWithAllDeprecatedAnnotation, List.of(
            "package test;",
            "",
            "import javax.annotation.Nonnull;",
            "import javax.annotation.CheckForNull;"
        ));

        Files.write(javaFileWithNonnullDeprecatedAnnotation, List.of(
            "package test;",
            "",
            "import javax.annotation.Nonnull;"
        ));

        Files.write(javaFileWithCheckForNullDeprecatedAnnotation, List.of(
            "package test;",
            "",
            "import javax.annotation.CheckForNull;"
        ));

        Files.write(javaFileWithNoDeprecatedAnnotation, List.of(
            "package test;"
        ));

        Files.write(txtFileShouldNotBeFound, List.of(
            "This-file-should-not-returned."
        ));

        when(ctx.getScmRepository()).thenReturn(repo);
        when(plugin.getName()).thenReturn("foo");

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.failure(JSR305Probe.KEY, "Deprecated imports found at foo plugin for test-class-1.java, test-class-2.java, test-class-3.java"));
        verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));

    }

    @Test
    void shouldNotReturnPluginsWithUpdatedAnnotations() throws IOException {
        final Path repo = Files.createTempDirectory("foo");
        Path directory = Files.createDirectories(repo.resolve("src/main/java"));
        final Path javaFileWithUpdatedAnnotation = Files.createFile(directory.resolve("test-class-3.java"));
        Files.createFile(directory.resolve("test-dummy-class-should-not-be-returned.java"));

        Files.write(javaFileWithUpdatedAnnotation, List.of(
            "package test;"
        ));

        when(ctx.getScmRepository()).thenReturn(repo);
        when(plugin.getName()).thenReturn("foo");

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.success(JSR305Probe.KEY, "Latest version of imports found at foo plugin."));
        verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));
    }


}
