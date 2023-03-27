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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;

import org.junit.jupiter.api.Test;

public class UpdateCenterPluginPublicationProbeTest {
    @Test
    public void shouldNotRequireRelease() {
        assertThat(spy(UpdateCenterPluginPublicationProbe.class).requiresRelease()).isFalse();
    }

    @Test
    public void shouldHaveStaticKey() {
        assertThat(spy(UpdateCenterPluginPublicationProbe.class).key()).isEqualTo("update-center-plugin-publication-probe");
    }

    @Test
    void shouldHaveDescription() {
        assertThat(spy(UpdateCenterPluginPublicationProbe.class).getDescription()).isNotBlank();
    }

    @Test
    public void shouldFailIfPluginIsNotInUpdateCenterMap() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String pluginName = "foo";

        when(plugin.getName()).thenReturn(pluginName);
        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of(),
            Map.of(),
            Collections.emptyList()
        ));

        final UpdateCenterPluginPublicationProbe probe = new UpdateCenterPluginPublicationProbe();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result)
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.failure(UpdateCenterPluginPublicationProbe.KEY, "This plugin's publication has been stopped by the update-center"));
    }

    @Test
    public void shouldSucceedIfPluginIsInUpdateCenterMap() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String pluginName = "foo";

        when(plugin.getName()).thenReturn(pluginName);
        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of(pluginName, new io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin(
                pluginName, null, null, null, List.of(), 0, "2.361.1", "main"
            )),
            Map.of(),
            List.of()
        ));

        final UpdateCenterPluginPublicationProbe probe = new UpdateCenterPluginPublicationProbe();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result)
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(UpdateCenterPluginPublicationProbe.KEY, "This plugin is still actively published by the update-center"));
    }
}
