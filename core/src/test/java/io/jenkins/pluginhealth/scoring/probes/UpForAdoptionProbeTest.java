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

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;
import io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin;
import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;

import hudson.util.VersionNumber;
import org.junit.jupiter.api.Test;

class UpForAdoptionProbeTest extends AbstractProbeTest<UpForAdoptionProbe> {
    @Override
    UpForAdoptionProbe getSpy() {
        return spy(UpForAdoptionProbe.class);
    }

    @Test
    void shouldNotRequireNewRelease() {
        assertThat(getSpy().requiresRelease()).isFalse();
    }

    @Test
    void shouldBeAbleToDetectPluginForAdoption() {
        final var plugin = mock(io.jenkins.pluginhealth.scoring.model.Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final UpForAdoptionProbe upForAdoptionProbe = getSpy();

        when(plugin.getName()).thenReturn("foo");
        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of("foo", new Plugin("foo", new VersionNumber("1.0"), "not-a-scm", ZonedDateTime.now().minusDays(1), List.of("builder", "adopt-this-plugin"), 0, "", "main")),
            Collections.emptyMap(),
            Collections.emptyList(),
            Collections.emptyList()
        ));

        final ProbeResult result = upForAdoptionProbe.apply(plugin, ctx);

        assertThat(result.status()).isEqualTo(ResultStatus.FAILURE);
    }

    @Test
    void shouldBeAbleToDetectPluginNotForAdoption() {
        final io.jenkins.pluginhealth.scoring.model.Plugin plugin = mock(io.jenkins.pluginhealth.scoring.model.Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final UpForAdoptionProbe upForAdoptionProbe = getSpy();

        when(plugin.getName()).thenReturn("foo");
        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of("foo", new Plugin("foo", new VersionNumber("1.0"), "not-a-scm", ZonedDateTime.now().minusDays(1), List.of("builder"), 0, "", "main")),
            Collections.emptyMap(),
            Collections.emptyList(),
            Collections.emptyList()
        ));

        final ProbeResult result = upForAdoptionProbe.apply(plugin, ctx);

        assertThat(result.status()).isEqualTo(ResultStatus.SUCCESS);
    }

    @Test
    void shouldFailWhenPluginNotPresentInUpdateCenter() {
        final var plugin = mock(io.jenkins.pluginhealth.scoring.model.Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getName()).thenReturn("foo");

        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of(),
            Map.of(),
            List.of(),
            List.of()
        ));

        final UpForAdoptionProbe probe = getSpy();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result.status()).isEqualTo(ResultStatus.FAILURE);
    }
}
