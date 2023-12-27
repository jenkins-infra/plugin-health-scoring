package io.jenkins.pluginhealth.scoring.probes;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

public class FailingBuildProbeTest extends AbstractProbeTest<FailingBuildProbe> {

    @Override
    FailingBuildProbe getSpy() {
         return spy(FailingBuildProbe.class);
    }

    @Test
    void shouldCorrectlyDetectMissingJenkinsfile() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final FailingBuildProbe probe = getSpy();

        final Path repo = Files.createTempDirectory("foo");
        when(ctx.getScmRepository()).thenReturn(Optional.of(repo));

        assertThat(probe.apply(plugin, ctx))
            .isNotNull()
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status", "message")
            .isEqualTo(ProbeResult.success(JenkinsfileProbe.KEY, "No Jenkinsfile found", probe.getVersion()));
    }
}
