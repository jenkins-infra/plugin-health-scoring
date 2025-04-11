/*
 * MIT License
 *
 * Copyright (c) 2022-2025 Jenkins Infra
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

import java.io.IOException;

import io.jenkins.pluginhealth.scoring.probes.ProbeEngine;
import io.jenkins.pluginhealth.scoring.scores.ScoringEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevProbeEngineScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DevProbeEngineScheduler.class);

    private final ProbeEngine probeEngine;
    private final ScoringEngine scoringEngine;

    public DevProbeEngineScheduler(ProbeEngine probeEngine, ScoringEngine scoringEngine) {
        this.probeEngine = probeEngine;
        this.scoringEngine = scoringEngine;
    }

    @Async
    @Scheduled(initialDelay = 10 * 1000 /* 10 secs after startup */, fixedDelay = 1000 * 60 * 90 * 1)
    public void run() throws IOException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Loaded via dev profile, so we're using initialDelay and fixedDelay");
        }
        probeEngine.run();
        scoringEngine.run();
    }
}
