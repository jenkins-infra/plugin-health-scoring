package io.jenkins.pluginhealth.scoring.probes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IncrementalBuildDetectionProbeTest extends AbstractProbeTest<IncrementalBuildDetectionProbe> {
    private Plugin plugin;
    private ProbeContext ctx;
    private IncrementalBuildDetectionProbe probe;

    @BeforeEach
    public void init() {
        plugin = mock(Plugin.class);
        ctx = mock(ProbeContext.class);
        probe = getSpy();
    }

    @Override
    IncrementalBuildDetectionProbe getSpy() {
        return spy(IncrementalBuildDetectionProbe.class);
    }

    @Test
    void shouldReturnASuccessfulCheckWhenIncrementalBuildConfiguredInBothFiles() {
        when(ctx.getScmRepository()).thenReturn(Optional.of(Path.of("src/test/resources/jenkinsci/plugin-repo-with-correct-configuration")));
        when(plugin.getName()).thenReturn("foo");
        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.success(IncrementalBuildDetectionProbe.KEY, "Incremental Build is configured in the foo plugin.", probe.getVersion()));
        verify(probe).doApply(plugin, ctx);
    }

    @Test
    void shouldReturnFailureWhenIncrementalBuildIsConfiguredOnlyInExtensionsXML() {
        when(ctx.getScmRepository()).thenReturn(Optional.of(Path.of("src/test/resources/jenkinsci/plugin-repo-with-missing-maven-config-file")));
        when(plugin.getName()).thenReturn("foo");
        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.success(IncrementalBuildDetectionProbe.KEY, "Incremental Build is not configured in the foo plugin.", probe.getVersion()));
        verify(probe).doApply(plugin, ctx);
    }

    @Test
    void shouldReturnFailureWhenIncrementalBuildIsConfiguredOnlyInMavenConfig() {
        when(ctx.getScmRepository()).thenReturn(Optional.of(Path.of("src/test/resources/jenkinsci/plugin-repo-with-missing-extensions-file")));
        when(plugin.getName()).thenReturn("foo");
        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.success(IncrementalBuildDetectionProbe.KEY, "Incremental Build is not configured in the foo plugin.", probe.getVersion()));
        verify(probe).doApply(plugin, ctx);
    }

    @Test
    void shouldFailWhenIncrementalBuildIsIncorrectlyConfiguredInBothFiles() {
        when(ctx.getScmRepository()).thenReturn(Optional.of(Path.of("src/test/resources/jenkinsci/plugin-repo-with-incorrect-configuration-lines-in-both-files")));
        when(plugin.getName()).thenReturn("foo");
        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.success(IncrementalBuildDetectionProbe.KEY, "Incremental Build is not configured in the foo plugin.", probe.getVersion()));
        verify(probe).doApply(plugin, ctx);
    }

    @Test
    void shouldFailWhenIncrementalBuildIsIncorrectlyConfiguredInExtensionsXML() {
        when(ctx.getScmRepository()).thenReturn(Optional.of(Path.of("src/test/resources/jenkinsci/test-plugin-incorrect-extensions-configuration")));
        when(plugin.getName()).thenReturn("foo");
        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.success(IncrementalBuildDetectionProbe.KEY, "Incremental Build is not configured in the foo plugin.", probe.getVersion()));
        verify(probe).doApply(plugin, ctx);
    }

    @Test
    void shouldFailWhenIncrementalBuildLinesAreIncorrectInMavenConfig() {
        when(ctx.getScmRepository()).thenReturn(Optional.of(Path.of("src/test/resources/jenkinsci/test-plugin-incorrect-maven-configuration")));
        when(plugin.getName()).thenReturn("foo");
        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.success(IncrementalBuildDetectionProbe.KEY, "Incremental Build is not configured in the foo plugin.", probe.getVersion()));
        verify(probe).doApply(plugin, ctx);
    }

    @Test
    void shouldFailWhenIncrementalBuildLinesAreMissingInMavenConfig() {
        when(ctx.getScmRepository()).thenReturn(Optional.of(Path.of("src/test/resources/jenkinsci/test-plugin-with-missing-lines-maven-configuration")));
        when(plugin.getName()).thenReturn("foo");
        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.success(IncrementalBuildDetectionProbe.KEY, "Incremental Build is not configured in the foo plugin.", probe.getVersion()));
        verify(probe).doApply(plugin, ctx);
    }

    @Test
    void shouldFailWhenMavenFolderIsNotFound() {
        when(ctx.getScmRepository()).thenReturn(Optional.of(Path.of("src/test/resources/jenkinsci/test-repo-without-mvn-should-not-be-found")));
        when(plugin.getName()).thenReturn("foo");
        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.error(IncrementalBuildDetectionProbe.KEY, "Could not find Maven configuration folder for the foo plugin.", probe.getVersion()));
        verify(probe).doApply(plugin, ctx);
    }
}
