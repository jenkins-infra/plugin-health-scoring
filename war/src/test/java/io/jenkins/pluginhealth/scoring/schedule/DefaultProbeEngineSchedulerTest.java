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
package io.jenkins.pluginhealth.scoring.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.IOException;

import io.jenkins.pluginhealth.scoring.config.ProbeEngineHealthIndicator;
import io.jenkins.pluginhealth.scoring.config.ScoringEngineHealthIndicator;
import io.jenkins.pluginhealth.scoring.probes.ProbeEngine;
import io.jenkins.pluginhealth.scoring.scores.ScoringEngine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Status;

@ExtendWith(MockitoExtension.class)
class DefaultProbeEngineSchedulerTest {

    @Mock
    private ProbeEngine probeEngine;

    @Mock
    private ScoringEngine scoringEngine;

    @Test
    void recordsBothSuccessesAfterSuccessfulRun() throws IOException {
        var probeHealth = new ProbeEngineHealthIndicator();
        var scoringHealth = new ScoringEngineHealthIndicator();
        var scheduler = new DefaultProbeEngineScheduler(probeEngine, scoringEngine, probeHealth, scoringHealth);

        scheduler.run();

        assertThat(probeHealth.health().getStatus()).isEqualTo(Status.UP);
        assertThat(scoringHealth.health().getStatus()).isEqualTo(Status.UP);
        assertThat(probeHealth.health().getDetails()).containsKey("lastSuccess");
        assertThat(scoringHealth.health().getDetails()).containsKey("lastSuccess");
    }

    @Test
    void recordsProbeFailureAndRethrowsWhenProbeEngineThrows() throws IOException {
        var probeHealth = new ProbeEngineHealthIndicator();
        var scoringHealth = new ScoringEngineHealthIndicator();
        var scheduler = new DefaultProbeEngineScheduler(probeEngine, scoringEngine, probeHealth, scoringHealth);

        doThrow(new IOException("update center unreachable")).when(probeEngine).run();

        assertThatThrownBy(scheduler::run).isInstanceOf(IOException.class);

        assertThat(probeHealth.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(probeHealth.health().getDetails().get("error")).isEqualTo("update center unreachable");
    }

    @Test
    void doesNotRunScoringWhenProbeEngineThrows() throws IOException {
        var probeHealth = new ProbeEngineHealthIndicator();
        var scoringHealth = new ScoringEngineHealthIndicator();
        var scheduler = new DefaultProbeEngineScheduler(probeEngine, scoringEngine, probeHealth, scoringHealth);

        doThrow(new IOException("update center unreachable")).when(probeEngine).run();

        assertThatThrownBy(scheduler::run).isInstanceOf(IOException.class);

        verifyNoInteractions(scoringEngine);
        assertThat(scoringHealth.health().getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
    }

    @Test
    void recordsScoringFailureWhenScoringEngineThrows() throws IOException {
        var probeHealth = new ProbeEngineHealthIndicator();
        var scoringHealth = new ScoringEngineHealthIndicator();
        var scheduler = new DefaultProbeEngineScheduler(probeEngine, scoringEngine, probeHealth, scoringHealth);

        doThrow(new RuntimeException("scoring failed")).when(scoringEngine).run();

        scheduler.run();

        assertThat(probeHealth.health().getStatus()).isEqualTo(Status.UP);
        assertThat(scoringHealth.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(scoringHealth.health().getDetails().get("error")).isEqualTo("scoring failed");
    }

    @Test
    void doesNotPropagateExceptionWhenScoringEngineThrows() throws IOException {
        var probeHealth = new ProbeEngineHealthIndicator();
        var scoringHealth = new ScoringEngineHealthIndicator();
        var scheduler = new DefaultProbeEngineScheduler(probeEngine, scoringEngine, probeHealth, scoringHealth);

        doThrow(new RuntimeException("scoring failed")).when(scoringEngine).run();

        scheduler.run();

        verify(probeEngine).run();
        verify(scoringEngine).run();
    }
}
