/*
 * MIT License
 *
 * Copyright (c) 2022 Jenkins Infra
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
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
import io.jenkins.pluginhealth.scoring.probes.LastCommitDateProbe;
import io.jenkins.pluginhealth.scoring.probes.UpForAdoptionProbe;

import org.springframework.stereotype.Component;

@Component
public class AdoptionScoring extends Scoring {
    private static final float COEFFICIENT = 0.8f;
    private static final String KEY = "adoption";

    @Override
    protected ScoreResult doApply(Plugin plugin) {
        final ProbeResult adoptionProbeResult = plugin.getDetails().get(UpForAdoptionProbe.KEY);
        if (adoptionProbeResult != null && adoptionProbeResult.status().equals(ResultStatus.FAILURE)) {
            return new ScoreResult(KEY, 0, COEFFICIENT);
        }

        final ProbeResult lastCommitProbeResult = plugin.getDetails().get(LastCommitDateProbe.KEY);
        if (lastCommitProbeResult != null && lastCommitProbeResult.status().equals(ResultStatus.SUCCESS)) {
            final String message = lastCommitProbeResult.message();
            final ZonedDateTime commitDateTime = ZonedDateTime.parse(message);

            final Duration between = Duration.between(plugin.getReleaseTimestamp().toInstant(), commitDateTime.toInstant());
            if (between.toDays() <= Duration.of(6 * 30, ChronoUnit.DAYS).toDays()) { // Less than 6 months
                return new ScoreResult(KEY, 1, COEFFICIENT);
            } else if (between.toDays() < Duration.of(365, ChronoUnit.DAYS).toDays()) { // Less than a year
                return new ScoreResult(KEY, .75f, COEFFICIENT);
            } else if (between.toDays() < Duration.of(2 * 365, ChronoUnit.DAYS).toDays()) { // Less than 2 years
                return new ScoreResult(KEY, .5f, COEFFICIENT);
            } else if (between.toDays() < Duration.of(4 * 365, ChronoUnit.DAYS).toDays()) { // Less than 4 years
                return new ScoreResult(KEY, .25f, COEFFICIENT);
            }
        }

        return new ScoreResult(KEY, 0, COEFFICIENT);
    }
}
