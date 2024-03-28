/*
 * MIT License
 *
 * Copyright (c) 2023-2024 Jenkins Infra
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
import io.jenkins.pluginhealth.scoring.probes.KnownSecurityVulnerabilityProbe;

import org.springframework.stereotype.Component;

@Component
public class SecurityWarningScoring extends Scoring {
    private static final float COEFFICIENT = 1f;
    private static final String KEY = "security";

    @Override
    public List<ScoringComponent> getComponents() {
        return List.of(new ScoringComponent() {
            @Override
            public String getDescription() {
                return "The plugin must not have on-going security advisory.";
            }

            @Override
            public ScoringComponentResult getScore(Plugin $, Map<String, ProbeResult> probeResults) {
                final ProbeResult probeResult = probeResults.get(KnownSecurityVulnerabilityProbe.KEY);
                final int index = probeResult.message().indexOf(":");
                if (probeResult == null || ProbeResult.Status.ERROR.equals(probeResult.status())) {
                    return new ScoringComponentResult(
                            -100, 100, List.of("Cannot determine if plugin has on-going security advisory."));
                }
                if ("No known security vulnerabilities.".equals(probeResult.message())) {
                    return new ScoringComponentResult(
                            100, getWeight(), List.of("Plugin does not seem to have on-going security advisory."));
                }
                return new ScoringComponentResult(
                        0,
                        getWeight(),
                        List.of("Plugin seem to have on-going security advisory.", probeResult.message()),
                        List.of(new Resolution(probeResult.message().substring(0, index), probeResult.message().substring(index))));
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
        return "Scores plugin based on current and active security warnings.";
    }

    @Override
    public int version() {
        return 2;
    }
}
