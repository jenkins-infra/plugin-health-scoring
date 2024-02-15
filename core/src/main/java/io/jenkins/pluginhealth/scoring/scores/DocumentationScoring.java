/*
 * MIT License
 *
 * Copyright (c) 2024 Jenkins Infra
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
import io.jenkins.pluginhealth.scoring.model.ScoringComponentResult;
import io.jenkins.pluginhealth.scoring.probes.ContributingGuidelinesProbe;
import io.jenkins.pluginhealth.scoring.probes.DocumentationMigrationProbe;

import org.springframework.stereotype.Component;

@Component
public class DocumentationScoring extends Scoring {
    public static final String KEY = "documentation";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public float weight() {
        return .5f;
    }

    @Override
    public String description() {
        return "Validates that the plugin has a specific contributing guide and a documentation.";
    }

    @Override
    public List<ScoringComponent> getComponents() {
        return List.of(new ScoringComponent() {
            @Override
            public String getDescription() {
                return "The plugin has a specific contributing guide.";
            }

            @Override
            public ScoringComponentResult getScore(Plugin plugin, Map<String, ProbeResult> probeResults) {
                ProbeResult probeResult = probeResults.get(ContributingGuidelinesProbe.KEY);
                if (probeResult != null && "Contributing guidelines found.".equals(probeResult.message())) {
                    return new ScoringComponentResult(100, getWeight(), List.of("Plugin has a contributing guide."));
                }
                return new ScoringComponentResult(0, getWeight(), List.of("The plugin relies on the global contributing guide."));
            }

            @Override
            public int getWeight() {
                return 2;
            }
        },
            new ScoringComponent() {
                @Override
                public String getDescription() {
                    return "Plugin documentation should be migrated from the wiki.";
                }

                @Override
                public ScoringComponentResult getScore(Plugin $, Map<String, ProbeResult> probeResults) {
                    final ProbeResult probeResult = probeResults.get(DocumentationMigrationProbe.KEY);
                    if (probeResult == null || ProbeResult.Status.ERROR.equals(probeResult.status())) {
                        return new ScoringComponentResult(0, getWeight(), List.of("Cannot confirm or not the documentation migration."));
                    }
                    return switch (probeResult.message()) {
                        case "Documentation is located in the plugin repository." ->
                            new ScoringComponentResult(100, getWeight(), List.of("Documentation is in plugin repository."));
                        case "Documentation is not located in the plugin repository." ->
                            new ScoringComponentResult(
                                0,
                                getWeight(),
                                List.of("Documentation should be migrated in plugin repository."),
                                List.of("https://www.jenkins.io/doc/developer/tutorial-improve/migrate-documentation-to-github/")
                            );
                        default ->
                            new ScoringComponentResult(0, getWeight(), List.of("Cannot confirm or not the documentation migration.", probeResult.message()));
                    };
                }

                @Override
                public int getWeight() {
                    return 8;
                }
            });
    }

    @Override
    public int version() {
        return 1;
    }
}
