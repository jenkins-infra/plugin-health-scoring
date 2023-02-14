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

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ContributingGuidelinesProbeTest {
    @Test
    public void shouldNotRequireRelease() {
        final ContributingGuidelinesProbe contributingGuidelinesProbe = spy(ContributingGuidelinesProbe.class);
        assertThat(contributingGuidelinesProbe.requiresRelease()).isFalse();
    }

    @Test
    public void shouldKeepUsingTheSameKey() {
        final ContributingGuidelinesProbe contributingGuidelinesProbe = spy(ContributingGuidelinesProbe.class);
        assertThat(contributingGuidelinesProbe.key()).isEqualTo("contributing-guidelines");
    }

    @Test
    public void shouldCorrectlyDetectMissingContributingGuidelines() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final ContributingGuidelinesProbe probe = new ContributingGuidelinesProbe();

        when(plugin.getDetails()).thenReturn(Map.of(
                SCMLinkValidationProbe.KEY, new ProbeResult(SCMLinkValidationProbe.KEY, "", ResultStatus.SUCCESS,
                        ZonedDateTime.now().minusMinutes(5))));
        when(ctx.getScmRepository()).thenReturn(
                Files.createTempDirectory("foo"));

        assertThat(probe.apply(plugin, ctx).status()).isEqualTo(ResultStatus.FAILURE);
    }

    @Test
    public void shouldCorrectlyDetectContributingGuidelines() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final ContributingGuidelinesProbe probe = new ContributingGuidelinesProbe();

        when(plugin.getDetails()).thenReturn(Map.of(
                SCMLinkValidationProbe.KEY, new ProbeResult(SCMLinkValidationProbe.KEY, "", ResultStatus.SUCCESS,
                        ZonedDateTime.now().minusMinutes(5))));
        when(ctx.getScmRepository()).thenReturn(
                Files.createFile(
                        Paths.get(Files.createTempDirectory("foo").toAbsolutePath().toString(), "CONTRIBUTING.md")));

        assertThat(probe.apply(plugin, ctx).status()).isEqualTo(ResultStatus.SUCCESS);
    }
}
