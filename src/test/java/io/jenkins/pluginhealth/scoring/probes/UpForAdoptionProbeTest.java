/*
 * MIT License
 *
 * Copyright (c) 2022 Jenkins Infra
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
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;
import io.jenkins.pluginhealth.scoring.model.UpdateCenter;
import io.jenkins.pluginhealth.scoring.model.UpdateCenterPlugin;

import org.junit.jupiter.api.Test;

public class UpForAdoptionProbeTest {
    @Test
    public void shouldNotRequireNewRelease() {
        assertThat(new UpForAdoptionProbe().requiresRelease()).isFalse();
    }

    @Test
    public void shouldKeepTheSameKey() {
        assertThat(new UpForAdoptionProbe().key()).isEqualTo("up-for-adoption");
    }

    @Test
    public void shouldBeAbleToDetectPluginForAdoption() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final UpForAdoptionProbe upForAdoptionProbe = new UpForAdoptionProbe();

        when(plugin.getName()).thenReturn("foo");
        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of("foo", new UpdateCenterPlugin("foo", "not-a-scm", ZonedDateTime.now().minusDays(1), List.of("builder", "adopt-this-plugin"))),
            Collections.emptyMap()
        ));

        final ProbeResult result = upForAdoptionProbe.apply(plugin, ctx);

        assertThat(result.status()).isEqualTo(ResultStatus.FAILURE);
    }

    @Test
    public void shouldBeAbleToDetectPluginNotForAdoption() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final UpForAdoptionProbe upForAdoptionProbe = new UpForAdoptionProbe();

        when(plugin.getName()).thenReturn("foo");
        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of("foo", new UpdateCenterPlugin("foo", "not-a-scm", ZonedDateTime.now().minusDays(1), List.of("builder"))),
            Collections.emptyMap()
        ));

        final ProbeResult result = upForAdoptionProbe.apply(plugin, ctx);

        assertThat(result.status()).isEqualTo(ResultStatus.SUCCESS);
    }
}
