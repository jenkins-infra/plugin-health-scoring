package io.jenkins.pluginhealth.scoring.probes;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

public class FailingBuildProbeTest extends AbstractProbeTest<FailingBuildProbe> {
    private FailingBuildProbe probe;
    private Plugin plugin = mock(Plugin.class);
    private ProbeContext ctx = mock(ProbeContext.class);

    @BeforeEach
    public void init() {
        plugin = mock(Plugin.class);
        ctx = mock(ProbeContext.class);
        probe = getSpy();
    }

    @Override
    FailingBuildProbe getSpy() {
         return spy(FailingBuildProbe.class);
    }

    @Test
    void shouldCorrectlyDetectMissingJenkinsfile() throws IOException {
        final Path repo = Files.createTempDirectory("foo");
        when(ctx.getScmRepository()).thenReturn(Optional.of(repo));
        assertThat(probe.apply(plugin,ctx)).withFailMessage("No JenkinsFile found");
    }
}
