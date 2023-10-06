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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SecurityScanProbeTest extends AbstractProbeTest<SecurityScanProbe> {
    private Plugin plugin;
    private ProbeContext ctx;
    private SecurityScanProbe probe;

    @BeforeEach
    public void init() {
        plugin = mock(Plugin.class);
        ctx = mock(ProbeContext.class);
        probe = getSpy();
    }

    @Override
    SecurityScanProbe getSpy() {
        return spy(SecurityScanProbe.class);
    }

    @Test
    void shouldBeAbleToDetectRepositoryWithNoGitHubWorkflowConfigured() throws IOException {
        final Path repo = Files.createTempDirectory("foo");
        when(ctx.getScmRepository()).thenReturn(Optional.of(repo));

        assertThat(probe.apply(plugin, ctx))
            .isNotNull()
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(SecurityScanProbe.KEY, "Plugin has no GitHub Action configured.", probe.getVersion()));
    }

    @Test
    void shouldBeAbleToDetectRepositoryWithNoSecurityScanConfigured() throws IOException {
        final Path repo = Files.createTempDirectory("foo");
        Files.createDirectories(repo.resolve(".github/workflows"));
        when(ctx.getScmRepository()).thenReturn(Optional.of(repo));

        assertThat(probe.apply(plugin, ctx))
            .isNotNull()
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(SecurityScanProbe.KEY, "GitHub workflow security scan is not configured in the plugin.", probe.getVersion()));
    }

    @Test
    void shouldNotFindSecurityScanConfiguredInGitHubWorkflow() throws IOException {
        final Path repo = Files.createTempDirectory("foo");
        Path workflowPath = Files.createDirectories(repo.resolve(".github/workflows"));
        final Path workflowFile = Files.createFile(workflowPath.resolve("jenkins-security-scan.yaml"));

        Files.write(workflowFile, List.of(
            "name: Test Security Scan Job",
            "jobs:",
            "  security-scan-name:",
            "    uses: this-is-not-the-workflow-we-are-looking-for"
        ));
        when(ctx.getScmRepository()).thenReturn(Optional.of(repo));

        assertThat(probe.apply(plugin, ctx))
            .isNotNull()
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(SecurityScanProbe.KEY, "GitHub workflow security scan is not configured in the plugin.", probe.getVersion()));
    }

    @Test
    void shouldSucceedIfSecurityScanIsConfigured() throws IOException {
        final Path repo = Files.createTempDirectory("foo");
        Path workflowPath = Files.createDirectories(repo.resolve(".github/workflows"));
        final Path workflowFile = Files.createFile(workflowPath.resolve("jenkins-security-scan.yaml"));

        Files.write(workflowFile, List.of(
            "name: Test Security Scan Job",
            "jobs:",
            "  this-is-a-valid-security-scan:",
            "    uses: jenkins-infra/jenkins-security-scan/.github/workflows/jenkins-security-scan.yaml"
        ));
        when(ctx.getScmRepository()).thenReturn(Optional.of(repo));

        assertThat(probe.apply(plugin, ctx))
            .isNotNull()
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(SecurityScanProbe.KEY, "GitHub workflow security scan is configured in the plugin.", probe.getVersion()));
    }

    @Test
    void shouldSucceedToFindWorkflowEvenWithVersion() throws IOException {
        final Path repo = Files.createTempDirectory("foo");
        Path workflowPath = Files.createDirectories(repo.resolve(".github/workflows"));
        final Path workflowFile = Files.createFile(workflowPath.resolve("jenkins-security-scan.yaml"));

        Files.write(workflowFile, List.of(
            "name: Test Security Scan Job",
            "jobs:",
            "  security-scan:",
            "    uses: jenkins-infra/jenkins-security-scan/.github/workflows/jenkins-security-scan.yaml@v42"
        ));
        when(ctx.getScmRepository()).thenReturn(Optional.of(repo));

        assertThat(probe.apply(plugin, ctx))
            .isNotNull()
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(SecurityScanProbe.KEY, "GitHub workflow security scan is configured in the plugin.", probe.getVersion()));
    }
}
