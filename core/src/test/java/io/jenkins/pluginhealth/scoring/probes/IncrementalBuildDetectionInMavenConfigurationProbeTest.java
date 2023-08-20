package io.jenkins.pluginhealth.scoring.probes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IncrementalBuildDetectionInMavenConfigurationProbeTest extends AbstractProbeTest<IncrementalBuildDetectionInMavenConfigurationProbe> {
    private Plugin plugin;
    private ProbeContext ctx;
    private IncrementalBuildDetectionInMavenConfigurationProbe probe;

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
    IncrementalBuildDetectionInMavenConfigurationProbe getSpy() {
        return spy(IncrementalBuildDetectionInMavenConfigurationProbe.class);
    }

    @Test
    void shouldReturnASuccessfulCheckIncrementalBuildConfiguredInBothFiles() {
        when(ctx.getScmRepository()).thenReturn(Path.of("src/test/resources/jenkinsci/test-plugin-1"));
        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.success(IncrementalBuildDetectionInMavenConfigurationProbe.KEY, "Incremental Build is configured in the plugin."));
        verify(probe).doApply(plugin, ctx);
    }

    @Test
    void shouldReturnASuccessfulCheckWhenIncrementalBuildIsConfiguredOnlyInExtensionsXML() {
        when(ctx.getScmRepository()).thenReturn(Path.of("src/test/resources/jenkinsci/test-plugin-repo-2"));
        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.success(IncrementalBuildDetectionInMavenConfigurationProbe.KEY, "Incremental Build is configured in the plugin."));
        verify(probe).doApply(plugin, ctx);
    }

    @Test
    void shouldReturnASuccessfulCheckWhenIncrementalBuildIsConfiguredOnlyInMavenConfig() {
        when(ctx.getScmRepository()).thenReturn(Path.of("src/test/resources/jenkinsci/test-plugin-repo-3"));
        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.success(IncrementalBuildDetectionInMavenConfigurationProbe.KEY, "Incremental Build is configured in the plugin."));
        verify(probe).doApply(plugin, ctx);
    }

    @Test
    void shouldFailWhenIncrementalBuildIsIncorrectlyConfiguredInBothFiles() {
        when(ctx.getScmRepository()).thenReturn(Path.of("src/test/resources/jenkinsci/test-plugin-incorrect-repo-1"));
        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.failure(IncrementalBuildDetectionInMavenConfigurationProbe.KEY, "Incremental Build is not configured in the plugin."));
        verify(probe).doApply(plugin, ctx);
    }

    @Test
    void shouldFailWhenIncrementalBuildIsIncorrectlyConfiguredInExtensionsXML() {
        when(ctx.getScmRepository()).thenReturn(Path.of("src/test/resources/jenkinsci/test-plugin-incorrect-repo-2"));
        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.failure(IncrementalBuildDetectionInMavenConfigurationProbe.KEY, "Incremental Build is not configured in the plugin."));
        verify(probe).doApply(plugin, ctx);
    }

    @Test
    void shouldFailWhenIncrementalBuildIsIncorrectlyConfiguredInMavenConfig() {
        when(ctx.getScmRepository()).thenReturn(Path.of("src/test/resources/jenkinsci/test-plugin-incorrect-repo-3"));
        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.failure(IncrementalBuildDetectionInMavenConfigurationProbe.KEY, "Incremental Build is not configured in the plugin."));
        verify(probe).doApply(plugin, ctx);
    }
}
