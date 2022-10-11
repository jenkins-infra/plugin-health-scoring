package io.jenkins.pluginhealth.scoring.probes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentationLocationProbeTest {
    @Test
    public void shouldRequireRelease() {
        final DocumentationLocationProbe probe = spy(DocumentationLocationProbe.class);
        assertThat(probe.requiresRelease()).isTrue();
    }

    @Test
    public void shouldUseDocumentationKey() {
        final DocumentationLocationProbe probe = spy(DocumentationLocationProbe.class);
        assertThat(probe.key()).isEqualTo("documentation");
    }

    @Test
    public void shouldHaveDescription() {
        final DocumentationLocationProbe probe = spy(DocumentationLocationProbe.class);
        assertThat(probe.getDescription()).isNotBlank();
    }

    // TODO same for gradle
    @Test
    public void shouldValidateCompletedMigrationOnMaven() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final DocumentationLocationProbe probe = new DocumentationLocationProbe();

        final String pluginRepositoryUrl = "this-is-the-url";
        when(plugin.getScm()).thenReturn(pluginRepositoryUrl);
        final Path repository = Files.createTempDirectory("boo");
        Files.createFile(repository.resolve("README.md"));
        final Path pom = Files.createFile(repository.resolve("pom.xml"));
        Files.write(pom, List.of(
            "<project>","<url>",pluginRepositoryUrl, "</url>", "</project>"
        ), StandardCharsets.UTF_8);

        when(ctx.getScmRepository()).thenReturn(repository);

        final ProbeResult result = probe.apply(plugin, ctx);
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(ResultStatus.SUCCESS);
    }

    @Test
    public void shouldInvalidateRepositoryWithMissingREADME() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final DocumentationLocationProbe probe = new DocumentationLocationProbe();

        final String pluginRepositoryUrl = "this-is-the-url";
        final Path repository = Files.createTempDirectory("boo");
        final Path pom = Files.createFile(repository.resolve("pom.xml"));
        Files.write(pom, List.of(
            "<project>","<url>",pluginRepositoryUrl, "</url>", "</project>"
        ), StandardCharsets.UTF_8);

        when(ctx.getScmRepository()).thenReturn(repository);

        final ProbeResult result = probe.apply(plugin, ctx);
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(ResultStatus.FAILURE);
        assertThat(result.message()).isEqualTo("The plugin has no README");
    }

    @Test
    public void shouldInvalidateRepositoryWithMissingUrl() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final DocumentationLocationProbe probe = new DocumentationLocationProbe();

        final String pluginRepositoryUrl = "this-is-the-url";
        when(plugin.getScm()).thenReturn(pluginRepositoryUrl);
        final Path repository = Files.createTempDirectory("boo");
        Files.createFile(repository.resolve("README.md"));
        final Path pom = Files.createFile(repository.resolve("pom.xml"));
        Files.write(pom, List.of(
            "<project>", "</project>"
        ), StandardCharsets.UTF_8);

        when(ctx.getScmRepository()).thenReturn(repository);

        final ProbeResult result = probe.apply(plugin, ctx);
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(ResultStatus.FAILURE);
        assertThat(result.message()).isEqualTo("The plugin documentation was not migrated");
    }
}
