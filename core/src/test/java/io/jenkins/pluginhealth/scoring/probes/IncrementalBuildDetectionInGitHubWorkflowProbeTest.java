package io.jenkins.pluginhealth.scoring.probes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IncrementalBuildDetectionInGitHubWorkflowProbeTest extends AbstractProbeTest<IncrementalBuildDetectionInGitHubWorkflowProbe> {
    private Plugin plugin;
    private ProbeContext ctx;
    private IncrementalBuildDetectionInGitHubWorkflowProbe probe;

    @BeforeEach
    public void init() {
        plugin = mock(Plugin.class);
        ctx = mock(ProbeContext.class);
        probe = getSpy();

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
    }

    @Override
    IncrementalBuildDetectionInGitHubWorkflowProbe getSpy() {
        return spy(IncrementalBuildDetectionInGitHubWorkflowProbe.class);
    }

    @Test
    void shouldBeAbleToDetectRepositoryWithNoIncrementalBuildConfigured() throws IOException {
        final Path repo = Files.createTempDirectory("foo");
        Files.createDirectories(repo.resolve(".github/workflows"));
        when(ctx.getScmRepository()).thenReturn(repo);

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.failure(IncrementalBuildDetectionInGitHubWorkflowProbe.KEY, "Incremental Build is not configured in the plugin."));
        verify(probe).doApply(plugin, ctx);
    }

    @Test
    void shouldSucceedIfIncrementalBuildIsConfigured() {
        when(ctx.getScmRepository()).thenReturn(Path.of("src/test/resources/jenkinsci/test-plugin-1"));
        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.success(IncrementalBuildDetectionInGitHubWorkflowProbe.KEY, "Incremental Build is configured in the plugin."));
        verify(probe).doApply(plugin, ctx);
    }

    @Test
    void shouldFailIfIncorrectIncrementalBuildIsConfigured() {
        when(ctx.getScmRepository()).thenReturn(Path.of("src/test/resources/jenkinsci/test-plugin-incorrect-repo-1"));
        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.failure(IncrementalBuildDetectionInGitHubWorkflowProbe.KEY, "Plugin has no GitHub Action configured"));
        verify(probe).doApply(plugin, ctx);
    }
}
