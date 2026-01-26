/*
 * MIT License
 *
 * Copyright (c) 2023-2026 Jenkins Infra
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
import java.nio.file.Path;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ParentPomVersionProbeTest {

    @Test
    void shouldExtractParentPomVersion(@TempDir Path temp) throws IOException {
        // Given: A pom.xml with parent version 4.80
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <parent>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>4.80</version>
                    <relativePath />
                </parent>
                <artifactId>test-plugin</artifactId>
                <version>1.0.0-SNAPSHOT</version>
            </project>
            """;

        Path pomFile = temp.resolve("pom.xml");
        Files.writeString(pomFile, pomContent);

        // When: Probe is applied
        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("test-plugin");

        ProbeContext context = mock(ProbeContext.class);
        when(context.getScmRepository()).thenReturn(Optional.of(temp));

        ParentPomVersionProbe probe = new ParentPomVersionProbe();
        ProbeResult result = probe.apply(plugin, context);

        // Then: Result should be success with version
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(ProbeResult.Status.SUCCESS);
        assertThat(result.message()).isEqualTo("4.80");
    }

    @Test
    void shouldHandleMissingPomFile(@TempDir Path temp) {
        // Given: No pom.xml in repository
        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("test-plugin");

        ProbeContext context = mock(ProbeContext.class);
        when(context.getScmRepository()).thenReturn(Optional.of(temp));

        // When: Probe is applied
        ParentPomVersionProbe probe = new ParentPomVersionProbe();
        ProbeResult result = probe.apply(plugin, context);

        // Then: Should return error
        assertThat(result.status()).isEqualTo(ProbeResult.Status.ERROR);
        assertThat(result.message()).asString().contains("pom.xml not found");
    }

    @Test
    void shouldHandleNoParentPom(@TempDir Path temp) throws IOException {
        // Given: A pom.xml without parent
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <groupId>test</groupId>
                <artifactId>test-plugin</artifactId>
                <version>1.0.0</version>
            </project>
            """;

        Path pomFile = temp.resolve("pom.xml");
        Files.writeString(pomFile, pomContent);

        // When: Probe is applied
        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("test-plugin");

        ProbeContext context = mock(ProbeContext.class);
        when(context.getScmRepository()).thenReturn(Optional.of(temp));

        ParentPomVersionProbe probe = new ParentPomVersionProbe();
        ProbeResult result = probe.apply(plugin, context);

        // Then: Should return error
        assertThat(result.status()).isEqualTo(ProbeResult.Status.ERROR);
        assertThat(result.message()).asString().contains("No parent POM found");
    }

    @Test
    void shouldHandleNoLocalRepository() {
        // Given: No local repository available
        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("test-plugin");

        ProbeContext context = mock(ProbeContext.class);
        when(context.getScmRepository()).thenReturn(Optional.empty());

        // When: Probe is applied
        ParentPomVersionProbe probe = new ParentPomVersionProbe();
        ProbeResult result = probe.apply(plugin, context);

        // Then: Should return error
        assertThat(result.status()).isEqualTo(ProbeResult.Status.ERROR);
        assertThat(result.message()).asString().contains("no local repository");
    }

    @Test
    void shouldHandleMultilineParentBlock(@TempDir Path temp) throws IOException {
        // Given: Parent block with formatting/whitespace
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <parent>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>plugin</artifactId>
                    <version>
                        4.75
                    </version>
                    <relativePath />
                </parent>
                <artifactId>test-plugin</artifactId>
            </project>
            """;

        Path pomFile = temp.resolve("pom.xml");
        Files.writeString(pomFile, pomContent);

        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("test-plugin");

        ProbeContext context = mock(ProbeContext.class);
        when(context.getScmRepository()).thenReturn(Optional.of(temp));

        // When: Probe is applied
        ParentPomVersionProbe probe = new ParentPomVersionProbe();
        ProbeResult result = probe.apply(plugin, context);

        // Then: Should handle whitespace correctly
        assertThat(result.status()).isEqualTo(ProbeResult.Status.SUCCESS);
        assertThat(result.message()).isEqualTo("4.75");
    }

    @Test
    void shouldReturnCorrectKey() {
        ParentPomVersionProbe probe = new ParentPomVersionProbe();
        assertThat(probe.key()).isEqualTo("parent-pom-version");
    }

    @Test
    void shouldReturnDescription() {
        ParentPomVersionProbe probe = new ParentPomVersionProbe();
        assertThat(probe.getDescription()).isNotBlank().contains("parent POM version");
    }

    @Test
    void shouldBeSourceCodeRelated() {
        ParentPomVersionProbe probe = new ParentPomVersionProbe();
        assertThat(probe.isSourceCodeRelated()).isTrue();
    }

    @Test
    void shouldHaveVersionOne() {
        ParentPomVersionProbe probe = new ParentPomVersionProbe();
        assertThat(probe.getVersion()).isEqualTo(1);
    }
}
