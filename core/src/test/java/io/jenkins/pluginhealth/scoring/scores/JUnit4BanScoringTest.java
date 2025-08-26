/*
 * MIT License
 *
 * Copyright (c) 2025 Jenkins Infra
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
import io.jenkins.pluginhealth.scoring.model.Resolution;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
import io.jenkins.pluginhealth.scoring.model.ScoringComponentResult;
import io.jenkins.pluginhealth.scoring.probes.MavenPropertiesProbe;

import org.junit.jupiter.api.Test;

class JUnit4BanScoringTest extends AbstractScoringTest<JUnit4BanScoring> {

    @Override
    JUnit4BanScoring getSpy() {
        return spy(JUnit4BanScoring.class);
    }

    @Test
    void shouldNotWeightInGeneralPluginScore() {
        assertThat(getSpy().weight()).isZero();
    }

    @Test
    void requiresMavenPropertiesProbe() {
        final Plugin plugin = mock(Plugin.class);
        final JUnit4BanScoring scoring = getSpy();

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result)
                .isNotNull()
                .usingRecursiveComparison()
                .comparingOnlyFields("value", "weight", "componentsResults")
                .isEqualTo(new ScoreResult(
                        JUnit4BanScoring.KEY,
                        0,
                        0,
                        Set.of(new ScoringComponentResult(
                                0, 1, List.of("Cannot find Maven properties for the plugin."))),
                        0));
    }

    @Test
    void requiresWellFormedMavenPropertiesProbe() {
        final Plugin plugin = mock(Plugin.class);
        final JUnit4BanScoring scoring = getSpy();

        when(plugin.getDetails())
                .thenReturn(Map.of(MavenPropertiesProbe.KEY, ProbeResult.success(MavenPropertiesProbe.KEY, "foo", 1)));

        final ScoreResult result = scoring.apply(plugin);
        assertThat(result)
                .isNotNull()
                .usingRecursiveComparison()
                .comparingOnlyFields("value", "weight", "componentsResults")
                .isEqualTo(new ScoreResult(
                        JUnit4BanScoring.KEY,
                        0,
                        0,
                        Set.of(new ScoringComponentResult(
                                0, 1, List.of("Cannot use the Maven properties from the plugin."))),
                        0));
    }

    @Test
    void shouldRecogniseJUnit4BanProperty() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final JUnit4BanScoring scoring = getSpy();

        when(plugin.getDetails())
                .thenReturn(Map.of(
                        MavenPropertiesProbe.KEY,
                        ProbeResult.success(MavenPropertiesProbe.KEY, Map.of("ban-junit4-imports.skip", "true"), 1)));
        assertThat(scoring.apply(plugin))
                .isNotNull()
                .usingRecursiveComparison()
                .comparingOnlyFields("value", "weight", "componentsResults")
                .isEqualTo(new ScoreResult(
                        JUnit4BanScoring.KEY,
                        0,
                        0,
                        Set.of(new ScoringComponentResult(
                                0,
                                1,
                                List.of("ban-junit4-imports.skip property is not set or true on the plugin."),
                                List.of(new Resolution(
                                        "How to set up JUnit 4 import ban",
                                        "https://github.com/jenkinsci/plugin-pom/pull/1178.")))),
                        0));

        when(plugin.getDetails())
                .thenReturn(Map.of(
                        MavenPropertiesProbe.KEY,
                        ProbeResult.success(MavenPropertiesProbe.KEY, Map.of("ban-junit4-imports.skip", "false"), 1)));
        assertThat(scoring.apply(plugin))
                .isNotNull()
                .usingRecursiveComparison()
                .comparingOnlyFields("value", "weight", "componentsResults")
                .isEqualTo(new ScoreResult(
                        JUnit4BanScoring.KEY,
                        100,
                        0,
                        Set.of(new ScoringComponentResult(100, 1, List.of("JUnit4 imports are banned on the plugin."))),
                        0));
    }
}
