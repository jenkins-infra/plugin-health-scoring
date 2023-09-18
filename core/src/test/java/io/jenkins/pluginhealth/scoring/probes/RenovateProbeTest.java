package io.jenkins.pluginhealth.scoring.probes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.junit.jupiter.api.Test;

public class RenovateProbeTest extends AbstractProbeTest<RenovateProbe> {
    @Override
    RenovateProbe getSpy() {
        return spy(RenovateProbe.class);
    }

    @Test
    void shouldNotRequireRelease() {
        assertThat(getSpy().requiresRelease()).isFalse();
    }

    @Test
    void shouldBeRelatedToCode() {
        assertThat(getSpy().isSourceCodeRelated()).isTrue();
    }

    @Test
    void shouldSurvivePluginWithoutLocalRepository() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getName()).thenReturn("foo");
        when(ctx.getScmRepository()).thenReturn(Optional.empty());

        final RenovateProbe probe = getSpy();
        assertThat(probe.apply(plugin, ctx))
            .isNotNull()
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.error(RenovateProbe.KEY, "There is no local repository for plugin " + plugin.getName() + ".", probe.getVersion()));
    }

    @Test
    void shouldDetectMissingGitHubActionFolder() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final RenovateProbe probe = getSpy();

        final Path repo = Files.createTempDirectory("foo");
        when(ctx.getScmRepository()).thenReturn(Optional.of(repo));

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.success(RenovateProbe.KEY, "No GitHub configuration folder found.", probe.getVersion()));
    }

    @Test
    void shouldDetectMissingRenovateFile() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final RenovateProbe probe = getSpy();

        final Path repo = Files.createTempDirectory("foo");
        Files.createDirectory(repo.resolve(".github"));
        when(ctx.getScmRepository()).thenReturn(Optional.of(repo));

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.success(RenovateProbe.KEY, "renovate is not configured.", probe.getVersion()));
    }

    @Test
    void shouldDetectRenovateFile() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final RenovateProbe probe = getSpy();

        final Path repo = Files.createTempDirectory("foo");
        final Path github = Files.createDirectories(repo.resolve(".github"));

        Files.createFile(github.resolve("renovate.json"));
        when(ctx.getScmRepository()).thenReturn(Optional.of(repo));

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.success(RenovateProbe.KEY, "renovate is configured.", probe.getVersion()));
    }
}
