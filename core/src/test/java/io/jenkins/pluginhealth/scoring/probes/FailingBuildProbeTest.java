package io.jenkins.pluginhealth.scoring.probes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;

import org.junit.jupiter.api.Test;

public class FailingBuildProbeTest extends AbstractProbeTest<FailingBuildProbe> {

    @Override
    FailingBuildProbe getSpy() {
         return spy(FailingBuildProbe.class);
    }

    @Test
    void shouldCorrectlyDetectMissingJenkinsfile() throws IOException {
        final FailingBuildProbe probe = getSpy();
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final Path repo = Files.createTempDirectory("foo");
        when(ctx.getScmRepository()).thenReturn(Optional.of(repo));
        assertThat(probe.apply(plugin, ctx)).withFailMessage("No JenkinsFile found");
    }
}
