package io.jenkins.pluginhealth.scoring.probes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;

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
        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.error(OpenIssuesProbe.KEY, "open-issue does not meet the criteria to be executed on null"));
        verify(probe, never()).doApply(plugin, ctx);
    }

    @Test
    void shouldBeAbleToFindNumberOfOpenIssuesInBothJiraGH() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getDetails()).thenReturn(
            Map.of(
                UpdateCenterPluginPublicationProbe.KEY, ProbeResult.success(UpdateCenterPluginPublicationProbe.KEY, "")
            )
        );

        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of(),
            Map.of(),
            List.of(),
            List.of(
                Map.of(
                    "reportUrl",
                    "https://www.jenkins.io/participate/report-issue/redirect/#15525",
                    "type",
                    "jira",
                    "viewUrl",
                    "https://issues.jenkins.io/issues/?jql=component=15525"
                ),
                Map.of(
                    "reportUrl",
                    "https://github.com/jenkinsci/accurev-plugin/issues/new/choose",
                    "type",
                    "github",
                    "viewUrl",
                    "https://github.com/jenkinsci/accurev-plugin/issues"
                )
            )
        ));

        final OpenIssuesProbe probe = getSpy();
        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/accurev-plugin");

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(OpenIssuesProbe.KEY, "0 open issues found"));
        verify(probe, atMostOnce()).doApply(plugin, ctx);
    }

    @Test
    void shouldBeAbleToFindNumberOfOpenIssuesInJira() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getDetails()).thenReturn(
            Map.of(
                UpdateCenterPluginPublicationProbe.KEY, ProbeResult.success(UpdateCenterPluginPublicationProbe.KEY, "")
            )
        );

        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of(),
            Map.of(),
            List.of(),
            List.of(
                Map.of(
                    "reportUrl",
                    "https://www.jenkins.io/participate/report-issue/redirect/#22024",
                    "type",
                    "jira",
                    "viewUrl",
                    "https://issues.jenkins.io/issues/?jql=component=22024"
                )
            )
        ));

        final OpenIssuesProbe probe = getSpy();
        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/active-directory-plugin");

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(OpenIssuesProbe.KEY, "0 open issues found"));
        verify(probe, atMostOnce()).doApply(plugin, ctx);
    }

    @Test
    void shouldBeAbleToFindNumberOfOpenIssuesInGH() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getDetails()).thenReturn(
            Map.of(
                UpdateCenterPluginPublicationProbe.KEY, ProbeResult.success(UpdateCenterPluginPublicationProbe.KEY, "")
            )
        );

        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of(),
            Map.of(),
            List.of(),
            List.of(
                Map.of(
                    "reportUrl",
                    "https://github.com/jenkinsci/advanced-installer-msi-builder-plugin/issues/new/choose",
                    "type",
                    "github",
                    "viewUrl",
                    "https://github.com/jenkinsci/advanced-installer-msi-builder-plugin/issues"
                )
            )
        ));

        final OpenIssuesProbe probe = getSpy();
        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/advanced-installer-msi-builder-plugin");

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(OpenIssuesProbe.KEY, "0 open issues found"));
        verify(probe, atMostOnce()).doApply(plugin, ctx);
    }
}
