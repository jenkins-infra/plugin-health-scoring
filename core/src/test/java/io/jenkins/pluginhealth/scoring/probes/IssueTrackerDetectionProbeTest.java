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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;

import org.junit.jupiter.api.Test;

class IssueTrackerDetectionProbeTest  extends AbstractProbeTest<IssueTrackerDetectionProbe> {
    @Override
    IssueTrackerDetectionProbe getSpy() {
        return spy(IssueTrackerDetectionProbe.class);
    }

    @Test
    void shouldDetectIssueTrackerInPlugin() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx =spy(new ProbeContext(plugin.getName(), new UpdateCenter(Map.of(), Map.of(), List.of(), List.of())));;
        final io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin.IssueTrackers issueTrackerGithub = new io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin.IssueTrackers("github", "https://github.com/foo-plugin/issues", "https://github.com/foo-plugin/issues/new/choose");
        final io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin.IssueTrackers issueTrackerJira = new io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin.IssueTrackers("jira", "https://issues.jenkins.io/issues/?jql=component=18331", "https://www.jenkins.io/participate/report-issue/redirect/#18331");
        final String pluginName = "foo";

        when(plugin.getDetails()).thenReturn(
            Map.of(
                UpdateCenterPluginPublicationProbe.KEY, ProbeResult.success(UpdateCenterPluginPublicationProbe.KEY, "")
            )
        );

        when(plugin.getName()).thenReturn(pluginName);
        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of(pluginName, new io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin(
                pluginName, null, null, null, List.of(), 0, "2.361.1", "main",
                List.of(issueTrackerGithub, issueTrackerJira)
            )),
            Map.of(),
            List.of(),
            List.of()
        ));

        final IssueTrackerDetectionProbe probe = getSpy();

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(IssueTrackerDetectionProbe.KEY, "Issue tracker detected and returned successfully."));

        assert(ctx.getIssueTrackerType()).equals(Map.of("github","https://github.com/foo-plugin/issues", "jira", "https://issues.jenkins.io/issues/?jql=component=18331"));
        verify(probe).doApply(plugin, ctx);
    }

}
