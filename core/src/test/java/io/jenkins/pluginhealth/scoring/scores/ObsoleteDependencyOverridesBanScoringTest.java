/*
 * MIT License
 *
 * Copyright (c) 2025-2026 Jenkins Infra
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.jenkins.pluginhealth.scoring.scores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
import io.jenkins.pluginhealth.scoring.model.ScoringComponentResult;
import io.jenkins.pluginhealth.scoring.probes.MavenParentVersionProbe;
import io.jenkins.pluginhealth.scoring.probes.MavenPropertiesProbe;

import org.junit.jupiter.api.Test;

class ObsoleteDependencyOverridesBanScoringTest extends AbstractScoringTest<ObsoleteDependencyOverridesBanScoring> {
    private static final String MINIMUM_SUPPORTED_PARENT_VERSION = "6.2185.v41f4290b_a_458";
    private static final String UNSUPPORTED_PARENT_VERSION = "6.2184.v41f4290b_a_458";

    @Override
    ObsoleteDependencyOverridesBanScoring getSpy() {
        return spy(ObsoleteDependencyOverridesBanScoring.class);
    }

    @Test
    void shouldNotWeightInGeneralPluginScore() {
        assertThat(getSpy().weight()).isZero();
    }

    @Test
    void requiresMavenParentVersionProbe() {
        final Plugin plugin = mock(Plugin.class);
        final ObsoleteDependencyOverridesBanScoring scoring = getSpy();

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result)
                .isNotNull()
                .usingRecursiveComparison()
                .comparingOnlyFields("value", "weight", "componentsResults")
                .isEqualTo(new ScoreResult(
                        ObsoleteDependencyOverridesBanScoring.KEY,
                        0,
                        0,
                        Set.of(new ScoringComponentResult(
                                0, 1, List.of("Cannot find the Maven parent POM version for the plugin."))),
                        0));
    }

    @Test
    void requiresWellFormedMavenParentVersionProbe() {
        final Plugin plugin = mock(Plugin.class);
        final ObsoleteDependencyOverridesBanScoring scoring = getSpy();

        when(plugin.getDetails())
                .thenReturn(Map.of(
                        MavenParentVersionProbe.KEY, ProbeResult.success(MavenParentVersionProbe.KEY, Map.of(), 1)));

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result)
                .isNotNull()
                .usingRecursiveComparison()
                .comparingOnlyFields("value", "weight", "componentsResults")
                .isEqualTo(new ScoreResult(
                        ObsoleteDependencyOverridesBanScoring.KEY,
                        0,
                        0,
                        Set.of(new ScoringComponentResult(
                                0, 1, List.of("Cannot determine the Maven parent POM version for the plugin."))),
                        0));
    }

    @Test
    void shouldNotBeApplicableWhenParentVersionIsTooOld() {
        final Plugin plugin = mock(Plugin.class);
        final ObsoleteDependencyOverridesBanScoring scoring = getSpy();

        when(plugin.getDetails())
                .thenReturn(Map.of(
                        MavenParentVersionProbe.KEY,
                        ProbeResult.success(MavenParentVersionProbe.KEY, UNSUPPORTED_PARENT_VERSION, 1)));

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result)
                .isNotNull()
                .usingRecursiveComparison()
                .comparingOnlyFields("value", "weight", "componentsResults")
                .isEqualTo(new ScoreResult(
                        ObsoleteDependencyOverridesBanScoring.KEY,
                        0,
                        0,
                        Set.of(new ScoringComponentResult(
                                0,
                                1,
                                List.of("banObsoleteDependencyOverrides.skip property is not applicable: the plugin"
                                        + " parent POM version " + UNSUPPORTED_PARENT_VERSION
                                        + " is older than " + MINIMUM_SUPPORTED_PARENT_VERSION + "."))),
                        0));
    }

    @Test
    void requiresMavenPropertiesProbe() {
        final Plugin plugin = mock(Plugin.class);
        final ObsoleteDependencyOverridesBanScoring scoring = getSpy();

        when(plugin.getDetails())
                .thenReturn(Map.of(
                        MavenParentVersionProbe.KEY,
                        ProbeResult.success(MavenParentVersionProbe.KEY, MINIMUM_SUPPORTED_PARENT_VERSION, 1)));

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result)
                .isNotNull()
                .usingRecursiveComparison()
                .comparingOnlyFields("value", "weight", "componentsResults")
                .isEqualTo(new ScoreResult(
                        ObsoleteDependencyOverridesBanScoring.KEY,
                        0,
                        0,
                        Set.of(new ScoringComponentResult(
                                0, 1, List.of("Cannot find Maven properties for the plugin."))),
                        0));
    }

    @Test
    void requiresWellFormedMavenPropertiesProbe() {
        final Plugin plugin = mock(Plugin.class);
        final ObsoleteDependencyOverridesBanScoring scoring = getSpy();

        when(plugin.getDetails())
                .thenReturn(Map.of(
                        MavenParentVersionProbe.KEY,
                        ProbeResult.success(MavenParentVersionProbe.KEY, MINIMUM_SUPPORTED_PARENT_VERSION, 1),
                        MavenPropertiesProbe.KEY,
                        ProbeResult.success(MavenPropertiesProbe.KEY, "foo", 1)));

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result)
                .isNotNull()
                .usingRecursiveComparison()
                .comparingOnlyFields("value", "weight", "componentsResults")
                .isEqualTo(new ScoreResult(
                        ObsoleteDependencyOverridesBanScoring.KEY,
                        0,
                        0,
                        Set.of(new ScoringComponentResult(
                                0, 1, List.of("Cannot use the Maven properties from the plugin."))),
                        0));
    }

    @Test
    void shouldRecogniseBanObsoleteDependencyOverridesProperty() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ObsoleteDependencyOverridesBanScoring scoring = getSpy();

        when(plugin.getDetails())
                .thenReturn(Map.of(
                        MavenParentVersionProbe.KEY,
                        ProbeResult.success(MavenParentVersionProbe.KEY, MINIMUM_SUPPORTED_PARENT_VERSION, 1),
                        MavenPropertiesProbe.KEY,
                        ProbeResult.success(
                                MavenPropertiesProbe.KEY, Map.of("banObsoleteDependencyOverrides.skip", "true"), 1)));
        assertThat(scoring.apply(plugin))
                .isNotNull()
                .usingRecursiveComparison()
                .comparingOnlyFields("value", "weight", "componentsResults")
                .isEqualTo(new ScoreResult(
                        ObsoleteDependencyOverridesBanScoring.KEY,
                        0,
                        0,
                        Set.of(
                                new ScoringComponentResult(
                                        0,
                                        1,
                                        List.of(
                                                "banObsoleteDependencyOverrides.skip property is not set or true on the plugin."))),
                        0));

        when(plugin.getDetails())
                .thenReturn(Map.of(
                        MavenParentVersionProbe.KEY,
                        ProbeResult.success(MavenParentVersionProbe.KEY, MINIMUM_SUPPORTED_PARENT_VERSION, 1),
                        MavenPropertiesProbe.KEY,
                        ProbeResult.success(
                                MavenPropertiesProbe.KEY, Map.of("banObsoleteDependencyOverrides.skip", "false"), 1)));
        assertThat(scoring.apply(plugin))
                .isNotNull()
                .usingRecursiveComparison()
                .comparingOnlyFields("value", "weight", "componentsResults")
                .isEqualTo(new ScoreResult(
                        ObsoleteDependencyOverridesBanScoring.KEY,
                        100,
                        0,
                        Set.of(new ScoringComponentResult(
                                100, 1, List.of("Obsolete dependency overrides are banned on the plugin."))),
                        0));
    }
}
