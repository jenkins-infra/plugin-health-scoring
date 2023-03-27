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

import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.junit.jupiter.api.Test;

class DocumentationMigrationProbeTest {
    @Test
    void shouldHaveKey() {
        assertThat(spy(DocumentationMigrationProbe.class).key()).isEqualTo("documentation-migration");
    }

    @Test
    void shouldHaveDescription() {
        assertThat(spy(DocumentationMigrationProbe.class).getDescription()).isNotBlank();
    }

    @Test
    void shouldRequiredRelease() {
        assertThat(spy(DocumentationMigrationProbe.class).requiresRelease()).isTrue();
    }

    @Test
    void shouldNotBeRelatedToSourceCodeModifications() {
        assertThat(spy(DocumentationMigrationProbe.class).isSourceCodeRelated()).isFalse();
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldRequireValidSCMLink() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getDetails()).thenReturn(
            Map.of(),
            Map.of(SCMLinkValidationProbe.KEY, ProbeResult.failure(SCMLinkValidationProbe.KEY, ""))
        );

        final DocumentationMigrationProbe probe = new DocumentationMigrationProbe();

        assertThat(probe.apply(plugin, ctx)).usingRecursiveComparison()
            .comparingOnlyFields("id", "status")
            .isEqualTo(ProbeResult.error(DocumentationMigrationProbe.KEY, ""));


        assertThat(probe.apply(plugin, ctx)).usingRecursiveComparison()
            .comparingOnlyFields("id", "status")
            .isEqualTo(ProbeResult.error(DocumentationMigrationProbe.KEY, ""));
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldNotRegisterWhenDocumentationListIfEmpty() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getName()).thenReturn("foo");
        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, "")
        ));
        when(ctx.getPluginDocumentationLinks()).thenReturn(
            Map.of(),
            Map.of("something-else", "not-what-we-are-looking-for")
        );

        final DocumentationMigrationProbe probe = new DocumentationMigrationProbe();

        assertThat(probe.apply(plugin, ctx)).usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.error(DocumentationMigrationProbe.KEY, "No link to documentation can be confirmed"));

        assertThat(probe.apply(plugin, ctx)).usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.failure(DocumentationMigrationProbe.KEY, "Plugin is not listed in documentation migration source"));
    }

    @Test
    void shouldBeAbleToDetectNotMigratedDocumentation() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        final String pluginName = "foo";

        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/foo-plugin");
        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, "")
        ));
        when(ctx.getPluginDocumentationLinks()).thenReturn(
            Map.of(pluginName, "https://wiki.jenkins-ci.org/DISPLAY/foo-plugin")
        );

        final DocumentationMigrationProbe probe = new DocumentationMigrationProbe();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result).usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.failure(DocumentationMigrationProbe.KEY, "Documentation is not located in the plugin repository"));
    }

    @Test
    void shouldBeAbleToDetectMigratedDocumentation() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        final String pluginName = "foo";

        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/foo-plugin");
        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, "")
        ));
        when(ctx.getPluginDocumentationLinks()).thenReturn(
            Map.of(pluginName, "https://github.com/jenkinsci/foo-plugin")
        );

        final DocumentationMigrationProbe probe = new DocumentationMigrationProbe();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result).usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(DocumentationMigrationProbe.KEY, "Documentation is located in the plugin repository"));
    }
}
