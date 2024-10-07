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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;
import io.jenkins.pluginhealth.scoring.service.PluginDocumentationService;
import io.jenkins.pluginhealth.scoring.service.PluginService;
import io.jenkins.pluginhealth.scoring.service.ProbeService;
import io.jenkins.pluginhealth.scoring.service.UpdateCenterService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.GitHub;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProbeEngineTest {
    @Mock private PluginService pluginService;
    @Mock private ProbeService probeService;
    @Mock private UpdateCenterService updateCenterService;
    @Mock private GitHub gitHub;
    @Mock private PluginDocumentationService pluginDocumentationService;

    @BeforeEach
    void setup() throws IOException {
        when(updateCenterService.fetchUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of(),
            Map.of(),
            List.of()
        ));
    }

    @Test
    void shouldBeAbleToRunSimpleProbe() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final Probe probe = spy(Probe.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        final ProbeResult expectedResult = ProbeResult.success("foo", "bar", 1);

        when(plugin.getScm()).thenReturn("this-is-ok-for-testing");
        when(plugin.getDetails()).thenReturn(Map.of());
        when(probe.key()).thenReturn("probe");
        when(probe.doApply(plugin, ctx)).thenReturn(expectedResult);

        when(probeService.getProbeContext(any(Plugin.class), any(UpdateCenter.class))).thenReturn(ctx);
        when(probeService.getProbes()).thenReturn(List.of(probe));
        when(pluginService.streamAll()).thenReturn(Stream.of(plugin));

        final ProbeEngine probeEngine = new ProbeEngine(probeService, pluginService, updateCenterService, gitHub, pluginDocumentationService);
        probeEngine.run();

        verify(probe).doApply(plugin, ctx);
        verify(plugin).addDetails(expectedResult);
        verify(pluginService).saveOrUpdate(plugin);
    }

    @Test
    void shouldNotApplyProbeWithReleaseRequirementOnPluginWithNoNewReleaseWithPastResult() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final String probeKey = "wiz";
        final Probe probe = spy(Probe.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getScm()).thenReturn("this-is-ok-for-testing");
        when(plugin.getName()).thenReturn("foo");
        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusDays(1));
        when(plugin.getDetails()).thenReturn(Map.of(probeKey, ProbeResult.success(probeKey, "This is good", 1)));

        when(probe.requiresRelease()).thenReturn(true);
        when(probe.key()).thenReturn(probeKey);
        when(probe.getVersion()).thenReturn(1L);

        when(probeService.getProbeContext(any(Plugin.class), any(UpdateCenter.class))).thenReturn(ctx);
        when(probeService.getProbes()).thenReturn(List.of(probe));
        when(pluginService.streamAll()).thenReturn(Stream.of(plugin));

        final ProbeEngine probeEngine = new ProbeEngine(probeService, pluginService, updateCenterService, gitHub, pluginDocumentationService);
        probeEngine.run();

        verify(probe, never()).doApply(plugin, ctx);
        verify(pluginService).saveOrUpdate(plugin);
    }

    @Test
    void shouldNotApplyProbeRelatedToCodeWithNoNewCode() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final Probe probe = spy(Probe.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getScm()).thenReturn("this-is-ok-for-testing");
        when(plugin.getDetails()).thenReturn(Map.of(
            "probe", new ProbeResult("probe", "message", ProbeResult.Status.SUCCESS, ZonedDateTime.now().minusDays(1), 1)
        ));
        when(plugin.getName()).thenReturn("foo");

        when(probe.getVersion()).thenReturn(1L);
        when(probe.key()).thenReturn("probe");
        when(probe.requiresRelease()).thenReturn(false);
        when(probe.isSourceCodeRelated()).thenReturn(true);

        when(probeService.getProbeContext(any(Plugin.class), any(UpdateCenter.class))).thenReturn(ctx);
        when(probeService.getProbes()).thenReturn(List.of(probe));
        when(pluginService.streamAll()).thenReturn(Stream.of(plugin));

        final ProbeEngine probeEngine = new ProbeEngine(probeService, pluginService, updateCenterService, gitHub, pluginDocumentationService);
        probeEngine.run();

        verify(probe, never()).doApply(plugin, ctx);
    }

    @Test
    void shouldApplyProbeRelatedToCodeWithNewCommit() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final Probe probe = spy(Probe.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String probeKey = "probe";

        final ProbeResult result = new ProbeResult(probeKey, "message", ProbeResult.Status.SUCCESS, 1);

        when(plugin.getDetails()).thenReturn(Map.of(
            probeKey, new ProbeResult(probeKey, "message", ProbeResult.Status.SUCCESS, ZonedDateTime.now().minusDays(1), 1)
        ));

        when(probe.getVersion()).thenReturn(1L);
        when(ctx.getScmRepository()).thenReturn(Optional.of(Files.createTempDirectory("ProbeEngineTest-shouldApplyProbeRelatedToCodeWithNewCommit")));
        when(ctx.getLastCommitDate()).thenReturn(Optional.of(ZonedDateTime.now()));

        when(probe.key()).thenReturn(probeKey);
        when(probe.isSourceCodeRelated()).thenReturn(true);
        when(probe.doApply(plugin, ctx)).thenReturn(result);

        when(probeService.getProbeContext(any(Plugin.class), any(UpdateCenter.class))).thenReturn(ctx);
        when(probeService.getProbes()).thenReturn(List.of(probe));
        when(pluginService.streamAll()).thenReturn(Stream.of(plugin));

        final ProbeEngine probeEngine = new ProbeEngine(probeService, pluginService, updateCenterService, gitHub, pluginDocumentationService);
        probeEngine.run();

        verify(probe).doApply(plugin, ctx);
        verify(plugin).addDetails(result);
    }

    @Test
    void shouldApplyProbeWithReleaseRequirementOnPluginWithNewReleaseAndPastResult() throws IOException {
        final String probeKey = "wiz";
        final Plugin plugin = mock(Plugin.class);
        final Probe probe = spy(Probe.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now());
        when(plugin.getDetails()).thenReturn(Map.of(probeKey, new ProbeResult(probeKey, "this is ok", ProbeResult.Status.SUCCESS, ZonedDateTime.now().minusDays(1), 1)));

        when(probe.getVersion()).thenReturn(1L);
        when(probe.key()).thenReturn(probeKey);
        when(probe.requiresRelease()).thenReturn(true);
        when(probe.doApply(plugin, ctx)).thenReturn(ProbeResult.success(probeKey, "This is also ok", 1));

        when(probeService.getProbeContext(any(Plugin.class), any(UpdateCenter.class))).thenReturn(ctx);
        when(probeService.getProbes()).thenReturn(List.of(probe));
        when(pluginService.streamAll()).thenReturn(Stream.of(plugin));

        final ProbeEngine probeEngine = new ProbeEngine(probeService, pluginService, updateCenterService, gitHub, pluginDocumentationService);
        probeEngine.run();

        verify(probe).doApply(eq(plugin), any(ProbeContext.class));
        verify(pluginService).saveOrUpdate(plugin);
    }

    @Test
    void shouldApplyProbeWithNoReleaseRequirementOnPluginWithPastResult() throws IOException {
        final String probeKey = "wiz";
        final Plugin plugin = mock(Plugin.class);
        final Probe probe = spy(Probe.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getDetails()).thenReturn(Map.of(
            probeKey,
            new ProbeResult(probeKey, "this is ok", ProbeResult.Status.SUCCESS, ZonedDateTime.now().minusDays(1), 1)
        ));

        when(probe.getVersion()).thenReturn(1L);
        when(probe.requiresRelease()).thenReturn(false);
        when(probe.doApply(plugin, ctx)).thenReturn(ProbeResult.success(probeKey, "This is also ok", 1));
        when(probe.key()).thenReturn(probeKey);

        when(probeService.getProbeContext(any(Plugin.class), any(UpdateCenter.class))).thenReturn(ctx);
        when(probeService.getProbes()).thenReturn(List.of(probe));
        when(pluginService.streamAll()).thenReturn(Stream.of(plugin));

        final ProbeEngine probeEngine = new ProbeEngine(probeService, pluginService, updateCenterService, gitHub, pluginDocumentationService);
        probeEngine.run();

        verify(probe).doApply(plugin, ctx);
        verify(pluginService).saveOrUpdate(plugin);
    }

    @Test
    void shouldSaveEvenErrors() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final Probe probe = spy(Probe.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(probe.doApply(plugin, ctx)).thenReturn(ProbeResult.error("foo", "bar", 1));

        when(probeService.getProbeContext(any(Plugin.class), any(UpdateCenter.class))).thenReturn(ctx);
        when(probeService.getProbes()).thenReturn(List.of(probe));
        when(pluginService.streamAll()).thenReturn(Stream.of(plugin));

        final ProbeEngine probeEngine = new ProbeEngine(probeService, pluginService, updateCenterService, gitHub, pluginDocumentationService);
        probeEngine.run();

        verify(plugin).addDetails(any(ProbeResult.class));
    }

    @Test
    void shouldBeAbleToGetPreviousContextResultInExecution() throws IOException {
        final Plugin plugin = spy(Plugin.class);
        final Probe probeOne = spy(Probe.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final Probe probeTwo = new Probe() {
            @Override
            protected ProbeResult doApply(Plugin plugin, ProbeContext ctx) {
                return plugin.getDetails().get("foo") != null ?
                    ProbeResult.success(key(), "This is also ok", this.getVersion()) :
                    ProbeResult.error(key(), "This cannot be validated", this.getVersion());
            }

            @Override
            public String key() {
                return "bar";
            }

            @Override
            public String getDescription() {
                return "description";
            }

            @Override
            public long getVersion() {
                return 1;
            }
        };

        when(probeOne.key()).thenReturn("foo");
        when(probeOne.doApply(plugin, ctx)).thenReturn(ProbeResult.success("foo", "This is ok", 1));

        when(probeService.getProbeContext(any(Plugin.class), any(UpdateCenter.class))).thenReturn(ctx);
        when(probeService.getProbes()).thenReturn(List.of(probeOne, probeTwo));
        when(pluginService.streamAll()).thenReturn(Stream.of(plugin));

        final ProbeEngine probeEngine = new ProbeEngine(probeService, pluginService, updateCenterService, gitHub, pluginDocumentationService);
        probeEngine.run();

        verify(plugin, times(2)).addDetails(any(ProbeResult.class));
        assertThat(plugin.getDetails().keySet()).containsExactlyInAnyOrder("foo", "bar");
    }

    @Test
    void shouldForceProbeExecutionWhenNewVersionOfTheProbe() throws IOException {
        final String probeKey = "foo";
        final long version = 1L;
        final ProbeResult newProbeResult = new ProbeResult(
            probeKey, "this is a message", ProbeResult.Status.SUCCESS, version + 1
        );

        final Plugin plugin = mock(Plugin.class);
        final Probe probe = spy(new Probe() {
            @Override
            protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
                return newProbeResult;
            }

            @Override
            public String key() {
                return probeKey;
            }

            @Override
            public String getDescription() {
                return "";
            }

            @Override
            public long getVersion() {
                return version + 1;
            }

            @Override
            protected boolean isSourceCodeRelated() {
                return true;
            }
        });
        final ProbeContext ctx = mock(ProbeContext.class);

        final ProbeResult previousResult = new ProbeResult(probeKey, "this is a message", ProbeResult.Status.SUCCESS, version);
        when(plugin.getDetails()).thenReturn(Map.of(
            probeKey, previousResult
        ));

        when(probeService.getProbeContext(any(Plugin.class), any(UpdateCenter.class))).thenReturn(ctx);
        when(probeService.getProbes()).thenReturn(List.of(probe));
        when(pluginService.streamAll()).thenReturn(Stream.of(plugin));

        final ProbeEngine probeEngine = new ProbeEngine(probeService, pluginService, updateCenterService, gitHub, pluginDocumentationService);
        probeEngine.run();

        verify(probe).doApply(plugin, ctx);
        verify(plugin).addDetails(newProbeResult);
    }

    @Test
    void shouldRequestUpdateCenterAndDocumentationUrlOnce() throws IOException {
        final Plugin p1 = mock(Plugin.class);
        final Plugin p2 = mock(Plugin.class);
        final Probe probe = mock(Probe.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(probe.apply(p1, ctx)).thenReturn(ProbeResult.success("foo", "this is fine", 1));
        when(probe.apply(p2, ctx)).thenReturn(ProbeResult.success("foo", "this is ok too", 1));

        when(probeService.getProbes()).thenReturn(List.of(probe));
        when(probeService.getProbeContext(any(Plugin.class), any(UpdateCenter.class))).thenReturn(ctx);
        when(pluginService.streamAll()).thenReturn(Stream.of(p1, p2));

        final ProbeEngine probeEngine = new ProbeEngine(probeService, pluginService, updateCenterService, gitHub, pluginDocumentationService);
        probeEngine.run();

        verify(pluginDocumentationService).fetchPluginDocumentationUrl();
        verify(updateCenterService).fetchUpdateCenter();
    }

    @Test
    void shouldNotPreventProbeExecutionWithNoSCM() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final Probe probe = spy(Probe.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(probe.key()).thenReturn("probe");
        when(probe.doApply(plugin, ctx)).thenReturn(ProbeResult.success("probe", "this is fine", 1));

        when(probeService.getProbes()).thenReturn(List.of(probe));
        when(probeService.getProbeContext(any(Plugin.class), any(UpdateCenter.class))).thenReturn(ctx);
        when(pluginService.streamAll()).thenReturn(Stream.of(plugin));

        final ProbeEngine probeEngine = new ProbeEngine(probeService, pluginService, updateCenterService, gitHub, pluginDocumentationService);
        probeEngine.run();

        verify(probe).doApply(plugin, ctx);
    }
}
