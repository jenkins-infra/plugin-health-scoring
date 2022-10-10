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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;
import io.jenkins.pluginhealth.scoring.service.PluginService;
import io.jenkins.pluginhealth.scoring.service.UpdateCenterService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProbeEngineTest {

    @Mock
    private PluginService pluginService;
    @Mock
    private UpdateCenterService updateCenterService;

    @Test
    public void shouldBeAbleToRunSimpleProbe() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final Probe probe = mock(Probe.class);

        final ProbeEngine probeEngine = new ProbeEngine(List.of(probe), pluginService, updateCenterService);
        final ProbeResult expectedResult = ProbeResult.success("foo", "bar");

        when(probe.doApply(any(Plugin.class), any(ProbeContext.class))).thenReturn(expectedResult);
        when(pluginService.streamAll()).thenReturn(Stream.of(plugin));
        probeEngine.run();

        verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));
        verify(plugin).addDetails(expectedResult);
        verify(pluginService).saveOrUpdate(plugin);
    }

    @Test
    public void shouldNotApplyProbeWithReleaseRequirementOnPluginWithNoNewReleaseWithPastResult() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final String probeKey = "wiz";
        final Probe probe = mock(Probe.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final ProbeEngine probeEngine = new ProbeEngine(List.of(probe), pluginService, updateCenterService);

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusDays(1));
        when(plugin.getDetails()).thenReturn(Map.of(probeKey, ProbeResult.success(probeKey, "This is good")));
        when(probe.requiresRelease()).thenReturn(true);
        when(probe.key()).thenReturn(probeKey);
        when(pluginService.streamAll()).thenReturn(Stream.of(plugin));
        probeEngine.run();

        verify(probe, never()).doApply(plugin, ctx);
        verify(pluginService).saveOrUpdate(plugin);
    }

    @Test
    public void shouldApplyProbeWithReleaseRequirementOnPluginWithNewReleaseAndPastResult() throws IOException {
        final String probeKey = "wiz";
        final Plugin plugin = mock(Plugin.class);
        final Probe probe = mock(Probe.class);
        final ProbeEngine probeEngine = new ProbeEngine(List.of(probe), pluginService, updateCenterService);

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now());
        when(plugin.getDetails()).thenReturn(Map.of(probeKey, new ProbeResult(probeKey, "this is ok", ResultStatus.SUCCESS, ZonedDateTime.now().minusDays(1))));
        when(probe.requiresRelease()).thenReturn(true);
        when(probe.apply(any(Plugin.class), any(ProbeContext.class)))
            .thenReturn(ProbeResult.success(probeKey, "This is also ok"));
        when(probe.key()).thenReturn(probeKey);
        when(pluginService.streamAll()).thenReturn(Stream.of(plugin));
        probeEngine.run();

        verify(probe).doApply(eq(plugin), any(ProbeContext.class));
        verify(pluginService).saveOrUpdate(plugin);
    }

    @Test
    public void shouldApplyProbeWithNoReleaseRequirementOnPluginWithPastResult() throws IOException {
        final String probeKey = "wiz";
        final Plugin plugin = mock(Plugin.class);
        final Probe probe = mock(Probe.class);
        final ProbeEngine probeEngine = new ProbeEngine(List.of(probe), pluginService, updateCenterService);

        when(plugin.getDetails()).thenReturn(Map.of(probeKey, new ProbeResult(probeKey, "this is ok", ResultStatus.SUCCESS, ZonedDateTime.now().minusDays(1))));
        when(probe.requiresRelease()).thenReturn(false);
        when(probe.apply(eq(plugin), any(ProbeContext.class))).thenReturn(ProbeResult.success(probeKey, "This is also ok"));
        when(probe.key()).thenReturn(probeKey);
        when(pluginService.streamAll()).thenReturn(Stream.of(plugin));
        probeEngine.run();

        verify(probe).doApply(eq(plugin), any(ProbeContext.class));
        verify(pluginService).saveOrUpdate(plugin);
    }

    @Test
    public void shouldNotSaveErrors() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final Probe probe = mock(Probe.class);
        final ProbeEngine probeEngine = new ProbeEngine(List.of(probe), pluginService, updateCenterService);

        when(probe.doApply(eq(plugin), any(ProbeContext.class))).thenReturn(ProbeResult.error("foo", "bar"));
        when(pluginService.streamAll()).thenReturn(Stream.of(plugin));
        probeEngine.run();

        verify(plugin, never()).addDetails(any(ProbeResult.class));
    }

    @Test
    public void shouldBeAbleToGetPreviousContextResultInExecution() throws IOException {
        final Plugin plugin = spy(Plugin.class);
        final Probe probeOne = mock(Probe.class);
        final Probe probeTwo = new Probe() {
            @Override
            protected ProbeResult doApply(Plugin plugin, ProbeContext ctx) {
                return plugin.getDetails().get("foo") != null ?
                    ProbeResult.success(key(), "This is also ok") :
                    ProbeResult.error(key(), "This cannot be validated");
            }

            @Override
            public String key() {
                return "bar";
            }

            @Override
            public String getDescription() {
                return null;
            }
        };
        final ProbeEngine probeEngine = new ProbeEngine(List.of(probeOne, probeTwo), pluginService, updateCenterService);

        when(probeOne.key()).thenReturn("foo");
        when(probeOne.doApply(any(Plugin.class), any(ProbeContext.class)))
            .thenReturn(ProbeResult.success("foo", "This is ok"));
        when(pluginService.streamAll()).thenReturn(Stream.of(plugin));
        probeEngine.run();

        verify(plugin, times(2)).addDetails(any(ProbeResult.class));
        assertThat(plugin.getDetails().keySet()).containsExactlyInAnyOrder("foo", "bar");
    }
}
