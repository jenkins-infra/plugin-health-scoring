/*
 * MIT License
 *
 * Copyright (c) 2022-2024 Jenkins Infra
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

import java.util.Map;
import java.util.Set;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
import io.jenkins.pluginhealth.scoring.probes.ContinuousDeliveryProbe;
import io.jenkins.pluginhealth.scoring.probes.ContributingGuidelinesProbe;
import io.jenkins.pluginhealth.scoring.probes.DocumentationMigrationProbe;
import io.jenkins.pluginhealth.scoring.probes.PluginDescriptionMigrationProbe;
import io.jenkins.pluginhealth.scoring.probes.ReleaseDrafterProbe;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class DocumentationScoringTest extends AbstractScoringTest<DocumentationScoring> {
    @Override
    DocumentationScoring getSpy() {
        return spy(DocumentationScoring.class);
    }

    @Test
    void shouldScoreOneHundredWithInheritedContributingGuide() {
        final Plugin plugin = mock(Plugin.class);
        final DocumentationScoring scoring = getSpy();

        when(plugin.getDetails())
            .thenReturn(Map.of(
                DocumentationMigrationProbe.KEY,
                ProbeResult.success(
                    DocumentationMigrationProbe.KEY,
                    "Documentation is located in the plugin repository.",
                    1),
                PluginDescriptionMigrationProbe.KEY,
                ProbeResult.success(
                    PluginDescriptionMigrationProbe.KEY, "Plugin seems to have a correct description.", 1),
                ContributingGuidelinesProbe.KEY,
                ProbeResult.success(
                    ContributingGuidelinesProbe.KEY, "Inherit from organization contributing guide.", 1),
                ReleaseDrafterProbe.KEY,
                ProbeResult.success(ReleaseDrafterProbe.KEY, "Release Drafter is configured.", 1)));

        ScoreResult result = scoring.apply(plugin);
        assertThat(result)
            .isNotNull()
            .usingRecursiveComparison()
            .comparingOnlyFields("key", "value")
            .isEqualTo(new ScoreResult(DocumentationScoring.KEY, 100, .5f, Set.of(), 1));
    }

    @Test
    void shouldScoreOneHundredEvenWithoutReleaseDrafter() {
        final Plugin plugin = mock(Plugin.class);
        final DocumentationScoring scoring = getSpy();

        when(plugin.getDetails())
            .thenReturn(Map.of(
                DocumentationMigrationProbe.KEY,
                ProbeResult.success(
                    DocumentationMigrationProbe.KEY,
                    "Documentation is located in the plugin repository.",
                    1),
                PluginDescriptionMigrationProbe.KEY,
                ProbeResult.success(
                    PluginDescriptionMigrationProbe.KEY,
                    "Plugin seems to have a correct description.",
                    1),
                ContributingGuidelinesProbe.KEY,
                ProbeResult.success(
                    ContributingGuidelinesProbe.KEY, "Inherit from organization contributing guide.", 1),
                ReleaseDrafterProbe.KEY,
                ProbeResult.success(ReleaseDrafterProbe.KEY, "Release Drafter not is configured.", 1)));

        ScoreResult result = scoring.apply(plugin);
        assertThat(result)
            .isNotNull()
            .usingRecursiveComparison()
            .comparingOnlyFields("key", "value")
            .isEqualTo(new ScoreResult(DocumentationScoring.KEY, 100, .5f, Set.of(), 1));
        assertThat(result.componentsResults())
            .hasSize(4)
            .haveAtLeastOne(new Condition<>(
                res -> {
                    return res.weight() == 0
                        && res.score() == 0
                        && res.resolutions().size() == 1
                        && res.reasons()
                        .contains("Plugin is not using Release Drafter to manage its changelog.");
                },
                "Release drafter is not configured"));
    }

    @Test
    void shouldConsiderCDAsConfiguringReleaseDrafter() {
        final Plugin plugin = mock(Plugin.class);
        final DocumentationScoring scoring = getSpy();

        when(plugin.getDetails())
            .thenReturn(Map.of(
                DocumentationMigrationProbe.KEY,
                ProbeResult.success(
                    DocumentationMigrationProbe.KEY,
                    "Documentation is located in the plugin repository.",
                    1),
                PluginDescriptionMigrationProbe.KEY,
                ProbeResult.success(
                    PluginDescriptionMigrationProbe.KEY,
                    "Plugin seems to have a correct description.",
                    1),
                ContributingGuidelinesProbe.KEY,
                ProbeResult.success(
                    ContributingGuidelinesProbe.KEY, "Contributing guidelines found.", 1),
                ReleaseDrafterProbe.KEY,
                ProbeResult.success(ReleaseDrafterProbe.KEY, "Release Drafter not is configured.", 1),
                ContinuousDeliveryProbe.KEY,
                ProbeResult.success(
                    ContinuousDeliveryProbe.KEY, "JEP-229 workflow definition found.", 1)));

        ScoreResult result = scoring.apply(plugin);
        assertThat(result)
            .isNotNull()
            .usingRecursiveComparison()
            .comparingOnlyFields("key", "value")
            .isEqualTo(new ScoreResult(DocumentationScoring.KEY, 100, .5f, Set.of(), 1));
        assertThat(result.componentsResults())
            .hasSize(4)
            .haveAtLeastOne(new Condition<>(
                res -> {
                    return res.weight() == 0
                        && res.score() == 100
                        && res.reasons().size() == 1
                        && res.reasons()
                        .contains("Plugin using Release Drafter because it has CD configured.");
                },
                "CD is configured."));
    }

    @Test
    void shouldScoreFortyWithMigratedDocumentationOnly() {
        final Plugin plugin = mock(Plugin.class);
        final DocumentationScoring scoring = getSpy();

        when(plugin.getDetails())
            .thenReturn(Map.of(
                DocumentationMigrationProbe.KEY,
                ProbeResult.success(
                    DocumentationMigrationProbe.KEY,
                    "Documentation is located in the plugin repository.",
                    1),
                PluginDescriptionMigrationProbe.KEY,
                ProbeResult.success(
                    PluginDescriptionMigrationProbe.KEY,
                    "Plugin is using description from the plugin archetype.",
                    1),
                ContributingGuidelinesProbe.KEY,
                ProbeResult.success(
                    ContributingGuidelinesProbe.KEY, "No contributing guidelines found.", 1)));

        ScoreResult result = scoring.apply(plugin);
        assertThat(result)
            .isNotNull()
            .usingRecursiveComparison()
            .comparingOnlyFields("key", "value")
            .isEqualTo(new ScoreResult(DocumentationScoring.KEY, 40, .5f, Set.of(), 1));
    }

    @Test
    void shouldScoreFortyWithMigratedDescriptionOnly() {
        final Plugin plugin = mock(Plugin.class);
        final DocumentationScoring scoring = getSpy();

        when(plugin.getDetails())
            .thenReturn(Map.of(
                DocumentationMigrationProbe.KEY,
                ProbeResult.success(
                    DocumentationMigrationProbe.KEY,
                    "Documentation is not located in the plugin repository.",
                    1),
                PluginDescriptionMigrationProbe.KEY,
                ProbeResult.success(
                    PluginDescriptionMigrationProbe.KEY,
                    "Plugin seems to have a correct description.",
                    1),
                ContributingGuidelinesProbe.KEY,
                ProbeResult.success(
                    ContributingGuidelinesProbe.KEY, "No contributing guidelines found.", 1)));

        ScoreResult result = scoring.apply(plugin);
        assertThat(result)
            .isNotNull()
            .usingRecursiveComparison()
            .comparingOnlyFields("key", "value")
            .isEqualTo(new ScoreResult(DocumentationScoring.KEY, 40, .5f, Set.of(), 1));
    }

    @Test
    void shouldScoreZeroWithMigratedDocumentationOnly() {
        final Plugin plugin = mock(Plugin.class);
        final DocumentationScoring scoring = getSpy();

        when(plugin.getDetails())
            .thenReturn(Map.of(
                DocumentationMigrationProbe.KEY,
                ProbeResult.success(
                    DocumentationMigrationProbe.KEY,
                    "Documentation is not located in the plugin repository.",
                    1),
                PluginDescriptionMigrationProbe.KEY,
                ProbeResult.success(
                    PluginDescriptionMigrationProbe.KEY,
                    "Plugin is using description from the plugin archetype.",
                    1),
                ContributingGuidelinesProbe.KEY,
                ProbeResult.success(
                    ContributingGuidelinesProbe.KEY, "No contributing guidelines found.", 1)));

        ScoreResult result = scoring.apply(plugin);
        assertThat(result)
            .isNotNull()
            .usingRecursiveComparison()
            .comparingOnlyFields("key", "value")
            .isEqualTo(new ScoreResult(DocumentationScoring.KEY, 0, .5f, Set.of(), 1));
    }
}
