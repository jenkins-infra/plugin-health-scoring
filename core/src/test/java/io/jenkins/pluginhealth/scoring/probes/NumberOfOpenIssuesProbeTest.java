package io.jenkins.pluginhealth.scoring.probes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

class NumberOfOpenIssuesProbeTest extends AbstractProbeTest<NumberOfOpenIssuesProbe> {

    @Override
    NumberOfOpenIssuesProbe getSpy() {
        return spy(NumberOfOpenIssuesProbe.class);
    }

    @Test
    void shouldNotRunWithInvalidProbeResultRequirement() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getDetails()).thenReturn(
            Map.of(),
            Map.of(
                UpdateCenterPluginPublicationProbe.KEY, ProbeResult.success(UpdateCenterPluginPublicationProbe.KEY, "")
            ),
            Map.of(
                SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, "")
            ),
            Map.of(
                SCMLinkValidationProbe.KEY, ProbeResult.failure(SCMLinkValidationProbe.KEY, ""),
                UpdateCenterPluginPublicationProbe.KEY, ProbeResult.failure(UpdateCenterPluginPublicationProbe.KEY, "")
            ),
            Map.of(
                SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
                UpdateCenterPluginPublicationProbe.KEY, ProbeResult.failure(UpdateCenterPluginPublicationProbe.KEY, "")
            ),
            Map.of(
                SCMLinkValidationProbe.KEY, ProbeResult.failure(SCMLinkValidationProbe.KEY, ""),
                UpdateCenterPluginPublicationProbe.KEY, ProbeResult.success(UpdateCenterPluginPublicationProbe.KEY, "")
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
    void shouldBeAbleToFindNumberOfOpenIssuesInBothJiraGH() throws IOException {
        final String pluginName = "maven-repo-cleaner";
        final String repository = "jenkinsci/" + pluginName + "-plugin";
        final String scmLink = "https://github.com/" + repository;

        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final GitHub gh = mock(GitHub.class);
        final GHRepository ghRepository = mock(GHRepository.class);

        when(plugin.getDetails()).thenReturn(
            Map.of(
                SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
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
                    "https://www.jenkins.io/participate/report-issue/redirect/#15979",
                    "type",
                    "jira",
                    "viewUrl",
                    "https://issues.jenkins.io/issues/?jql=component=15979"
                ),
                Map.of(
                    "reportUrl",
                    "https://github.com/jenkinsci/maven-repo-cleaner-plugin/issues/new/choose",
                    "type",
                    "github",
                    "viewUrl",
                    "https://github.com/jenkinsci/maven-repo-cleaner-plugin/issues"
                )
            )
        ));
        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getScm()).thenReturn(scmLink);

        when(ctx.getGitHub()).thenReturn(gh);
        when(ctx.getRepositoryName(scmLink)).thenReturn(Optional.of(repository));
        when(gh.getRepository(repository)).thenReturn(ghRepository);
        when(ghRepository.getOpenIssueCount()).thenReturn(10);

        final NumberOfOpenIssuesProbe probe = getSpy();

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(NumberOfOpenIssuesProbe.KEY, "6 open issues found in JIRA. 10 open issues found in GitHub."));
        verify(probe).doApply(plugin, ctx);
    }

    @Test
    void shouldBeAbleToFindNumberOfOpenIssuesOnlyInJira() throws JsonProcessingException {
        final String pluginName = "mailer";
        final String repository = "jenkinsci/" + pluginName + "-plugin";
        final String scmLink = "https://github.com/" + repository;

        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        JsonNode jsonNodeMock = mock(JsonNode.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        ResponseEntity responseEntity = mock(ResponseEntity.class);

        final String jsonString = "{\"expand\":\"names,schema\",\"startAt\":0,\"maxResults\":50,\"total\":1,\"issues\":[]}";

        when(plugin.getDetails()).thenReturn(
            Map.of(
                SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
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

        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getScm()).thenReturn(scmLink);
        when(ctx.getRepositoryName(scmLink)).thenReturn(Optional.of(repository));
        when(restTemplate.getForEntity(anyString(), anyString().getClass())).thenReturn(responseEntity);
        when(responseEntity.getBody()).thenReturn(jsonString);
        when(jsonNodeMock.get("total")).thenReturn(new IntNode(10));

        final NumberOfOpenIssuesProbe probe = getSpy();

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(NumberOfOpenIssuesProbe.KEY, "0 open issues found in JIRA."));
        verify(probe).doApply(plugin, ctx);
    }

    @Test
   void shouldBeAbleToFindNumberOfOpenIssuesOnlyInGH() throws IOException {
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
            .isEqualTo(ProbeResult.success(NumberOfOpenIssuesProbe.KEY, "6 open issues found in GitHub."));
        verify(probe).doApply(plugin, ctx);

    }
}



