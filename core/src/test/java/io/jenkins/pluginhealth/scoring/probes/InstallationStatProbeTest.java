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
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;
import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstallationStatProbeTest {

    @Test
    void doesNotRequireRelease() {
        final InstallationStatProbe probe = spy(InstallationStatProbe.class);
        assertThat(probe.requiresRelease()).isFalse();
    }

    @Test
    void doesNotRequireCodeModification() {
        final InstallationStatProbe probe = spy(InstallationStatProbe.class);
        assertThat(probe.isSourceCodeRelated()).isFalse();
    }

    @Test
    void shouldHaveStatAsKey() {
        final InstallationStatProbe probe = spy(InstallationStatProbe.class);
        assertThat(probe.key()).isEqualTo("stat");
    }

    @Test
    void shouldHaveDescription() {
        final InstallationStatProbe probe = spy(InstallationStatProbe.class);
        assertThat(probe.getDescription()).isNotBlank();
    }

    @Test
    void shouldFailWhenPluginIsNotInUpdateCenter() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final InstallationStatProbe probe = spy(InstallationStatProbe.class);

        final String pluginName = "plugin";
        when(plugin.getName()).thenReturn(pluginName);
        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of(),
            Map.of(),
            List.of()
        ));

        final ProbeResult result = probe.apply(plugin, ctx);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).extracting("status").isEqualTo(ResultStatus.FAILURE);
        });
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
                new io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin(pluginName, null, null, null, List.of(), 100, "", "main")
            ),
            Map.of(),
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
