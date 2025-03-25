package io.jenkins.pluginhealth.scoring.probes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ProbeTest {
    @TempDir
    public File tempDir;

    @Test
    void shouldBeExecutedWithNoPreviousResult() {
        final Probe probe = spy(Probe.class);
        final Plugin plugin = mock(Plugin.class);

        when(probe.key()).thenReturn("key");

        when(plugin.getDetails()).thenReturn(Map.of());

        assertThat(probe.isApplicable(plugin, mock(ProbeContext.class))).isTrue();
    }

    @Test
    void shouldBeExecutedWithNewVersion() {
        final Probe probe = spy(Probe.class);
        final Plugin plugin = mock(Plugin.class);

        when(probe.key()).thenReturn("key");
        when(probe.getVersion()).thenReturn(2L);

        when(plugin.getDetails()).thenReturn(Map.of(
            "key", ProbeResult.success("key", "", 1L)
        ));

        assertThat(probe.isApplicable(plugin, mock(ProbeContext.class))).isTrue();
    }

    @Test
    void shouldBeExecutedWithPreviousInError() {
        final Probe probe = spy(Probe.class);
        final Plugin plugin = mock(Plugin.class);

        when(probe.key()).thenReturn("key");
        when(probe.getVersion()).thenReturn(1L);

        when(plugin.getDetails()).thenReturn(Map.of(
            "key", ProbeResult.error("key", "", 1L)
        ));

        assertThat(probe.isApplicable(plugin, mock(ProbeContext.class))).isTrue();
    }

    @Test
    void shouldBeExecutedWhenDependingOnRepository() {
        final Probe probe = spy(Probe.class);
        final Plugin plugin = mock(Plugin.class);

        when(probe.key()).thenReturn("key");
        when(probe.getVersion()).thenReturn(1L);
        when(probe.isSourceCodeRelated()).thenReturn(false);
        when(probe.requiresRelease()).thenReturn(false);

        when(plugin.getDetails()).thenReturn(Map.of(
            "key", ProbeResult.success("key", "", 1L)
        ));

        assertThat(probe.isApplicable(plugin, mock(ProbeContext.class))).isTrue();
    }

    @Test
    void shouldBeExecutedWithNewRelease() {
        final Probe probe = spy(Probe.class);
        final Plugin plugin = mock(Plugin.class);

        when(probe.key()).thenReturn("key");
        when(probe.getVersion()).thenReturn(1L);
        when(probe.isSourceCodeRelated()).thenReturn(false);
        when(probe.requiresRelease()).thenReturn(true);

        when(plugin.getDetails()).thenReturn(Map.of(
            "key", ProbeResult.success("key", "", 1L)
        ));
        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().plusDays(1));

        assertThat(probe.isApplicable(plugin, mock(ProbeContext.class))).isTrue();
    }

    @Test
    void shouldNotBeExecutedWithNoSCM() {
        final Probe probe = spy(Probe.class);
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(probe.key()).thenReturn("key");
        when(probe.getVersion()).thenReturn(1L);
        when(probe.isSourceCodeRelated()).thenReturn(true);
        when(probe.requiresRelease()).thenReturn(true);

        when(plugin.getDetails()).thenReturn(Map.of(
            "key", ProbeResult.success("key", "", 1L)
        ));
        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusDays(1));

        when(ctx.getScmRepository()).thenReturn(Optional.empty());

        assertThat(probe.isApplicable(plugin, ctx)).isFalse();
    }

    @Test
    void shouldBeExecutedWhenCodeRelatedAndCodeChanged() {
        final Probe probe = spy(Probe.class);
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(probe.key()).thenReturn("key");
        when(probe.getVersion()).thenReturn(1L);
        when(probe.isSourceCodeRelated()).thenReturn(true);
        when(probe.requiresRelease()).thenReturn(false);

        when(plugin.getDetails()).thenReturn(Map.of(
            "key", ProbeResult.success("key", "", 1L)
        ));
        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusDays(1));

        when(ctx.getScmRepository()).thenReturn(Optional.of(tempDir.toPath()));
        when(ctx.getLastCommitDate()).thenReturn(Optional.of(ZonedDateTime.now().plusDays(1)));

        assertThat(probe.isApplicable(plugin, ctx)).isTrue();
    }

    @Test
    void shouldNotBeExecutedWhenCodeRelatedButNoRepository() {
        final Probe probe = spy(Probe.class);
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(probe.key()).thenReturn("key");
        when(probe.getVersion()).thenReturn(1L);
        when(probe.isSourceCodeRelated()).thenReturn(false);
        when(probe.requiresRelease()).thenReturn(true);

        when(plugin.getDetails()).thenReturn(Map.of(
            "key", ProbeResult.success("key", "", 1L)
        ));
        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusDays(1));

        when(ctx.getScmRepository()).thenReturn(Optional.of(tempDir.toPath()));
        when(ctx.getLastCommitDate()).thenReturn(Optional.of(ZonedDateTime.now().plusDays(1)));

        assertThat(probe.isApplicable(plugin, ctx)).isFalse();
    }

    @Test
    void shouldBeExecutedWhenCodeRelatedButNoLastCommit() {
        final Probe probe = spy(Probe.class);
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(probe.key()).thenReturn("key");
        when(probe.getVersion()).thenReturn(1L);
        when(probe.isSourceCodeRelated()).thenReturn(true);
        when(probe.requiresRelease()).thenReturn(true);

        when(plugin.getDetails()).thenReturn(Map.of(
            "key", ProbeResult.success("key", "", 1L)
        ));
        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusDays(1));

        when(ctx.getScmRepository()).thenReturn(Optional.of(tempDir.toPath()));
        when(ctx.getLastCommitDate()).thenReturn(Optional.empty());

        assertThat(probe.isApplicable(plugin, ctx)).isTrue();
    }

    @Test
    void shouldNotBeExecutedWhenCodeRelatedAndLastExecutedAfterLastCommit() {
        final Probe probe = spy(Probe.class);
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(probe.key()).thenReturn("key");
        when(probe.getVersion()).thenReturn(1L);
        when(probe.isSourceCodeRelated()).thenReturn(true);
        when(probe.requiresRelease()).thenReturn(true);

        when(plugin.getDetails()).thenReturn(Map.of(
            "key", ProbeResult.success("key", "", 1L)
        ));
        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusDays(1));

        when(ctx.getScmRepository()).thenReturn(Optional.of(tempDir.toPath()));
        when(ctx.getLastCommitDate()).thenReturn(Optional.of(ZonedDateTime.now().minusDays(1)));

        assertThat(probe.isApplicable(plugin, ctx)).isFalse();
    }
}
