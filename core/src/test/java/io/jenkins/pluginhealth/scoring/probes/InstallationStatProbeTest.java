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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;
import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class InstallationStatProbeTest extends AbstractProbeTest<InstallationStatProbe> {
    @Override
    InstallationStatProbe getSpy() {
        return spy(InstallationStatProbe.class);
    }

    @Test
    void doesNotRequireRelease() {
        assertThat(getSpy().requiresRelease()).isFalse();
    }

    @Test
    void doesNotRequireCodeModification() {
        assertThat(getSpy().isSourceCodeRelated()).isFalse();
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldRequirePluginToBeInUpdateCenter() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getDetails()).thenReturn(
            Map.of(),
            Map.of(
                UpdateCenterPluginPublicationProbe.KEY, ProbeResult.failure(UpdateCenterPluginPublicationProbe.KEY, "")
            )
        );

        final InstallationStatProbe probe = spy(InstallationStatProbe.class);
        for (int i = 0; i < 2; i++) {
            assertThat(probe.apply(plugin, ctx).status()).isEqualTo(ResultStatus.ERROR);
            verify(probe, never()).doApply(plugin, ctx);
        }
    }

    @Test
    void shouldBeAbleToFindInstallationCountInUpdateCenter() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final InstallationStatProbe probe = spy(InstallationStatProbe.class);

        final String pluginName = "plugin";
        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getDetails()).thenReturn(Map.of(
            UpdateCenterPluginPublicationProbe.KEY, ProbeResult.success(UpdateCenterPluginPublicationProbe.KEY, "")
        ));
        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of(
                pluginName,
                new io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin(pluginName, null, null, null, List.of(), 100, "", "main", List.of(new io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin.IssueTrackers("", "", "")))
            ),
            Map.of(),
            List.of(),
            List.of()
        ));

        final ProbeResult result = probe.apply(plugin, ctx);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).extracting("status").isEqualTo(ResultStatus.SUCCESS);
            softly.assertThat(result).extracting("message").isEqualTo("100");
        });
    }
}
