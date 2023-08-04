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

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.probes.HasUnreleasedProductionChangesProbe;
import io.jenkins.pluginhealth.scoring.probes.LastCommitDateProbe;
import io.jenkins.pluginhealth.scoring.probes.UpForAdoptionProbe;

import org.springframework.stereotype.Component;

@Component
public class AdoptionScoring extends Scoring {
    private static final float COEFFICIENT = 0.8f;
    private static final String KEY = "adoption";

    private static abstract class TimeSinceLastCommitChangelog extends Changelog {
        public final Duration getTimeBetweenLastCommitAndNow(String lastCommitDateMessage) {
            final ZoneId utc = ZoneId.of("UTC");
            final ZonedDateTime commitDate = ZonedDateTime
                .parse(lastCommitDateMessage, DateTimeFormatter.ISO_DATE_TIME)
                .withZoneSameInstant(utc);
            final ZonedDateTime now = ZonedDateTime.now(utc);
            return Duration.between(now, commitDate);
        }

        @Override
        public final int getWeight() {
            return 1;
        }
    }

    @Override
    public List<Changelog> getChangelog() {
        return List.of(
            new Changelog() {
                @Override
                public String getDescription() {
                    return "The plugin must not be marked as up for adoption.";
                }

                @Override
                public ChangelogResult getScore(Map<String, ProbeResult> probeResults) {
                    final ProbeResult upForAdoptionResult = probeResults.get(UpForAdoptionProbe.KEY);
                    if (ProbeResult.Status.ERROR.equals(upForAdoptionResult.status())) {
                        return new ChangelogResult(-100, 100, List.of("Cannot determine if the plugin is up for adoption."));
                    }

                    return switch (upForAdoptionResult.message()) {
                        case "This plugin is not up for adoption." ->
                            new ChangelogResult(1, weight(), List.of("The plugin is marked as up for adoption"));
                        case "This plugin is up for adoption." ->
                            new ChangelogResult(0, weight(), List.of("The plugin is not marked as up for adoption"));
                        default -> new ChangelogResult(-10, weight(), List.of());
                    };
                }

                @Override
                public int getWeight() {
                    return 5;
                }
            },
            new TimeSinceLastCommitChangelog() {
                @Override
                public String getDescription() {
                    return "The plugin must have a commit in the last 6 months.";
                }

                @Override
                public ChangelogResult getScore(Map<String, ProbeResult> probeResults) {
                    final ProbeResult probeResult = probeResults.get(LastCommitDateProbe.KEY);
                    if (ProbeResult.Status.ERROR.equals(probeResult.status())) {
                        return new ChangelogResult(-100, 100, List.of("Cannot determine the last commit date."));
                    }

                    if (getTimeBetweenLastCommitAndNow(probeResult.message()).toDays() <= Duration.of(6, ChronoUnit.MONTHS).toDays()) {
                        return new ChangelogResult(1, weight(), List.of("At least one commit happened in the last 6 months."));
                    }
                    return new ChangelogResult(0, weight(), List.of("No commit in the last 6 months."));
                }
            },
            new TimeSinceLastCommitChangelog() {
                @Override
                public String getDescription() {
                    return "The plugin must have a commit in the last year";
                }

                @Override
                public ChangelogResult getScore(Map<String, ProbeResult> probeResults) {
                    final ProbeResult probeResult = probeResults.get(LastCommitDateProbe.KEY);
                    if (ProbeResult.Status.ERROR.equals(probeResult.status())) {
                        return new ChangelogResult(-100, 100, List.of("Cannot determine the last commit date."));
                    }
                    if (getTimeBetweenLastCommitAndNow(probeResult.message()).toDays() <= Duration.of(1, ChronoUnit.YEARS).toDays()) {
                        return new ChangelogResult(.75f, weight(), List.of("At least one commit happened in the last year."));
                    }
                    return new ChangelogResult(0, weight(), List.of("No commit in the last year."));
                }
            },
            new TimeSinceLastCommitChangelog() {
                @Override
                public String getDescription() {
                    return "The plugin must have a commit in the last 2 years.";
                }

                @Override
                public ChangelogResult getScore(Map<String, ProbeResult> probeResults) {
                    final ProbeResult probeResult = probeResults.get(LastCommitDateProbe.KEY);
                    if (ProbeResult.Status.ERROR.equals(probeResult.status())) {
                        return new ChangelogResult(-100, 100, List.of("Cannot determine the last commit date."));
                    }
                    if (getTimeBetweenLastCommitAndNow(probeResult.message()).toDays() <= Duration.of(2, ChronoUnit.YEARS).toDays()) {
                        return new ChangelogResult(.5f, weight(), List.of("At least one commit happened in the last 2 years."));
                    }
                    return new ChangelogResult(0, weight(), List.of("No commit in the last 2 years."));
                }
            },
            new TimeSinceLastCommitChangelog() {
                @Override
                public String getDescription() {
                    return "The plugin must have a commit in the last 4 years.";
                }

                @Override
                public ChangelogResult getScore(Map<String, ProbeResult> probeResults) {
                    final ProbeResult probeResult = probeResults.get(LastCommitDateProbe.KEY);
                    if (ProbeResult.Status.ERROR.equals(probeResult.status())) {
                        return new ChangelogResult(-100, 100, List.of("Cannot determine the last commit date."));
                    }
                    if (getTimeBetweenLastCommitAndNow(probeResult.message()).toDays() <= Duration.of(4, ChronoUnit.YEARS).toDays()) {
                        return new ChangelogResult(.25f, weight(), List.of("At least one commit happened in the last 4 years."));
                    }
                    return new ChangelogResult(0, weight(), List.of("No commit in the last 4 years."));
                }
            },
            new Changelog() {
                @Override
                public String getDescription() {
                    return "Plugin should not have unreleased changes on production code.";
                }

                @Override
                public ChangelogResult getScore(Map<String, ProbeResult> probeResults) {
                    final ProbeResult probeResult = probeResults.get(HasUnreleasedProductionChangesProbe.KEY);
                    if (ProbeResult.Status.ERROR.equals(probeResult.status())) {
                        return new ChangelogResult(-100, 100, List.of("Cannot determine if the plugin has unreleased production changes."));
                    }

                    if ("All production modifications were released.".equals(probeResult.message())) {
                        return new ChangelogResult(1, getWeight(), List.of("The plugins does not have unreleased changes on productions code."));
                    }

                    if (probeResult.message().startsWith("Unreleased production modifications might exist")) {
                        return new ChangelogResult(0, getWeight(), List.of("The plugins might have unreleased changes on productions code.", probeResult.message()));
                    }
                    return new ChangelogResult(-5, getWeight(), List.of("Cannot determine if the plugin has unreleased production changes or not.", probeResult.message()));
                }

                @Override
                public int getWeight() {
                    return 2;
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
}
