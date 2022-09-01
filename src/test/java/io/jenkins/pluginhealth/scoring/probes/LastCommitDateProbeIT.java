package io.jenkins.pluginhealth.scoring.probes;

import java.io.IOException;
import java.time.ZonedDateTime;

import io.jenkins.pluginhealth.scoring.AbstractDBContainerTest;
import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class LastCommitDateProbeIT extends AbstractDBContainerTest {

    @Test
    public void shouldReturnSuccessStatusOnValidSCM() throws IOException {
        final Plugin plugin = new Plugin("parameterized-trigger", "https://github.com/jenkinsci/parameterized-trigger-plugin.git", ZonedDateTime.now())
            .addDetails(ProbeResult.success("scm", "The plugin SCM link is valid"));
        final LastCommitDateProbe probe = new LastCommitDateProbe();

        final ProbeResult r = probe.apply(plugin);

        assertThat(r.id()).isEqualTo("last-commit-date");
        assertThat(r.status()).isEqualTo(ResultStatus.SUCCESS);
    }
}
