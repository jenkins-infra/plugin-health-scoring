package io.jenkins.pluginhealth.scoring.probes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ThirdPartyRepositoryDetectionProbeTest extends AbstractProbeTest<ThirdPartyRepositoryDetectionProbe> {
    private static Stream<Arguments> successes() {
        return Stream.of(
            arguments(
                Paths.get("src", "test", "resources", "pom-test-only-correct-path"),
                "https://github.com/jenkinsci/test-plugin"
            ),
            arguments(
                Paths.get("src", "test", "resources", "pom-test-parent-child-no-third-party-repository-success", "plugin"),
                "https://github.com/jenkinsci/test-plugin"
            )
        );
    }

    private static Stream<Arguments> failures() {
        return Stream.of(
            arguments(
                Paths.get("src", "test", "resources", "pom-test-both-paths"),
                "https://github.com/jenkinsci/test-plugin"
            ),
            arguments(
                Paths.get("src", "test", "resources", "pom-test-only-incorrect-path"),
                "https://github.com/jenkinsci/test-plugin"
            ),
            arguments(
                Paths.get("src", "test", "resources", "pom-test-third-party-probe-in-parent-failure", "plugin"),
                "https://github.com/jenkinsci/test-plugin/plugin"
            ),
            arguments(
                Paths.get("src", "test", "resources", "pom-test-third-party-probe-in-child-failure", "plugin"),
                "https://github.com/jenkinsci/test-plugin/plugin"
            )
        );
    }

    private static Stream<Arguments> failures2() {
        return Stream.of(
            arguments(
                Paths.get("src", "test", "resources", "pom-test-no-repository-tag"),
                "https://github.com/jenkinsci/test-plugin"
            ),
            arguments(
                Paths.get("src", "test", "resources", "pom-test-parent-child-no-repository-tag-failure", "plugin"),
                "https://github.com/jenkinsci/test-plugin"
            )
        );
    }

    @Test
    void shouldBeExecutedAfterSCMLinkValidationProbeProbe() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final ThirdPartyRepositoryDetectionProbe probe = getSpy();
        when(plugin.getName()).thenReturn("foo-bar");
        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status")
            .isEqualTo(ProbeResult.error(ThirdPartyRepositoryDetectionProbe.KEY, ""));
        verify(probe, never()).doApply(plugin, ctx);
    }

    @Override
    ThirdPartyRepositoryDetectionProbe getSpy() {
        return spy(ThirdPartyRepositoryDetectionProbe.class);
    }

    @ParameterizedTest
    @MethodSource("successes")
    void shouldPassIfNoThirdPartyRepositoriesDetected(Path resourceDirectory, String scm) {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(ctx.getScmRepository()).thenReturn(resourceDirectory);
        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, "")
        ));
        when(plugin.getScm()).thenReturn(scm);

        try {
            final ThirdPartyRepositoryDetectionProbe probe = getSpy();
            assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.success(ThirdPartyRepositoryDetectionProbe.KEY, "The plugin has no third party repositories"));
            verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));
        } finally {
            File effectivePomFile = new File(resourceDirectory + "/effective-pom.xml");
            effectivePomFile.delete();
        }
    }

    @ParameterizedTest
    @MethodSource("failures")
    void shouldFailIfThirdPartRepositoriesDetected(Path resourceDirectory, String scm) {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(ctx.getScmRepository()).thenReturn(resourceDirectory);
        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, "")
        ));
        when(plugin.getScm()).thenReturn(scm);

        try {
            final ThirdPartyRepositoryDetectionProbe probe = getSpy();
            assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.failure(ThirdPartyRepositoryDetectionProbe.KEY, "Third party repositories detected in the plugin"));
            verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));
        } finally {
            File effectivePomFile = new File(resourceDirectory + "/effective-pom.xml");
            effectivePomFile.delete();
        }
    }

    @ParameterizedTest
    @MethodSource("failures2")
    void shouldFailWhenNoRepositoriesDetected(Path resourceDirectory, String scm) {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(ctx.getScmRepository()).thenReturn(resourceDirectory);
        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, "")
        ));
        when(plugin.getScm()).thenReturn(scm);

        try {
            final ThirdPartyRepositoryDetectionProbe probe = getSpy();
            assertThat(probe.apply(plugin, ctx))
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.failure(ThirdPartyRepositoryDetectionProbe.KEY, "No repositories detected"));
            verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));
        } finally {
            File effectivePomFile = new File(resourceDirectory + "/effective-pom.xml");
            effectivePomFile.delete();
        }
    }

    @Test
    public void testGenerateEffectivePom() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getDetails()).thenReturn(Map.of(
            SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, "")
        ));

        when(ctx.getScmRepository()).thenReturn(Path.of("src/test/resources/pom-to-create-effective-pom-from"));
        final ThirdPartyRepositoryDetectionProbe probe = getSpy();

        try {
            // Call the method to be tested
            probe.generateEffectivePom(ctx.getScmRepository() + "/pom.xml"); // will get the root pom the current repo
            // Verify the result
            File effectivePomFile = new File("src/test/resources/pom-to-create-effective-pom-from/effective-pom.xml");
            assertTrue(effectivePomFile.exists());
        } finally {
            // Clean up files
            File effectivePomFile = new File("src/test/resources/pom-to-create-effective-pom-from/effective-pom.xml");
            effectivePomFile.delete();
        }
    }
}
