package io.jenkins.pluginhealth.scoring.probes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;

import org.junit.jupiter.api.Test;

public class SecurityScanGithubWorkflowProbeTest extends AbstractProbeTest<SecurityScanGithubWorkflowProbe>{
    @Override
    SecurityScanGithubWorkflowProbe getSpy() {
        return spy(SecurityScanGithubWorkflowProbe.class);
    }

    @Test
    void shouldBeAbleToDetectRepositoryWithNoGitHubWorkflowConfigured() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final SecurityScanGithubWorkflowProbe probe = getSpy();

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(Files.createTempDirectory("foo"));

        final ProbeResult result = probe.apply(plugin, ctx);
        assertThat(result.status()).isEqualTo(ResultStatus.FAILURE);
        assertThat(result.message()).isEqualTo("Plugin has no GitHub Workflow configured");
    }

    @Test
    void shouldBeAbleToDetectRepositoryWithNoSecurityScanConfigured() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final SecurityScanGithubWorkflowProbe probe = getSpy();

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, "")
        ));
        final Path repo = Files.createTempDirectory("foo");

        when(ctx.getScmRepository()).thenReturn(repo);
        Files.createDirectories(repo.resolve(".github/workflows"));
        when(ctx.getScmRepository()).thenReturn(repo);

        final ProbeResult result = probe.apply(plugin, ctx);
        assertThat(result.status()).isEqualTo(ResultStatus.FAILURE);
        assertThat(result.message()).isEqualTo("Plugin has no GitHub Security Scan configured");
    }


}
