package io.jenkins.pluginhealth.scoring.probes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LastCommitDateProbeTest {
    @Test
    public void shouldKeepScmAsKey() {
        assertThat(new LastCommitDateProbe().key()).isEqualTo("last-commit-date");
    }

    @Test
    public void shouldNotRequireRelease() {
        assertThat(new LastCommitDateProbe().requiresRelease()).isFalse();
    }

    @Test
    public void shouldBeExecutedAfterSCMLinkValidation() {
        assertThat(SCMLinkValidationProbe.ORDER).isLessThan(LastCommitDateProbe.ORDER);
    }

    @Test
    public void shouldReturnSuccessStatusOnValidSCM() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final LastCommitDateProbe probe = new LastCommitDateProbe();

        when(plugin.getDetails()).thenReturn(Map.of(SCMLinkValidationProbe.KEY, ProbeResult.success("scm", "The plugin SCM link is valid")));
        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/parameterized-trigger-plugin.git");
        when(ctx.getScmRepository()).thenReturn(Files.createTempDirectory(UUID.randomUUID().toString()));
        final ProbeResult r = probe.apply(plugin, ctx);

        assertThat(r.id()).isEqualTo("last-commit-date");
        assertThat(r.status()).isEqualTo(ResultStatus.SUCCESS);
    }

    @Test
    public void shouldReturnSuccessStatusOnValidSCMWithSubFolder() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final LastCommitDateProbe probe = new LastCommitDateProbe();

        when(plugin.getDetails()).thenReturn(Map.of(SCMLinkValidationProbe.KEY, ProbeResult.success("scm", "The plugin SCM link is valid")));
        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/aws-java-sdk-plugin/aws-java-sdk-logs");
        when(ctx.getScmRepository()).thenReturn(Files.createTempDirectory(UUID.randomUUID().toString()));
        final ProbeResult r = probe.apply(plugin, ctx);

        assertThat(r.id()).isEqualTo("last-commit-date");
        assertThat(r.status()).isEqualTo(ResultStatus.SUCCESS);
    }

    @Test
    public void shouldReturnFailureOnInvalidSCM() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final LastCommitDateProbe probe = new LastCommitDateProbe();

        when(plugin.getDetails()).thenReturn(Map.of(SCMLinkValidationProbe.KEY, ProbeResult.failure("scm", "The plugin SCM link is invalid")));
        final ProbeResult r = probe.apply(plugin, ctx);

        assertThat(r.status()).isEqualTo(ResultStatus.FAILURE);
    }

    @Test
    public void shouldFailToRunAsFirstProbe() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final LastCommitDateProbe probe = new LastCommitDateProbe();

        when(plugin.getDetails()).thenReturn(Map.of());
        final ProbeResult r = probe.apply(plugin, ctx);

        assertThat(r.status()).isEqualTo(ResultStatus.ERROR);
    }
}
