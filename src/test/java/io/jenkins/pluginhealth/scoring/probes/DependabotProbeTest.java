package io.jenkins.pluginhealth.scoring.probes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DependabotProbeTest {
    @Test
    public void shouldNotRequireRelease() {
        final DependabotProbe probe = spy(DependabotProbe.class);
        assertThat(probe.requiresRelease()).isFalse();
    }

    @Test
    public void shouldUseDependabotKey() {
        final DependabotProbe probe = spy(DependabotProbe.class);
        assertThat(probe.key()).isEqualTo("dependabot");
    }

    @Test
    public void shouldDetectMissingDependabotFile() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final DependabotProbe probe = new DependabotProbe();

        final Path repo = Files.createTempDirectory("foo");
        when(ctx.getScmRepository()).thenReturn(repo);

        assertThat(probe.apply(plugin, ctx).status()).isEqualTo(ResultStatus.FAILURE);
    }

    @Test
    public void shouldDetectDependabotFile() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final DependabotProbe probe = new DependabotProbe();

        final Path repo = Files.createTempDirectory("foo");
        final Path github = Files.createDirectories(repo.resolve(".github"));

        Files.createFile(github.resolve("dependabot.yml"));

        when(ctx.getScmRepository()).thenReturn(repo);

        final ProbeResult result = probe.apply(plugin, ctx);
        assertThat(result.status()).isEqualTo(ResultStatus.SUCCESS);
    }
}
