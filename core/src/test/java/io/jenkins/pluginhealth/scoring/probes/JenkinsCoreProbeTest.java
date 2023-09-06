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

import org.junit.jupiter.api.Test;

class JenkinsCoreProbeTest extends AbstractProbeTest<JenkinsCoreProbe> {
    @Override
    JenkinsCoreProbe getSpy() {
        return spy(JenkinsCoreProbe.class);
    }

    @Test
    void shouldRequireRelease() {
        assertThat(getSpy().requiresRelease()).isTrue();
    }

    @Test
    void shouldNotRequireSourceCodeChange() {
        assertThat(getSpy().isSourceCodeRelated()).isFalse();
    }

    @Test
    void shouldFailIfPluginNotInUpdateCenter() {
        final String pluginName = "plugin";
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getName()).thenReturn(pluginName);
        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of(),
            Map.of(),
            List.of()
        ));

        final JenkinsCoreProbe probe = getSpy();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result)
            .isNotNull()
            .extracting("id", "status")
            .containsExactly(probe.key(), ResultStatus.ERROR);

        verify(probe, never()).doApply(plugin, ctx);
    }

    @Test
    void shouldBeAbleToExtractJenkinsVersionFromUpdateCenter() {
        final String pluginName = "plugin";
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getDetails()).thenReturn(Map.of(
            UpdateCenterPluginPublicationProbe.KEY, ProbeResult.success(UpdateCenterPluginPublicationProbe.KEY, "")
        ));
        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of(
                pluginName,
                io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin.of(
                    pluginName, null, null, null, List.of(), 0, "2.361.1", "main"
                )
            ),
            Map.of(),
            List.of()
        ));

        final JenkinsCoreProbe probe = getSpy();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result).isNotNull();
        assertThat(result)
            .extracting("id", "status", "message")
            .containsExactly(probe.key(), ResultStatus.SUCCESS, "2.361.1");
    }
}
