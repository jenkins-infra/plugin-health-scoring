package io.jenkins.pluginhealth.scoring.probes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.junit.jupiter.api.Test;

public class OpenIssuesProbeTest extends AbstractProbeTest<OpenIssuesProbe> {
    @Override
    OpenIssuesProbe getSpy() {
        return spy(OpenIssuesProbe.class);
    }

    @Test
    void shouldNotRunWithInvalidUpdateCenter() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getDetails()).thenReturn(
            Map.of(),
            Map.of(
                UpdateCenterPluginPublicationProbe.KEY, ProbeResult.failure(UpdateCenterPluginPublicationProbe.KEY, "")
            )
        );

        final OpenIssuesProbe probe = getSpy();
        for (int i = 0; i < 2; i++) {
            assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status")
                .isEqualTo(ProbeResult.error(OpenIssuesProbe.KEY, ""));
            verify(probe, never()).doApply(plugin, ctx);
        }

    }

    @Test
    void shouldBeAbleToFindOpenIssuesInBothJiraGH() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getDetails()).thenReturn(
            Map.of(
                UpdateCenterPluginPublicationProbe.KEY, ProbeResult.success(UpdateCenterPluginPublicationProbe.KEY, "")
            )
        );

        final OpenIssuesProbe probe = getSpy();
        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/accurev-plugin");

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.error(DocumentationMigrationProbe.KEY, "0 open issues found"));
    }

    @Test
    void shouldBeAbleToFindOpenIssuesInJira() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getDetails()).thenReturn(
            Map.of(
                UpdateCenterPluginPublicationProbe.KEY, ProbeResult.success(UpdateCenterPluginPublicationProbe.KEY, "")
            )
        );

        final OpenIssuesProbe probe = getSpy();
        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/active-directory-plugin");

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.error(DocumentationMigrationProbe.KEY, "0 open issues found"));

    }

    @Test
    void shouldBeAbleToFindOpenIssuesInGH() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getDetails()).thenReturn(
            Map.of(
                UpdateCenterPluginPublicationProbe.KEY, ProbeResult.success(UpdateCenterPluginPublicationProbe.KEY, "")
            )
        );

        final OpenIssuesProbe probe = getSpy();
        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/advanced-installer-msi-builder-plugin");

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.error(DocumentationMigrationProbe.KEY, "0 open issues found"));
    }
}
