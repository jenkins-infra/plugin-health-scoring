/*
 * MIT License
 *
 * Copyright (c) 2023 Jenkins Infra
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
import static org.mockito.ArgumentMatchers.anyString;
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
import org.mockito.InjectMocks;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

class JiraOpenIssuesProbeTest extends AbstractProbeTest<JiraOpenIssuesProbe> {

    @InjectMocks
     JiraOpenIssuesProbe jiraOpenIssuesProbe = new JiraOpenIssuesProbe();

    @Override
    JiraOpenIssuesProbe getSpy() {
        return spy(JiraOpenIssuesProbe.class);
    }

    @Test
    void shouldNotRunWithInvalidProbeResultRequirement() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getDetails()).thenReturn(
            Map.of(),
            Map.of(
                IssueTrackerDetectionProbe.KEY, ProbeResult.success(IssueTrackerDetectionProbe.KEY, "")
            ),
            Map.of(
                SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, "")
            ),
            Map.of(
                SCMLinkValidationProbe.KEY, ProbeResult.failure(SCMLinkValidationProbe.KEY, ""),
                IssueTrackerDetectionProbe.KEY, ProbeResult.failure(IssueTrackerDetectionProbe.KEY, "")
            ),
            Map.of(
                SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
                IssueTrackerDetectionProbe.KEY, ProbeResult.failure(IssueTrackerDetectionProbe.KEY, "")
            ),
            Map.of(
                SCMLinkValidationProbe.KEY, ProbeResult.failure(SCMLinkValidationProbe.KEY, ""),
                IssueTrackerDetectionProbe.KEY, ProbeResult.success(IssueTrackerDetectionProbe.KEY, "")
            )
        );

        final JiraOpenIssuesProbe probe = getSpy();
        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.error(JiraOpenIssuesProbe.KEY, "jira-open-issues does not meet the criteria to be executed on null"));
        verify(probe, never()).doApply(plugin, ctx);
    }

    @Test
    void shouldBeAbleToFindNumberOfOpenIssuesInJira() {
        final String pluginName = "mailer";
        final String repository = "jenkinsci/" + pluginName + "-plugin";
        final String scmLink = "https://github.com/" + repository;

        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final RestTemplate mockedRestTemplate = mock(RestTemplate.class);
        final ResponseEntity mockedResponseEntity = mock(ResponseEntity.class);

        final io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin.IssueTrackers issueTrackerJira =
            new io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin.IssueTrackers("jira", "https://issues.jenkins.io/issues/?jql=component=18331", "https://www.jenkins.io/participate/report-issue/redirect/#18331");

        final String JSONString = "{\"expand\":\"names,schema\",\"startAt\":0,\"maxResults\":50,\"total\":10,\"issues\":[]}";

        when(plugin.getDetails()).thenReturn(
            Map.of(
                SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
                IssueTrackerDetectionProbe.KEY, ProbeResult.success(IssueTrackerDetectionProbe.KEY, "")
            )
        );

        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of(pluginName, new io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin(
                pluginName, null, null, null, List.of(), 0, "2.361.1", "main",
                List.of(issueTrackerJira)
            )),
            Map.of(),
            List.of()
        ));

        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getScm()).thenReturn(scmLink);
        when(ctx.getIssueTrackerNameAndUrl()).thenReturn(Map.of("jira", "https://issues.jenkins.io/issues/?jql=component=18331"));

        when(mockedResponseEntity.getBody()).thenReturn(JSONString);

        when(mockedRestTemplate.getForEntity(
                anyString(),
                any(Class.class)
            ))
            .thenReturn(mockedResponseEntity);

        jiraOpenIssuesProbe.restTemplate = mockedRestTemplate;

        assertThat(jiraOpenIssuesProbe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(JiraOpenIssuesProbe.KEY, "10 open issues found in the mailer plugin."));
    }

    @Test
    void shouldReturnErrorWhenJIRAReturnsErrors() {
        final String pluginName = "foo";
        final String repository = "jenkinsci/" + pluginName + "-plugin";
        final String scmLink = "https://github.com/" + repository;

        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final RestTemplate mockedRestTemplate = mock(RestTemplate.class);
        final ResponseEntity mockedResponseEntity = mock(ResponseEntity.class);

        final io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin.IssueTrackers issueTrackerJira =
            new io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin.IssueTrackers("jira", "https://issues.jenkins.io/issues/?jql=component=0", "https://www.jenkins.io/participate/report-issue/redirect/#0");

        final String errorJSONString = "{\"errorMessages\":[\"A value with ID '0' does not exist for the field 'component'.\"],\"errors\":{}}";

        when(plugin.getDetails()).thenReturn(
            Map.of(
                SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
                IssueTrackerDetectionProbe.KEY, ProbeResult.success(IssueTrackerDetectionProbe.KEY, "")
            )
        );

        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of(pluginName, new io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin(
                pluginName, null, null, null, List.of(), 0, "2.361.1", "main",
                List.of(issueTrackerJira)
            )),
            Map.of(),
            List.of()
        ));

        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getScm()).thenReturn(scmLink);
        when(ctx.getIssueTrackerNameAndUrl()).thenReturn(Map.of("jira", "https://issues.jenkins.io/issues/?jql=component=0"));

        when(mockedResponseEntity.getBody()).thenReturn(errorJSONString);

        when(mockedRestTemplate.getForEntity(
            anyString(),
            any(Class.class)
        )).thenReturn(mockedResponseEntity);

        jiraOpenIssuesProbe.restTemplate = mockedRestTemplate;

        assertThat(jiraOpenIssuesProbe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.failure(JiraOpenIssuesProbe.KEY, "Could not find open issues in the foo plugin."));
    }
}

