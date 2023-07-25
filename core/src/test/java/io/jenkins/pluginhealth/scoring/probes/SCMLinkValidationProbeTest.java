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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

class SCMLinkValidationProbeTest extends AbstractProbeTest<SCMLinkValidationProbe> {
    @Override
    SCMLinkValidationProbe getSpy() {
        return spy(SCMLinkValidationProbe.class);
    }

    @Test
    void shouldRequireRelease() {
        assertThat(getSpy().requiresRelease()).isTrue();
    }

    @Test
    void shouldRequirePluginToBeInUpdateCenter() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getDetails()).thenReturn(Map.of());
        final SCMLinkValidationProbe probe = getSpy();

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status")
            .isEqualTo(ProbeResult.error("scm", ""));
        verify(probe, never()).doApply(plugin, ctx);
    }

    @Test
    void shouldNotAcceptNullNorEmptyScm() {
        final Plugin p1 = mock(Plugin.class);
        final ProbeContext ctxP1 = mock(ProbeContext.class);
        final Plugin p2 = mock(Plugin.class);
        final ProbeContext ctxP2 = mock(ProbeContext.class);
        final SCMLinkValidationProbe probe = getSpy();

        when(p1.getDetails()).thenReturn(Map.of(
            UpdateCenterPluginPublicationProbe.KEY, ProbeResult.success(UpdateCenterPluginPublicationProbe.KEY, "")
        ));
        when(p1.getScm()).thenReturn(null);
        when(p2.getDetails()).thenReturn(Map.of(
            UpdateCenterPluginPublicationProbe.KEY, ProbeResult.success(UpdateCenterPluginPublicationProbe.KEY, "")
        ));
        when(p2.getScm()).thenReturn("");

        final ProbeResult r1 = probe.apply(p1, ctxP1);
        final ProbeResult r2 = probe.apply(p2, ctxP2);

        assertThat(r1.status()).isEqualTo(ResultStatus.ERROR);
        assertThat(r2.status()).isEqualTo(ResultStatus.ERROR);
        assertThat(r1.message()).isEqualTo("The plugin SCM link is empty");
    }

    @Test
    void shouldRecognizeIncorrectSCMUrl() {
        final Plugin p1 = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final SCMLinkValidationProbe probe = getSpy();

        when(p1.getScm()).thenReturn("this-is-not-correct");
        when(p1.getDetails()).thenReturn(Map.of(
            UpdateCenterPluginPublicationProbe.KEY, ProbeResult.success(UpdateCenterPluginPublicationProbe.KEY, "")
        ));
        final ProbeResult r1 = probe.apply(p1, ctx);

        assertThat(r1.status()).isEqualTo(ResultStatus.FAILURE);
        assertThat(r1.message()).isEqualTo("SCM link doesn't match GitHub plugin repositories");
    }

    @Test
    void shouldRecognizeCorrectGitHubUrl() throws IOException {
        final Plugin p1 = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final GitHub gh = mock(GitHub.class);
        final String repositoryName = "jenkinsci/mailer-plugin";

        when(p1.getScm()).thenReturn("https://github.com/" + repositoryName);
        when(p1.getDetails()).thenReturn(Map.of(
            UpdateCenterPluginPublicationProbe.KEY, ProbeResult.success(UpdateCenterPluginPublicationProbe.KEY, "")
        ));
        when(ctx.getGitHub()).thenReturn(gh);
        when(gh.getRepository(repositoryName)).thenReturn(new GHRepository());

        final SCMLinkValidationProbe probe = getSpy();
        final ProbeResult r1 = probe.apply(p1, ctx);

        assertThat(r1.status()).isEqualTo(ResultStatus.SUCCESS);
        assertThat(r1.message()).isEqualTo("The plugin SCM link is valid");
    }

    @Test
    void shouldRecognizeInvalidGitHubUrl() throws Exception {
        final Plugin p1 = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final GitHub gh = mock(GitHub.class);
        final String repositoryName = "jenkinsci/this-is-not-going-to-work";

        when(p1.getScm()).thenReturn("https://github.com/" + repositoryName);
        when(p1.getDetails()).thenReturn(Map.of(
            UpdateCenterPluginPublicationProbe.KEY, ProbeResult.success(UpdateCenterPluginPublicationProbe.KEY, "")
        ));
        when(ctx.getGitHub()).thenReturn(gh);
        when(gh.getRepository(repositoryName)).thenThrow(IOException.class);

        final SCMLinkValidationProbe probe = getSpy();
        final ProbeResult r1 = probe.apply(p1, ctx);

        assertThat(r1.status()).isEqualTo(ResultStatus.FAILURE);
        assertThat(r1.message()).isEqualTo("The plugin SCM link is invalid");
    }

    @Test
    void shouldReturnCorrectScmFolderPath() throws IOException {
        final Plugin p1 = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final GitHub gh = mock(GitHub.class);
        final String repositoryName = "jenkinsci/test-repo";

        Path parentDirectory = Files.createTempDirectory("jenkinsci");
        Path directoryPath = parentDirectory.resolve("test-repo");
        final Path tempDirectory1 = Files.createDirectories(directoryPath.resolve("nested-directory-1"));
        final Path pomFile1 = Files.createFile(tempDirectory1.resolve("pom.xml"));
        final Path tempDirectory2 = Files.createDirectories(directoryPath.resolve("nested-directory-2"));
        final Path pomFile2 = Files.createFile(tempDirectory2.resolve("pom.xml"));

        Files.write(pomFile1,
            """
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                      <modelVersion>4.0.0</modelVersion>
                     
                      <groupId>test-group</groupId>
                      <artifactId>test-repo</artifactId>
                      <packaging>pom</packaging>
                </project>                             
                """.getBytes()
        );

        Files.write(pomFile2,
            """
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                      <modelVersion>4.0.0</modelVersion>
                     
                      <groupId>test-group</groupId>
                      <artifactId>test-repo</artifactId>
                      <packaging>hpi</packaging>
                </project>                             
                """.getBytes()
        );

        when(p1.getScm()).thenReturn("https://github.com/" + repositoryName);
        when(p1.getDetails()).thenReturn(Map.of(
            UpdateCenterPluginPublicationProbe.KEY, ProbeResult.success(UpdateCenterPluginPublicationProbe.KEY, "")
        ));
        when(ctx.getGitHub()).thenReturn(gh);

        final SCMLinkValidationProbe probe = getSpy();
        final ProbeResult r1 = probe.apply(p1, ctx);

        assertThat(ctx.getScmFolderPath()).isEqualTo("test-repo");
        assertThat(r1.status()).isEqualTo(ResultStatus.SUCCESS);
        assertThat(r1.message()).isEqualTo("The plugin SCM link is valid");

    }
}
