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

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;

/**
 * Tracks the outcome of an engine's scheduled runs and exposes it as a health contributor.
 * <p>
 * A run that never happened is reported as {@link org.springframework.boot.health.contributor.Status#UNKNOWN},
 * so that the aggregate health endpoint is not degraded before the first scheduled run.
 * A run still in progress past {@link #STUCK_RUN_THRESHOLD} is reported as
 * {@link org.springframework.boot.health.contributor.Status#DOWN}, which is what makes a wedged
 * engine distinguishable from one that simply has not run yet.
 */
public abstract class EngineHealthIndicator extends AbstractHealthIndicator {
    static final Duration STUCK_RUN_THRESHOLD = Duration.ofHours(6);

    private record RunState(ZonedDateTime startedAt, ZonedDateTime lastSuccess, Throwable lastError) {}

    private final AtomicReference<RunState> state = new AtomicReference<>();

    public void recordStart(ZonedDateTime time) {
        state.updateAndGet(previous -> new RunState(time, lastSuccessOf(previous), null));
    }

    public void recordSuccess(ZonedDateTime time) {
        state.set(new RunState(null, time, null));
    }

    public void recordFailure(Throwable t) {
        state.updateAndGet(previous -> new RunState(null, lastSuccessOf(previous), t));
    }

    private static ZonedDateTime lastSuccessOf(RunState state) {
        return Objects.isNull(state) ? null : state.lastSuccess();
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        final RunState current = state.get();
        if (Objects.isNull(current)) {
            builder.unknown();
            return;
        }

        if (Objects.nonNull(current.lastError())) {
            builder.down().withDetail("error", current.lastError().getMessage());
        } else if (Objects.nonNull(current.startedAt())) {
            describeRunInProgress(builder, current.startedAt());
        } else {
            builder.up();
        }

        Optional.ofNullable(current.lastSuccess())
                .ifPresent(lastSuccess -> builder.withDetail("lastSuccess", lastSuccess));
    }

    private static void describeRunInProgress(Health.Builder builder, ZonedDateTime startedAt) {
        final Duration elapsed = Duration.between(startedAt, ZonedDateTime.now());
        if (elapsed.compareTo(STUCK_RUN_THRESHOLD) >= 0) {
            builder.down().withDetail("error", "run started %s ago has not completed".formatted(elapsed));
        } else {
            builder.up();
        }
        builder.withDetail("runningSince", startedAt);
    }
}
