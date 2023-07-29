package io.jenkins.pluginhealth.scoring.probes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

public class NumberOfOpenIssuesProbeTest extends AbstractProbeTest<NumberOfOpenIssuesProbe> {

    @Override
    NumberOfOpenIssuesProbe getSpy() {
        return spy(NumberOfOpenIssuesProbe.class);
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

        final NumberOfOpenIssuesProbe probe = getSpy();
        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.error(NumberOfOpenIssuesProbe.KEY, "open-issue does not meet the criteria to be executed on null"));
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

        final NumberOfOpenIssuesProbe probe = getSpy();
        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/accurev-plugin");

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(NumberOfOpenIssuesProbe.KEY, "0 open issues found"));
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
                    "https://www.jenkins.io/participate/report-issue/redirect/#18331",
                    "type",
                    "jira",
                    "viewUrl",
                    "https://issues.jenkins.io/issues/?jql=component=18331"
                )
            )
        ));

        final NumberOfOpenIssuesProbe probe = getSpy();
        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/active-directory-plugin");

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(NumberOfOpenIssuesProbe.KEY, "0 open issues found"));
        verify(probe, atMostOnce()).doApply(plugin, ctx);
    }

    @Test
   void shouldBeAbleToFindNumberOfOpenIssuesInGH() throws IOException {
        final String pluginName = "cloudevents";
        final String repository = "jenkinsci/" + pluginName + "-plugin";
        final String scmLink = "https://github.com/" + repository;

        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        final GitHub gh = mock(GitHub.class);
        final GHRepository ghRepository = mock(GHRepository.class);

        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getDetails()).thenReturn(
            Map.of(
                SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
                UpdateCenterPluginPublicationProbe.KEY, ProbeResult.success(UpdateCenterPluginPublicationProbe.KEY, "")
            )
        );

        when(plugin.getScm()).thenReturn(scmLink);
        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of(),
            Map.of(),
            List.of(),
            List.of(
                Map.of(
                    "reportUrl",
                    "https://github.com/" + repository + "/issues/new/choose",
                    "type",
                    "github",
                    "viewUrl",
                    "https://github.com/" + repository + "/issues"
                )
            )
        ));
        when(ctx.getGitHub()).thenReturn(gh);
        when(ctx.getRepositoryName(plugin.getScm())).thenReturn(Optional.of(repository));
        when(gh.getRepository(repository)).thenReturn(ghRepository);
        when(ghRepository.getOpenIssueCount()).thenReturn(6);

        final NumberOfOpenIssuesProbe probe = getSpy();

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(NumberOfOpenIssuesProbe.KEY, "6 open issues found"));
        verify(probe, atMostOnce()).doApply(plugin, ctx);

    }
}



