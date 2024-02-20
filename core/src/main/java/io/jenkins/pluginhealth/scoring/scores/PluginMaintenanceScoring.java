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
import io.jenkins.pluginhealth.scoring.probes.ContinuousDeliveryProbe;
import io.jenkins.pluginhealth.scoring.probes.DependabotProbe;
import io.jenkins.pluginhealth.scoring.probes.DependabotPullRequestProbe;
import io.jenkins.pluginhealth.scoring.probes.JenkinsfileProbe;
import io.jenkins.pluginhealth.scoring.probes.RenovateProbe;

import org.springframework.stereotype.Component;

@Component
public class PluginMaintenanceScoring extends Scoring {
    private static final float COEFFICIENT = .5f;
    private static final String KEY = "repository-configuration";

    @Override
    public List<ScoringComponent> getComponents() {
        return List.of(
            new ScoringComponent() { // JenkinsFile presence
                @Override
                public String getDescription() {
                    return "Plugin must have a Jenkinsfile.";
                }

                @Override
                public ScoringComponentResult getScore(Plugin $, Map<String, ProbeResult> probeResults) {
                    final ProbeResult probeResult = probeResults.get(JenkinsfileProbe.KEY);
                    if (probeResult == null || ProbeResult.Status.ERROR.equals(probeResult.status())) {
                        return new ScoringComponentResult(0, getWeight(), List.of("Cannot confirm or not the presence of Jenkinsfile."));
                    }
                    return switch (probeResult.message()) {
                        case "Jenkinsfile found" ->
                            new ScoringComponentResult(100, getWeight(), List.of("Jenkinsfile detected in plugin repository."));
                        case "No Jenkinsfile found" ->
                            new ScoringComponentResult(
                                0,
                                getWeight(),
                                List.of("Jenkinsfile not detected in plugin repository."),
                                List.of(
                                    new Resolution("See how to add a Jenkinsfile", "https://www.jenkins.io/doc/developer/tutorial-improve/add-a-jenkinsfile/")
                                )
                            );
                        default ->
                            new ScoringComponentResult(0, getWeight(), List.of("Cannot confirm or not the presence of Jenkinsfile.", probeResult.message()));
                    };
                }

                @Override
                public int getWeight() {
                    return 65;
                }
            },
            new ScoringComponent() { // Dependabot and not dependency pull requests
                @Override
                public String getDescription() {
                    return "Plugin should be using a using a dependency version management bot.";
                }

                @Override
                public ScoringComponentResult getScore(Plugin pl, Map<String, ProbeResult> probeResults) {
                    final ProbeResult dependabot = probeResults.get(DependabotProbe.KEY);
                    final ProbeResult renovate = probeResults.get(RenovateProbe.KEY);
                    final ProbeResult dependencyPullRequest = probeResults.get(DependabotPullRequestProbe.KEY);

                    if (dependabot != null && "Dependabot is configured.".equals(dependabot.message()) && renovate != null && "Renovate is configured.".equals(renovate.message())) {
                        return new ScoringComponentResult(50, getWeight(), List.of("It seems that both dependabot and renovate are configured.", dependabot.message(), renovate.message()));
                    }

                    if (dependabot != null && ProbeResult.Status.SUCCESS.equals(dependabot.status()) && "Dependabot is configured.".equals(dependabot.message())) {
                        return manageOpenDependencyPullRequestValue(pl, dependabot, dependencyPullRequest);
                    }
                    if (renovate != null && ProbeResult.Status.SUCCESS.equals(renovate.status()) && "Renovate is configured.".equals(renovate.message())) {
                        return manageOpenDependencyPullRequestValue(pl, renovate, dependencyPullRequest);
                    }

                    return new ScoringComponentResult(
                        0,
                        getWeight(),
                        List.of("No dependency version manager bot are used on the plugin repository."),
                        List.of(
                            new Resolution("See how to automate the dependencies updates", "https://www.jenkins.io/doc/developer/tutorial-improve/automate-dependency-update-checks/")
                        )
                    );
                }

                private ScoringComponentResult manageOpenDependencyPullRequestValue(Plugin plugin, ProbeResult dependencyBotResult, ProbeResult dependencyPullRequestResult) {
                    if (dependencyPullRequestResult != null) {
                        return "0".equals(dependencyPullRequestResult.message()) ?
                            new ScoringComponentResult(
                                100,
                                getWeight(),
                                List.of(dependencyBotResult.message(), "%s open dependency pull request".formatted(dependencyPullRequestResult.message()))
                            ) :
                            new ScoringComponentResult(
                                50,
                                getWeight(),
                                List.of(dependencyBotResult.message(), "%s open dependency pull request".formatted(dependencyPullRequestResult.message())),
                                List.of(
                                    new Resolution("See the open pull requests of the plugin", "%s/pulls?q=is%%3Aopen+is%%3Apr+label%%3Adependencies".formatted(plugin.getScm()))
                                )
                            );
                    }
                    return new ScoringComponentResult(
                        0,
                        getWeight(),
                        List.of(
                            dependencyBotResult.message(),
                            "Cannot determine if there is any dependency pull request opened on the repository."
                        )
                    );
                }

                @Override
                public int getWeight() {
                    return 15;
                }
            },
            new ScoringComponent() { // ContinuousDelivery JEP
                @Override
                public String getDescription() {
                    return "The plugin could benefit from setting up the continuous delivery workflow.";
                }

                @Override
                public ScoringComponentResult getScore(Plugin $, Map<String, ProbeResult> probeResults) {
                    final ProbeResult probeResult = probeResults.get(ContinuousDeliveryProbe.KEY);
                    if (probeResult == null || ProbeResult.Status.ERROR.equals(probeResult.status())) {
                        return new ScoringComponentResult(0, getWeight(), List.of("Cannot confirm or not the JEP-229 configuration."));
                    }
                    return switch (probeResult.message()) {
                        case "JEP-229 workflow definition found." ->
                            new ScoringComponentResult(100, getWeight(), List.of("JEP-229 is configured on the plugin."));
                        case "Could not find JEP-229 workflow definition." ->
                            new ScoringComponentResult(0, getWeight(), List.of("JEP-229 is not configured on the plugin."));
                        default ->
                            new ScoringComponentResult(0, getWeight(), List.of("Cannot confirm or not the JEP-229 configuration.", probeResult.message()));
                    };
                }

                @Override
                public int getWeight() {
                    return 5;
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
        return """
            Scores plugin based on Jenkinsfile presence, documentation migration, dependabot and JEP-229 configuration.
            """;
    }

    @Override
    public int version() {
        return 3;
    }
}
