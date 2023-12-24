package io.jenkins.pluginhealth.scoring.probes;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

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

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        when(ctx.getScmRepository()).thenReturn(
            Files.createTempDirectory("foo")
        );
         assertThat(probe.apply(plugin, ctx).status()).isEqualTo(ResultStatus.FAILURE);
    }
}
