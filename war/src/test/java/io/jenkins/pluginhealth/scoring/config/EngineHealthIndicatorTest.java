/*
 * MIT License
 *
 * Copyright (c) 2026 Jenkins Infra
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
package io.jenkins.pluginhealth.scoring.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

class EngineHealthIndicatorTest {

    private static Stream<EngineHealthIndicator> indicators() {
        return Stream.of(new ProbeEngineHealthIndicator(), new ScoringEngineHealthIndicator());
    }

    /**
     * A never-run engine is reported as UNKNOWN rather than OUT_OF_SERVICE: the
     * application serves traffic normally before the first scheduled run, and
     * UNKNOWN keeps the aggregate health endpoint out of a 503.
     */
    @ParameterizedTest
    @MethodSource("indicators")
    void reportsUnknownWhenNeverRan(EngineHealthIndicator indicator) {
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
    }

    @ParameterizedTest
    @MethodSource("indicators")
    void reportsUpWithRunningSinceWhileRunIsInProgress(EngineHealthIndicator indicator) {
        ZonedDateTime start = ZonedDateTime.now();
        indicator.recordStart(start);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("runningSince", start);
    }

    @ParameterizedTest
    @MethodSource("indicators")
    void reportsDownWhenRunExceedsStuckThreshold(EngineHealthIndicator indicator) {
        ZonedDateTime start = ZonedDateTime.now().minus(EngineHealthIndicator.STUCK_RUN_THRESHOLD.plusMinutes(1));
        indicator.recordStart(start);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("runningSince", start);
        assertThat(health.getDetails().get("error").toString()).contains("has not completed");
    }

    @ParameterizedTest
    @MethodSource("indicators")
    void reportsUpWithLastSuccessAfterCompletedRun(EngineHealthIndicator indicator) {
        ZonedDateTime success = ZonedDateTime.now();
        indicator.recordStart(success.minusMinutes(5));
        indicator.recordSuccess(success);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("lastSuccess", success);
        assertThat(health.getDetails()).doesNotContainKey("runningSince");
    }

    @ParameterizedTest
    @MethodSource("indicators")
    void reportsDownWithErrorAfterFailedRun(EngineHealthIndicator indicator) {
        indicator.recordStart(ZonedDateTime.now());
        indicator.recordFailure(new RuntimeException("engine exploded"));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("error", "engine exploded");
        assertThat(health.getDetails()).doesNotContainKey("runningSince");
    }

    @ParameterizedTest
    @MethodSource("indicators")
    void startingNewRunClearsPreviousFailure(EngineHealthIndicator indicator) {
        indicator.recordFailure(new RuntimeException("transient error"));

        ZonedDateTime restart = ZonedDateTime.now();
        indicator.recordStart(restart);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("runningSince", restart);
        assertThat(health.getDetails()).doesNotContainKey("error");
    }

    @ParameterizedTest
    @MethodSource("indicators")
    void keepsLastSuccessVisibleWhileNextRunIsInProgress(EngineHealthIndicator indicator) {
        ZonedDateTime success = ZonedDateTime.now().minusHours(2);
        indicator.recordSuccess(success);
        indicator.recordStart(ZonedDateTime.now());

        Health health = indicator.health();

        assertThat(health.getDetails()).containsEntry("lastSuccess", success);
        assertThat(health.getDetails()).containsKey("runningSince");
    }

    @ParameterizedTest
    @MethodSource("indicators")
    void keepsLastSuccessVisibleAfterAFailedRun(EngineHealthIndicator indicator) {
        ZonedDateTime success = ZonedDateTime.now().minusHours(2);
        indicator.recordSuccess(success);
        indicator.recordFailure(new RuntimeException("engine exploded"));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("lastSuccess", success);
        assertThat(health.getDetails()).containsEntry("error", "engine exploded");
    }

    /**
     * The actuator component name is derived from the bean name, so the two
     * implementations must stay distinct types.
     */
    @Test
    void bothEnginesAreReportedIndependently() {
        var probe = new ProbeEngineHealthIndicator();
        var scoring = new ScoringEngineHealthIndicator();

        probe.recordSuccess(ZonedDateTime.now());

        assertThat(probe.health().getStatus()).isEqualTo(Status.UP);
        assertThat(scoring.health().getStatus()).isEqualTo(Status.UNKNOWN);
    }
}
