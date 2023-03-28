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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DependabotProbeTest {
    @Test
    void shouldNotRequireRelease() {
        assertThat(spy(DependabotProbe.class).requiresRelease()).isFalse();
    }

    @Test
    void shouldBeRelatedToCode() {
        assertThat(spy(DependabotProbe.class).isSourceCodeRelated()).isTrue();
    }


    @Test
    void shouldUseDependabotKey() {
        assertThat(spy(DependabotProbe.class).key()).isEqualTo("dependabot");
    }

    @Test
    void shouldHaveDescription() {
        assertThat(spy(DependabotProbe.class).getDescription()).isNotBlank();
    }

    @Test
    void shouldRequireValidSCM() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getDetails()).thenReturn(Map.of());

        final DependabotProbe probe = new DependabotProbe();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result.status()).isEqualTo(ResultStatus.ERROR);
    }

    @Test
    void shouldDetectMissingDependabotFile() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final DependabotProbe probe = new DependabotProbe();

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        final Path repo = Files.createTempDirectory("foo");
        when(ctx.getScmRepository()).thenReturn(repo);

        assertThat(probe.apply(plugin, ctx).status()).isEqualTo(ResultStatus.FAILURE);
    }

    @Test
    void shouldDetectDependabotFile() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final DependabotProbe probe = new DependabotProbe();

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        final Path repo = Files.createTempDirectory("foo");
        final Path github = Files.createDirectories(repo.resolve(".github"));

        Files.createFile(github.resolve("dependabot.yml"));

        when(ctx.getScmRepository()).thenReturn(repo);

        final ProbeResult result = probe.apply(plugin, ctx);
        assertThat(result.status()).isEqualTo(ResultStatus.SUCCESS);
    }
}
