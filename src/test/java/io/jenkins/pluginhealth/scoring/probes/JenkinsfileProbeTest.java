package io.jenkins.pluginhealth.scoring.probes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class JenkinsfileProbeTest {
    @Test
    public void shouldNotRequireRelease() {
        final JenkinsfileProbe jenkinsfileProbe = spy(JenkinsfileProbe.class);
        assertThat(jenkinsfileProbe.requiresRelease()).isFalse();
    }

    @Test
    public void shouldKeepUsingTheSameKey() {
        final JenkinsfileProbe jenkinsfileProbe = spy(JenkinsfileProbe.class);
        assertThat(jenkinsfileProbe.key()).isEqualTo("jenkinsfile");
    }

    @Test
    public void shouldCorrectlyDetectMissingJenkinsfile() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final JenkinsfileProbe probe = new JenkinsfileProbe();


        when(ctx.getScmRepository()).thenReturn(
            Files.createTempDirectory("foo")
        );

        assertThat(probe.apply(plugin, ctx).status()).isEqualTo(ResultStatus.FAILURE);
    }

    @Test
    public void shouldCorrectlyDetectJenkinsfile() throws IOException {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final JenkinsfileProbe probe = new JenkinsfileProbe();

        when(ctx.getScmRepository()).thenReturn(
            Files.createFile(
                Paths.get(Files.createTempDirectory("foo").toAbsolutePath().toString(), "Jenkinsfile")
            )
        );

        assertThat(probe.apply(plugin, ctx).status()).isEqualTo(ResultStatus.SUCCESS);
    }
}
