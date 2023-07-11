package io.jenkins.pluginhealth.scoring.probes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;

import org.junit.jupiter.api.Test;

public class RenovateProbeTest extends AbstractProbeTest<RenovateProbe> {
    @Override
    RenovateProbe getSpy() {
        return spy(new RenovateProbe("renovate"));
    }

    @Test
    void shouldNotRequireRelease() {
        assertThat(getSpy().requiresRelease()).isFalse();
    }

    @Test
    void shouldBeRelatedToCode() {
        assertThat(getSpy().isSourceCodeRelated()).isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldRequireValidSCMAndLastCommit() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getDetails()).thenReturn(
            Map.of(),
            Map.of(
                SCMLinkValidationProbe.KEY, ProbeResult.failure(SCMLinkValidationProbe.KEY, "")
            ),
            Map.of(
                SCMLinkValidationProbe.KEY, ProbeResult.failure(SCMLinkValidationProbe.KEY, ""),
                LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
            ),
            Map.of(
                LastCommitDateProbe.KEY, ProbeResult.failure(LastCommitDateProbe.KEY, "")
            ),
            Map.of(
                LastCommitDateProbe.KEY, ProbeResult.failure(LastCommitDateProbe.KEY, ""),
                SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, "")
            ),
            Map.of(
                SCMLinkValidationProbe.KEY, ProbeResult.failure(SCMLinkValidationProbe.KEY, ""),
                LastCommitDateProbe.KEY, ProbeResult.failure(LastCommitDateProbe.KEY, "")
            )
        );

        final RenovateProbe probe = getSpy();
        for (int i = 0; i < 6; i++) {
            assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.error(RenovateProbe.KEY, "renovate does not meet the criteria to be executed on null"));

            verify(probe, never()).doApply(plugin, ctx);
        }
    }

    @Test
    void shouldDetectMissingRenovateFile() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final RenovateProbe probe = getSpy();

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        final Path repo = Files.createTempDirectory("foo");
        when(ctx.getScmRepository()).thenReturn(repo);

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.failure(RenovateProbe.KEY, "No GitHub configuration folder found"));
        verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));
    }

    @Test
    void shouldDetectRenovateFile() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final RenovateProbe probe = getSpy();

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""),
            LastCommitDateProbe.KEY, ProbeResult.success(LastCommitDateProbe.KEY, "")
        ));
        final Path repo = Files.createTempDirectory("foo");
        final Path github = Files.createDirectories(repo.resolve(".github"));

        Files.createFile(github.resolve("renovate.json"));

        when(ctx.getScmRepository()).thenReturn(repo);

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.success(RenovateProbe.KEY, "renovate is configured"));
        verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));
    }
}
