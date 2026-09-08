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

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

class ProbeEngineHealthIndicatorTest {

    @Test
    void reportsOutOfServiceWhenNeverRan() {
        var indicator = new ProbeEngineHealthIndicator();
        Health health = indicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
    }

    @Test
    void reportsUpWithLastSuccessTimestampAfterSuccessfulRun() {
        var indicator = new ProbeEngineHealthIndicator();
        ZonedDateTime successTime = ZonedDateTime.now();
        indicator.recordSuccess(successTime);

        Health health = indicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("lastSuccess");
        assertThat(health.getDetails().get("lastSuccess")).isEqualTo(successTime);
    }

    @Test
    void reportsDownWithErrorDetailAfterFailedRun() {
        var indicator = new ProbeEngineHealthIndicator();
        indicator.recordFailure(new RuntimeException("probe engine exploded"));

        Health health = indicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
        assertThat(health.getDetails().get("error")).isEqualTo("probe engine exploded");
    }

    @Test
    void reportsUpAfterRecoveringFromFailure() {
        var indicator = new ProbeEngineHealthIndicator();
        indicator.recordFailure(new RuntimeException("transient error"));
        indicator.recordSuccess(ZonedDateTime.now());

        Health health = indicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("lastSuccess");
        assertThat(health.getDetails()).doesNotContainKey("error");
    }
}
