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

import io.jenkins.pluginhealth.scoring.model.ChangelogResult;
import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.probes.LastCommitDateProbe;
import io.jenkins.pluginhealth.scoring.probes.UpForAdoptionProbe;

import org.springframework.stereotype.Component;

@Component
public class AdoptionScoring extends Scoring {
    private static final float COEFFICIENT = 0.8f;
    private static final String KEY = "adoption";

    private abstract static class TimeSinceLastCommitChangelog extends Changelog {
        public final Duration getTimeBetweenLastCommitAndDate(String lastCommitDateMessage, ZonedDateTime then) {
            final ZonedDateTime commitDate = ZonedDateTime
                .parse(lastCommitDateMessage, DateTimeFormatter.ISO_DATE_TIME)
                .withZoneSameInstant(getZone());
            return Duration.between(commitDate, then);
        }

        protected ZoneId getZone() {
            return ZoneId.of("UTC");
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
                public ChangelogResult getScore(Plugin $, Map<String, ProbeResult> probeResults) {
                    final ProbeResult probeResult = probeResults.get(UpForAdoptionProbe.KEY);
                    if (probeResult == null || ProbeResult.Status.ERROR.equals(probeResult.status())) {
                        return new ChangelogResult(-100, 100, List.of("Cannot determine if the plugin is up for adoption."));
                    }

                    return switch (probeResult.message()) {
                        case "This plugin is not up for adoption." ->
                            new ChangelogResult(100, getWeight(), List.of("The plugin is not marked as up for adoption"));
                        case "This plugin is up for adoption." ->
                            new ChangelogResult(0, getWeight(), List.of("The plugin is marked as up for adoption"));
                        default -> new ChangelogResult(-100, getWeight(), List.of());
                    };
                }

                @Override
                public int getWeight() {
                    return 10;
                }
            },
            new TimeSinceLastCommitChangelog() {
                @Override
                public String getDescription() {
                    return "The plugin must have a commit in the last 6 months.";
                }

                @Override
                public ChangelogResult getScore(Plugin plugin, Map<String, ProbeResult> probeResults) {
                    final ProbeResult probeResult = probeResults.get(LastCommitDateProbe.KEY);
                    if (probeResult == null || ProbeResult.Status.ERROR.equals(probeResult.status())) {
                        return new ChangelogResult(-100, 100, List.of("Cannot determine the last commit date."));
                    }

                    if (getTimeBetweenLastCommitAndDate(probeResult.message(), plugin.getReleaseTimestamp().withZoneSameInstant(getZone())).toDays() <= Duration.of(6 * 30, ChronoUnit.DAYS).toDays()) {
                        return new ChangelogResult(100, getWeight(), List.of("At least one commit happened in the last 6 months."));
                    }
                    return new ChangelogResult(0, getWeight(), List.of("No commit in the last 6 months."));
                }
            },
            new TimeSinceLastCommitChangelog() {
                @Override
                public String getDescription() {
                    return "The plugin must have a commit in the last year";
                }

                @Override
                public ChangelogResult getScore(Plugin plugin, Map<String, ProbeResult> probeResults) {
                    final ProbeResult probeResult = probeResults.get(LastCommitDateProbe.KEY);
                    if (probeResult == null || ProbeResult.Status.ERROR.equals(probeResult.status())) {
                        return new ChangelogResult(-100, 100, List.of("Cannot determine the last commit date."));
                    }
                    if (getTimeBetweenLastCommitAndDate(probeResult.message(), plugin.getReleaseTimestamp().withZoneSameInstant(getZone())).toDays() > Duration.of(6 * 30, ChronoUnit.DAYS).toDays() &&
                        getTimeBetweenLastCommitAndDate(probeResult.message(), plugin.getReleaseTimestamp().withZoneSameInstant(getZone())).toDays() <= Duration.of(365, ChronoUnit.DAYS).toDays()) {
                        return new ChangelogResult(75, getWeight(), List.of("At least one commit happened in the last year."));
                    }
                    return new ChangelogResult(0, getWeight(), List.of("No commit in the last year."));
                }
            },
            new TimeSinceLastCommitChangelog() {
                @Override
                public String getDescription() {
                    return "The plugin must have a commit in the last 2 years.";
                }

                @Override
                public ChangelogResult getScore(Plugin plugin, Map<String, ProbeResult> probeResults) {
                    final ProbeResult probeResult = probeResults.get(LastCommitDateProbe.KEY);
                    if (probeResult == null || ProbeResult.Status.ERROR.equals(probeResult.status())) {
                        return new ChangelogResult(-100, 100, List.of("Cannot determine the last commit date."));
                    }
                    if (getTimeBetweenLastCommitAndDate(probeResult.message(), plugin.getReleaseTimestamp().withZoneSameInstant(getZone())).toDays() > Duration.of(365, ChronoUnit.DAYS).toDays() &&
                        getTimeBetweenLastCommitAndDate(probeResult.message(), plugin.getReleaseTimestamp().withZoneSameInstant(getZone())).toDays() <= Duration.of(2 * 365, ChronoUnit.DAYS).toDays()) {
                        return new ChangelogResult(50, getWeight(), List.of("At least one commit happened in the last 2 years."));
                    }
                    return new ChangelogResult(0, getWeight(), List.of("No commit in the last 2 years."));
                }
            },
            new TimeSinceLastCommitChangelog() {
                @Override
                public String getDescription() {
                    return "The plugin must have a commit in the last 4 years.";
                }

                @Override
                public ChangelogResult getScore(Plugin plugin, Map<String, ProbeResult> probeResults) {
                    final ProbeResult probeResult = probeResults.get(LastCommitDateProbe.KEY);
                    if (probeResult == null || ProbeResult.Status.ERROR.equals(probeResult.status())) {
                        return new ChangelogResult(-100, 100, List.of("Cannot determine the last commit date."));
                    }
                    if (getTimeBetweenLastCommitAndDate(probeResult.message(), plugin.getReleaseTimestamp().withZoneSameInstant(getZone())).toDays() > Duration.of(2 * 365, ChronoUnit.DAYS).toDays() &&
                        getTimeBetweenLastCommitAndDate(probeResult.message(), plugin.getReleaseTimestamp().withZoneSameInstant(getZone())).toDays() <= Duration.of(4 * 365, ChronoUnit.DAYS).toDays()) {
                        return new ChangelogResult(25, getWeight(), List.of("At least one commit happened in the last 4 years."));
                    }
                    return new ChangelogResult(0, getWeight(), List.of("No commit in the last 4 years."));
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
