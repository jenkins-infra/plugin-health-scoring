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

import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;
import io.jenkins.pluginhealth.scoring.model.updatecenter.Deprecation;
import io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin;
import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;

import hudson.util.VersionNumber;
import org.junit.jupiter.api.Test;

public class DeprecatedPluginProbeTest {
    @Test
    public void shouldNotRequireRelease() {
        assertThat(new DeprecatedPluginProbe().requiresRelease()).isFalse();
    }

    @Test
    public void shouldHaveStaticKey() {
        assertThat(new DeprecatedPluginProbe().key()).isEqualTo("deprecation");
    }

    @Test
    public void shouldBeAbleToDetectNonDeprecatedPlugin() {
        final io.jenkins.pluginhealth.scoring.model.Plugin plugin = mock(io.jenkins.pluginhealth.scoring.model.Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final DeprecatedPluginProbe probe = new DeprecatedPluginProbe();

        when(plugin.getName()).thenReturn("foo");
        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of("foo", new Plugin("foo", new VersionNumber("1.0"), "scm", ZonedDateTime.now().minusDays(1), Collections.emptyList(), 0, "", "main")),
            Map.of("bar", new Deprecation("find-the-reason-here")),
            Collections.emptyList()
        ));

        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result.status()).isEqualTo(ResultStatus.SUCCESS);
    }

    @Test
    public void shouldBeAbleToDetectDeprecatedPlugin() {
        final io.jenkins.pluginhealth.scoring.model.Plugin plugin = mock(io.jenkins.pluginhealth.scoring.model.Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final DeprecatedPluginProbe probe = new DeprecatedPluginProbe();

        when(plugin.getName()).thenReturn("foo");
        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of("foo", new Plugin("foo", new VersionNumber("1.0"), "scm", ZonedDateTime.now().minusDays(1), Collections.emptyList(), 0, "", "main")),
            Map.of("bar", new Deprecation("find-the-reason-here"), "foo", new Deprecation("this-is-the-reason")),
            Collections.emptyList()
        ));

        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result.status()).isEqualTo(ResultStatus.FAILURE);
        assertThat(result.message()).isEqualTo("this-is-the-reason");
    }

    @Test
    public void shouldBeAbleToDetectDeprecatedPluginFromLabels() {
        final io.jenkins.pluginhealth.scoring.model.Plugin plugin = mock(io.jenkins.pluginhealth.scoring.model.Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String pluginName = "foo";

        when(plugin.getName()).thenReturn(pluginName);
        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of(
                pluginName, new Plugin(pluginName, new VersionNumber("1.0"), "", ZonedDateTime.now(), List.of("deprecated"), 0, "2.361", "main")
            ),
            Map.of(),
            Collections.emptyList()
        ));

        final DeprecatedPluginProbe probe = new DeprecatedPluginProbe();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result)
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.failure(DeprecatedPluginProbe.KEY, "This plugin is marked as deprecated"));
    }

    @Test
    public void shouldSurviveIfPluginIsNotInUpdateCenter() {
        final io.jenkins.pluginhealth.scoring.model.Plugin plugin = mock(io.jenkins.pluginhealth.scoring.model.Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final String pluginName = "foo";

        when(plugin.getName()).thenReturn(pluginName);
        when(ctx.getUpdateCenter()).thenReturn(new UpdateCenter(
            Map.of(),
            Map.of(),
            Collections.emptyList()
        ));

        final DeprecatedPluginProbe probe = new DeprecatedPluginProbe();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result)
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.failure(DeprecatedPluginProbe.KEY, "This plugin is not in update-center"));
    }
}
