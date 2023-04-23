package io.jenkins.pluginhealth.scoring.probes;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class HasUnreleasedProductionChangesProbeTest extends AbstractProbeTest<HasUnreleasedProductionChangesProbe> {
    @Override
    HasUnreleasedProductionChangesProbe getSpy() {
        return spy(HasUnreleasedProductionChangesProbe.class);
    }

    @Test
    void shouldBeExecutedAfterLasCommitDateProbe() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final HasUnreleasedProductionChangesProbe probe = getSpy();

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status")
            .isEqualTo(ProbeResult.error(HasUnreleasedProductionChangesProbe.KEY, ""));
        verify(probe, never()).doApply(plugin, ctx);
    }

    @Test
    void shouldCheckIfTheUnreleasedCommitsExist() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final HasUnreleasedProductionChangesProbe probe = getSpy();

        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/git-plugin/tree/master/src");
        when(ctx.getScmRepository()).thenReturn(Files.createTempDirectory(UUID.randomUUID().toString()));
        final ProbeResult probeResult = probe.apply(plugin, ctx);

        assertThat(probeResult.id()).isEqualTo("unreleased-production-changes");
        assertThat(probeResult.status()).isEqualTo(ResultStatus.SUCCESS);

        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/git-plugin/blob/master/pom.xml");
        when(ctx.getScmRepository()).thenReturn(Files.createTempDirectory(UUID.randomUUID().toString()));
        final ProbeResult probeResult2 = probe.apply(plugin, ctx);

        assertThat(probeResult2.id()).isEqualTo("unreleased-production-changes");
        assertThat(probeResult2.status()).isEqualTo(ResultStatus.SUCCESS);

    }


}
