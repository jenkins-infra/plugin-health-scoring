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
class JenkinsCoreProbeTest {
    @Test
    void shouldBeUsingJenkinsVersionKey() {
        final JenkinsCoreProbe probe = spy(JenkinsCoreProbe.class);
        assertThat(probe.key()).isEqualTo("jenkins-version");
    }

    @Test
    void shouldRequireRelease() {
        final JenkinsCoreProbe probe = spy(JenkinsCoreProbe.class);
        assertThat(probe.requiresRelease()).isTrue();
    }

    @Test
    void shouldNotRequireSourceCodeChange() {
        final JenkinsCoreProbe probe = spy(JenkinsCoreProbe.class);
        assertThat(probe.isSourceCodeRelated()).isFalse();
    }

    @Test
    void shouldHaveDescription() {
        final JenkinsCoreProbe probe = spy(JenkinsCoreProbe.class);
        assertThat(probe.getDescription()).isNotBlank();
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

        final JenkinsCoreProbe probe = spy(JenkinsCoreProbe.class);
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result).isNotNull();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).extracting("id").isEqualTo(probe.key());
            softly.assertThat(result).extracting("status").isEqualTo(ResultStatus.FAILURE);
            softly.assertThat(result).extracting("message").isEqualTo("Plugin is not in the update-center");
        });
    }

    @Test
    void shouldBeAbleToExtractJenkinsVersionFromUpdateCenter() {
        final String pluginName = "plugin";
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getName()).thenReturn(pluginName);
        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of(
                pluginName,
                new io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin(
                    pluginName, null, null, null, List.of(), 0, "2.361.1"
                )
            ),
            Map.of(),
            List.of()
        ));

        final JenkinsCoreProbe probe = spy(JenkinsCoreProbe.class);
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result).isNotNull();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).extracting("id").isEqualTo(probe.key());
            softly.assertThat(result).extracting("status").isEqualTo(ResultStatus.SUCCESS);
            softly.assertThat(result).extracting("message").isEqualTo("2.361.1");
        });
    }
}
