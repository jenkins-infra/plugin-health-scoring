/*
 * MIT License
 *
 * Copyright (c) 2023-2025 Jenkins Infra
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

import java.util.List;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.Resolution;
import io.jenkins.pluginhealth.scoring.model.ScoringComponentResult;
import io.jenkins.pluginhealth.scoring.probes.DeprecatedPluginProbe;
import io.jenkins.pluginhealth.scoring.probes.RepositoryArchivedStatusProbe;
import io.jenkins.pluginhealth.scoring.probes.UpdateCenterPluginPublicationProbe;

import org.springframework.stereotype.Component;

@Component
public class DeprecatedPluginScoring extends Scoring {
    private static final float COEFFICIENT = 1f;
    private static final String KEY = "deprecation";

    @Override
    public List<ScoringComponent> getComponents() {
        return List.of(
                new ScoringComponent() {
                    @Override
                    public String getDescription() {
                        return "The plugin must not be marked as deprecated.";
                    }

                    @Override
                    public ScoringComponentResult getScore(Plugin $, Map<String, ProbeResult> probeResults) {
                        final ProbeResult probeResult = probeResults.get(DeprecatedPluginProbe.KEY);
                        if (probeResult == null) {
                            return new ScoringComponentResult(
                                    0,
                                    getWeight(),
                                    List.of("Cannot determine if the plugin is marked as deprecated or not."));
                        }

                        return switch (probeResult.message()) {
                            case "This plugin is marked as deprecated." -> new ScoringComponentResult(
                                    0,
                                    getWeight(),
                                    List.of("Plugin is marked as deprecated."),
                                    List.of(
                                            new Resolution(
                                                    "See deprecation guidelines",
                                                    "https://www.jenkins.io/doc/developer/plugin-governance/deprecating-or-removing-plugin/")));
                            case "This plugin is NOT deprecated." -> new ScoringComponentResult(
                                    100, 0, List.of("Plugin is not marked as deprecated."));
                            default -> new ScoringComponentResult(
                                    0,
                                    getWeight(),
                                    List.of(
                                            "Cannot determine if the plugin is marked as deprecated or not.",
                                            probeResult.message()));
                        };
                    }

                    @Override
                    public int getWeight() {
                        return 1;
                    }
                },
                new ScoringComponent() {
                    @Override
                    public String getDescription() {
                        return "The plugin's repository must not be archived";
                    }

                    @Override
                    public ScoringComponentResult getScore(Plugin plugin, Map<String, ProbeResult> probeResults) {
                        final ProbeResult probeResult = probeResults.get(RepositoryArchivedStatusProbe.KEY);
                        if (probeResult == null || ProbeResult.Status.ERROR.equals(probeResult.status())) {
                            return new ScoringComponentResult(
                                    -100, 100, List.of("Cannot determine if the repository is archived or not."));
                        }

                        final boolean isArchived = Boolean.parseBoolean(probeResult.message());
                        return isArchived
                                ? new ScoringComponentResult(
                                        0, getWeight(), List.of("The plugin repository is archived."))
                                : new ScoringComponentResult(100, 0, List.of("The repository is not archived."));
                    }

                    @Override
                    public int getWeight() {
                        return 1;
                    }
                },
                new ScoringComponent() {
                    @Override
                    public String getDescription() {
                        return "Plugin should be present in the update-center to be distributed.";
                    }

                    @Override
                    public ScoringComponentResult getScore(Plugin $, Map<String, ProbeResult> probeResults) {
                        final ProbeResult probeResult = probeResults.get(UpdateCenterPluginPublicationProbe.KEY);
                        if (probeResult == null || ProbeResult.Status.ERROR.equals(probeResult.status())) {
                            return new ScoringComponentResult(
                                    -100, 100, List.of("Cannot determine if the plugin is part of the update-center."));
                        }

                        return switch (probeResult.message()) {
                            case "This plugin is still actively published by the update-center." -> new ScoringComponentResult(
                                    100, 0, List.of("The plugin appears in the update-center."));
                            case "This plugin's publication has been stopped by the update-center." -> new ScoringComponentResult(
                                    0, getWeight(), List.of("Ths plugin is not part of the update-center."));
                            default -> new ScoringComponentResult(
                                    -5,
                                    getWeight(),
                                    List.of(
                                            "Cannot determine if the plugin is part of the update-center or not.",
                                            probeResult.message()));
                        };
                    }

                    @Override
                    public int getWeight() {
                        return 1;
                    }
                });
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public float weight() {
        return COEFFICIENT;
    }

    @Override
    public String description() {
        return "Scores plugin based on its deprecation status.";
    }

    @Override
    public int version() {
        return 5;
    }
}
