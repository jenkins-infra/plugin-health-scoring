package io.jenkins.pluginhealth.scoring.probes;

import java.time.ZonedDateTime;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.jgit.api.errors.GitAPIException;
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
    public void shouldReturnSuccessStatusOnValidSCM() {
        final Plugin plugin = new Plugin("parameterized-trigger", "https://github.com/jenkinsci/parameterized-trigger-plugin.git", ZonedDateTime.now())
            .addDetails(ProbeResult.success("scm", "The plugin SCM link is valid"));
        final LastCommitDateProbe probe = new LastCommitDateProbe();

        final ProbeResult r = probe.apply(plugin);

        assertThat(r.id()).isEqualTo("last-commit-date");
        assertThat(r.status()).isEqualTo(ResultStatus.SUCCESS);
    }

    @Test
    public void shouldReturnFailureOnInvalidSCM() throws GitAPIException {
        final Plugin plugin = new Plugin("parameterized-trigger", "https://github.com/jenkinsci/random-repo.git", ZonedDateTime.now())
            .addDetails(ProbeResult.failure("scm", "The plugin SCM link is invalid"));
        final LastCommitDateProbe probe = new LastCommitDateProbe();

        final ProbeResult r = probe.apply(plugin);

        assertThat(r.status()).isEqualTo(ResultStatus.FAILURE);
    }

    @Test
    public void shouldFailToRunAsFirstProbe() {
        final Plugin plugin = new Plugin("parameterized-trigger", "https://github.com/jenkinsci/random-repo.git", ZonedDateTime.now());
        final LastCommitDateProbe probe = new LastCommitDateProbe();

        final ProbeResult r = probe.apply(plugin);

        assertThat(r.status()).isEqualTo(ResultStatus.ERROR);
    }

}
