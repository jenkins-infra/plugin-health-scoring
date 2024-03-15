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

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.Resolution;
import io.jenkins.pluginhealth.scoring.model.ScoringComponentResult;
import io.jenkins.pluginhealth.scoring.probes.LastCommitDateProbe;
import io.jenkins.pluginhealth.scoring.probes.UpForAdoptionProbe;

import org.springframework.stereotype.Component;

@Component
public class AdoptionScoring extends Scoring {
    private static final float COEFFICIENT = 0.8f;
    private static final String KEY = "adoption";

    private abstract static class TimeSinceLastCommitScoringComponent implements ScoringComponent {
        protected final Duration getTimeBetweenLastCommitAndDate(String lastCommitDateMessage, ZonedDateTime then) {
            final ZonedDateTime commitDate = ZonedDateTime
                .parse(lastCommitDateMessage, DateTimeFormatter.ISO_DATE_TIME)
                .withZoneSameInstant(getZone());
            return Duration.between(then, commitDate);
        }

        protected ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public int getWeight() {
            return 1;
        }
    }

    @Override
    public List<ScoringComponent> getComponents() {
        return List.of(
            new ScoringComponent() {
                @Override
                public String getDescription() {
                    return "The plugin must not be marked as up for adoption.";
                }

                @Override
                public ScoringComponentResult getScore(Plugin $, Map<String, ProbeResult> probeResults) {
                    final ProbeResult probeResult = probeResults.get(UpForAdoptionProbe.KEY);
                    if (probeResult == null || ProbeResult.Status.ERROR.equals(probeResult.status())) {
                        return new ScoringComponentResult(-1000, 1000, List.of("Cannot determine if the plugin is up for adoption."));
                    }

                    return switch (probeResult.message()) {
                        case "This plugin is not up for adoption." ->
                            new ScoringComponentResult(100, getWeight(), List.of("The plugin is not marked as up for adoption."));
                        case "This plugin is up for adoption." ->
                            new ScoringComponentResult(
                                -1000,
                                getWeight(),
                                List.of("The plugin is marked as up for adoption."),
                                List.of(
                                    new Resolution("See adoption guidelines", "https://www.jenkins.io/doc/developer/plugin-governance/adopt-a-plugin/#plugins-marked-for-adoption")
                                )
                            );
                        default -> new ScoringComponentResult(-100, getWeight(), List.of());
                    };
                }

                @Override
                public int getWeight() {
                    return 1;
                }
            },
            new TimeSinceLastCommitScoringComponent() {
                @Override
                public String getDescription() {
                    return "There must be a reasonable time gap between last release and last commit.";
                }

                @Override
                public ScoringComponentResult getScore(Plugin plugin, Map<String, ProbeResult> probeResults) {
                    final ProbeResult probeResult = probeResults.get(LastCommitDateProbe.KEY);
                    if (probeResult == null || ProbeResult.Status.ERROR.equals(probeResult.status())) {
                        return new ScoringComponentResult(-100, 100, List.of("Cannot determine the last commit date."));
                    }
                    final long days = getTimeBetweenLastCommitAndDate(probeResult.message(), plugin.getReleaseTimestamp().withZoneSameInstant(getZone())).toDays();
                    if (days < 0) {
                        return new ScoringComponentResult(100, getWeight(), List.of("The latest release is more recent than the latest commit on the plugin."));
                    }
                    final String defaultReason = "There are %d days between last release and last commit.".formatted(days);
                    if (days < Duration.of(30 * 6, ChronoUnit.DAYS).toDays()) {
                        return new ScoringComponentResult(100, getWeight(), List.of(defaultReason, "Less than 6 months gap between last release and last commit."));
                    }
                    if (days < Duration.of((30 * 12) + 1, ChronoUnit.DAYS).toDays()) {
                        return new ScoringComponentResult(60, getWeight(), List.of(defaultReason, "Less than a year between last release and last commit."));
                    }
                    if (days < Duration.of((30 * 12 * 2) + 1, ChronoUnit.DAYS).toDays()) {
                        return new ScoringComponentResult(20, getWeight(), List.of(defaultReason, "Less than 2 years between last release and last commit."));
                    }
                    if (days < Duration.of((30 * 12 * 4) + 1, ChronoUnit.DAYS).toDays()) {
                        return new ScoringComponentResult(10, 2, List.of(defaultReason, "Less than 4 years between last release and last commit."));
                    }
                    return new ScoringComponentResult(-1000, getWeight(), List.of("There is more than 4 years between the last release and the last commit."));
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
        return "Scores plugin based on the time between the last commit and the last release.";
    }

    @Override
    public int version() {
        return 4;
    }
}
