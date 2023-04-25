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

package io.jenkins.pluginhealth.scoring.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.spy;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PluginTest {
    @Test
    void shouldUpdateDetailsIfNoPreviousDetailsPresent() {
        final Plugin plugin = spy(Plugin.class);
        final ProbeResult probeResult = ProbeResult.success("foo", "this is a message");

        plugin.addDetails(probeResult);
        assertThat(plugin.getDetails()).hasSize(1);
        assertThat(plugin.getDetails()).containsEntry("foo", probeResult);
    }

    @Test
    void shouldUpdateDetailsIfPreviousDetailsPresentAndDifferent() {
        final Plugin plugin = spy(Plugin.class);
        final String probeKey = "foo";
        final String probeResultMessage = "this is a message";

        final ProbeResult previousProbeResult = new ProbeResult(probeKey, probeResultMessage, ResultStatus.FAILURE, ZonedDateTime.now().minusMinutes(10));
        final ProbeResult probeResult = new ProbeResult(probeKey, probeResultMessage, ResultStatus.SUCCESS, ZonedDateTime.now());

        plugin.addDetails(previousProbeResult);
        plugin.addDetails(probeResult);

        assertThat(plugin.getDetails()).hasSize(1);
        assertThat(plugin.getDetails())
            .containsExactly(entry(probeKey, probeResult));
    }

    @Test
    void shouldNotUpdateDetailsIfPreviousDetailsPresentButSame() {
        final Plugin plugin = spy(Plugin.class);
        final String probeKey = "foo";
        final String probeResultMessage = "this is a message";

        final ProbeResult previousProbeResult = new ProbeResult(probeKey, probeResultMessage, ResultStatus.SUCCESS, ZonedDateTime.now().minusMinutes(10));
        final ProbeResult probeResult = new ProbeResult(probeKey, probeResultMessage, ResultStatus.SUCCESS, ZonedDateTime.now());

        plugin.addDetails(previousProbeResult);
        plugin.addDetails(probeResult);

        assertThat(plugin.getDetails()).hasSize(1);
        assertThat(plugin.getDetails())
            .containsExactly(entry(probeKey, previousProbeResult));
    }

    @Test
    void shouldOverrideSuccessfulPreviousResultWithFailure() {
        final Plugin plugin = spy(Plugin.class);
        final String probeKey = "foo";
        final String probeResultMessage = "this is a message";

        final ProbeResult previousProbeResult = new ProbeResult(probeKey, probeResultMessage, ResultStatus.SUCCESS, ZonedDateTime.now().minusMinutes(10));
        final ProbeResult probeResult = new ProbeResult(probeKey, probeResultMessage, ResultStatus.FAILURE, ZonedDateTime.now());

        plugin.addDetails(previousProbeResult);
        plugin.addDetails(probeResult);

        assertThat(plugin.getDetails()).hasSize(1);
        assertThat(plugin.getDetails())
            .containsExactly(entry(probeKey, probeResult));
    }

    @Test
    void shouldRemoveEntryWhenNewStatusInError() {
        final Plugin plugin = spy(Plugin.class);
        final String probeKey = "foo";

        plugin.addDetails(ProbeResult.success(probeKey, ""));
        assertThat(plugin.getDetails()).hasSize(1);

        plugin.addDetails(ProbeResult.error(probeKey, ""));
        assertThat(plugin.getDetails()).isEmpty();
    }
}
