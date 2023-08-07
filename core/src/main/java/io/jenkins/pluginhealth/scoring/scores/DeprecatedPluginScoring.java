/*
 * MIT License
 *
 * Copyright (c) 2023 Jenkins Infra
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

import io.jenkins.pluginhealth.scoring.model.ChangelogResult;
import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.probes.DeprecatedPluginProbe;

import org.springframework.stereotype.Component;

@Component
public class DeprecatedPluginScoring extends Scoring {
    private static final float COEFFICIENT = .8f;
    private static final String KEY = "deprecation";

    @Override
    public List<Changelog> getChangelog() {
        return List.of(
            new Changelog() {
                @Override
                public String getDescription() {
                    return "The plugin must not be marked as deprecated.";
                }

                @Override
                public ChangelogResult getScore(Plugin $, Map<String, ProbeResult> probeResults) {
                    final ProbeResult probeResult = probeResults.get(DeprecatedPluginProbe.KEY);
                    if (probeResult == null) {
                        return new ChangelogResult(0, getWeight(), List.of("Cannot determine if the plugin is marked as deprecated or not."));
                    }

                    return switch (probeResult.message()) {
                        case "This plugin is marked as deprecated." ->
                            new ChangelogResult(0, getWeight(), List.of("Plugin is marked as deprecated."));
                        case "This plugin is NOT deprecated." ->
                            new ChangelogResult(100, getWeight(), List.of("Plugin is not marked as deprecated."));
                        default ->
                            new ChangelogResult(0, getWeight(), List.of("Cannot determine if the plugin is marked as deprecated or not.", probeResult.message()));
                    };
                }

                @Override
                public int getWeight() {
                    return 1;
                }
            }
        );
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
}
