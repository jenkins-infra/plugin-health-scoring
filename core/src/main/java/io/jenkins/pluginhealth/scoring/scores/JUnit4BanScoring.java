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

import java.util.List;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.Resolution;
import io.jenkins.pluginhealth.scoring.model.ScoringComponentResult;
import io.jenkins.pluginhealth.scoring.probes.MavenPropertiesProbe;

import org.springframework.stereotype.Component;

@Component
public class JUnit4BanScoring extends Scoring {
    public static final String KEY = "junit4-ban";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public float weight() {
        return 0;
    }

    @Override
    public String description() {
        return "Shows if the plugin bans JUnit 4 imports. Not used in general plugin score, just for information.";
    }

    @Override
    public List<ScoringComponent> getComponents() {
        return List.of(new ScoringComponent() {
            @Override
            public String getDescription() {
                return "";
            }

            @Override
            public ScoringComponentResult getScore(Plugin plugin, Map<String, ProbeResult> probeResults) {
                final ProbeResult mavenPropertiesResult = probeResults.get(MavenPropertiesProbe.KEY);
                if (mavenPropertiesResult == null
                        || mavenPropertiesResult.status().equals(ProbeResult.Status.ERROR)) {
                    return new ScoringComponentResult(
                            0, getWeight(), List.of("Cannot find Maven properties for the plugin."));
                }
                final Object message = mavenPropertiesResult.message();
                if (Map.class.isAssignableFrom(message.getClass())) {
                    final Map<String, String> properties = (Map<String, String>) mavenPropertiesResult.message();
                    final String banJUnit4SkipProperty = properties.getOrDefault("ban-junit4-imports.skip", "true");
                    if (Boolean.parseBoolean(banJUnit4SkipProperty)) {
                        return new ScoringComponentResult(
                                0,
                                getWeight(),
                                List.of("ban-junit4-imports.skip property is not set or true on the plugin."),
                                List.of(new Resolution(
                                        "How to set up JUnit 4 import ban",
                                        "https://github.com/jenkinsci/plugin-pom/pull/1178.")));
                    }
                    return new ScoringComponentResult(
                            100, getWeight(), List.of("JUnit4 imports are banned on the plugin."));
                }
                return new ScoringComponentResult(
                        0, getWeight(), List.of("Cannot use the Maven properties from the plugin."));
            }

            @Override
            public int getWeight() {
                return 1;
            }
        });
    }

    @Override
    public int version() {
        return 1;
    }
}
