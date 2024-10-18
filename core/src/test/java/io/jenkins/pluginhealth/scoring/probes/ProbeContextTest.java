package io.jenkins.pluginhealth.scoring.probes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;

import org.junit.jupiter.api.Test;

public class ProbeContextTest {
    @Test
    void shouldBeAbleToReturnCorrectPluginRepositoryName() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final UpdateCenter uc = mock(UpdateCenter.class);

        when(plugin.getScm()).thenReturn("https://github.com/jenkinsci/git-client-plugin");

        final ProbeContext ctx = new ProbeContext(plugin, uc);
        assertThat(ctx.getRepositoryName()).isEqualTo(Optional.of("jenkinsci/git-client-plugin"));
    }
}
