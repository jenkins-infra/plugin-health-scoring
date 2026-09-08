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

import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

@Component
public class ScoringEngineHealthIndicator extends AbstractHealthIndicator {

    private record RunState(ZonedDateTime lastSuccess, Throwable lastError) {}

    private final AtomicReference<RunState> state = new AtomicReference<>();

    public void recordSuccess(ZonedDateTime time) {
        state.set(new RunState(time, null));
    }

    public void recordFailure(Throwable t) {
        state.set(new RunState(null, t));
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        RunState current = state.get();
        if (current == null) {
            builder.outOfService();
        } else if (current.lastError() != null) {
            builder.down().withDetail("error", current.lastError().getMessage());
        } else {
            builder.up().withDetail("lastSuccess", current.lastSuccess());
        }
    }
}
