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
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LastCommitDateProbeTest {
    @Test
    void shouldKeepScmAsKey() {
        assertThat(new LastCommitDateProbe().key()).isEqualTo("last-commit-date");
    }

    @Test
    void shouldNotRequireRelease() {
        assertThat(new LastCommitDateProbe().requiresRelease()).isFalse();
    }

    @Test
    void shouldNotBeRelatedToSourceCode() {
        assertThat(new LastCommitDateProbe().isSourceCodeRelated()).isFalse();
    }

    @Test
    void shouldBeExecutedAfterSCMLinkValidation() {
        assertThat(SCMLinkValidationProbe.ORDER).isLessThan(LastCommitDateProbe.ORDER);
    }

    @Test
    void shouldReturnSuccessStatusOnValidSCM() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final LastCommitDateProbe probe = new LastCommitDateProbe();

        when(plugin.getDetails()).thenReturn(Map.of(SCMLinkValidationProbe.KEY, ProbeResult.success("scm", "The plugin SCM link is valid")));
        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/parameterized-trigger-plugin.git");
        when(ctx.getScmRepository()).thenReturn(Files.createTempDirectory(UUID.randomUUID().toString()));
        final ProbeResult r = probe.apply(plugin, ctx);

        assertThat(r.id()).isEqualTo("last-commit-date");
        assertThat(r.status()).isEqualTo(ResultStatus.SUCCESS);
    }

    @Test
    void shouldReturnSuccessStatusOnValidSCMWithSubFolder() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final LastCommitDateProbe probe = new LastCommitDateProbe();

        when(plugin.getDetails()).thenReturn(Map.of(SCMLinkValidationProbe.KEY, ProbeResult.success("scm", "The plugin SCM link is valid")));
        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/aws-java-sdk-plugin/aws-java-sdk-logs");
        when(ctx.getScmRepository()).thenReturn(Files.createTempDirectory(UUID.randomUUID().toString()));
        final ProbeResult r = probe.apply(plugin, ctx);

        assertThat(r.id()).isEqualTo("last-commit-date");
        assertThat(r.status()).isEqualTo(ResultStatus.SUCCESS);
    }

    @Test
    void shouldReturnFailureOnInvalidSCM() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final LastCommitDateProbe probe = new LastCommitDateProbe();

        when(plugin.getDetails()).thenReturn(Map.of(SCMLinkValidationProbe.KEY, ProbeResult.failure("scm", "The plugin SCM link is invalid")));
        final ProbeResult r = probe.apply(plugin, ctx);

        assertThat(r.status()).isEqualTo(ResultStatus.FAILURE);
    }

    @Test
    void shouldFailToRunAsFirstProbe() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final LastCommitDateProbe probe = new LastCommitDateProbe();

        when(plugin.getDetails()).thenReturn(Map.of());
        final ProbeResult r = probe.apply(plugin, ctx);

        assertThat(r.status()).isEqualTo(ResultStatus.ERROR);
    }
}
