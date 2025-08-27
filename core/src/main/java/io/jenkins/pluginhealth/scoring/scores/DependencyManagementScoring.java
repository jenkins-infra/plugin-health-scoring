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
import io.jenkins.pluginhealth.scoring.probes.DependabotProbe;
import io.jenkins.pluginhealth.scoring.probes.DependabotPullRequestProbe;
import io.jenkins.pluginhealth.scoring.probes.MavenDependenciesProbe;
import io.jenkins.pluginhealth.scoring.probes.RenovateProbe;

import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

@Component
public class DependencyManagementScoring extends Scoring {
    public static final String KEY = "dependency-management";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public float weight() {
        return 0.2f;
    }

    @Override
    public String description() {
        return "We encourage the usage of Dependency Management tools";
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
                final ProbeResult mavenDependencies = probeResults.get(MavenDependenciesProbe.KEY);
                if (mavenDependencies != null && mavenDependencies.status().equals(ProbeResult.Status.SUCCESS)) {
                    final List<String> dependencies = (List<String>) mavenDependencies.message();
                    if (dependencies.isEmpty()) {
                        return new ScoringComponentResult(100, 0, List.of("The plugin is not using any dependencies"));
                    }
                }

                final ProbeResult dependabot = probeResults.get(DependabotProbe.KEY);
                final ProbeResult renovate = probeResults.get(RenovateProbe.KEY);
                final ProbeResult pullRequestCount = probeResults.get(DependabotPullRequestProbe.KEY);

                if (dependabot != null
                        && ProbeResult.Status.SUCCESS.equals(dependabot.status())
                        && "Dependabot is configured.".equals(dependabot.message())) {
                    return manageOpenDependencyPullRequestValue(plugin, dependabot, pullRequestCount);
                }
                if (renovate != null
                        && ProbeResult.Status.SUCCESS.equals(renovate.status())
                        && "Renovate is configured.".equals(renovate.message())) {
                    return manageOpenDependencyPullRequestValue(plugin, renovate, pullRequestCount);
                }

                return new ScoringComponentResult(
                        0,
                        getWeight(),
                        List.of("Could not retrieve details required to score the plugin."),
                        List.of(new Resolution(
                                "Please open an issue on the project.",
                                "https://github.com/jenkins-infra/plugin-health-scoring/issues/new/choose")));
            }

            private ScoringComponentResult manageOpenDependencyPullRequestValue(
                    Plugin plugin, @NotNull ProbeResult botProbeResult, ProbeResult pullRequestCount) {
                if (pullRequestCount != null) {
                    return 0 == (int) pullRequestCount.message()
                            ? new ScoringComponentResult(
                                    100,
                                    getWeight(),
                                    List.of((String) botProbeResult.message(), "0 open dependency pull request"))
                            : new ScoringComponentResult(
                                    50,
                                    getWeight(),
                                    List.of(
                                            (String) botProbeResult.message(),
                                            "%s open dependency pull request".formatted(pullRequestCount.message())),
                                    List.of(new Resolution(
                                            "See the open pull requests of the plugin",
                                            "%s/pulls?q=is%%3Aopen+is%%3Apr+label%%3Adependencies"
                                                    .formatted(plugin.getScm()))));
                }
                return new ScoringComponentResult(
                        0,
                        getWeight(),
                        List.of(
                                (String) botProbeResult.message(),
                                "Cannot determine if there is any dependency pull request opened on the repository."));
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
