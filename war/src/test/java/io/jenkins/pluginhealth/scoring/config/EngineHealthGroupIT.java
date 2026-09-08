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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jenkins.pluginhealth.scoring.AbstractDBContainerTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.github.app-id=test",
    "app.github.private-key-path=/nonexistent",
    "app.github.app-installation-name=test"
})
class EngineHealthGroupIT extends AbstractDBContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProbeEngineHealthIndicator probeEngineHealth;

    @Autowired
    private ScoringEngineHealthIndicator scoringEngineHealth;

    @Test
    void livenessReturns200WhenBothEnginesAreDown() throws Exception {
        probeEngineHealth.recordFailure(new RuntimeException("probe engine down"));
        scoringEngineHealth.recordFailure(new RuntimeException("scoring engine down"));

        mockMvc.perform(get("/actuator/health/liveness"))
            .andExpect(status().isOk());
    }

    @Test
    void readinessReturns200WhenBothEnginesAreDown() throws Exception {
        probeEngineHealth.recordFailure(new RuntimeException("probe engine down"));
        scoringEngineHealth.recordFailure(new RuntimeException("scoring engine down"));

        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk());
    }

    @Test
    void engineIndicatorsAreVisibleInAggregateHealthEndpoint() throws Exception {
        probeEngineHealth.recordFailure(new RuntimeException("probe engine down"));

        mockMvc.perform(get("/actuator/health"))
            .andExpect(jsonPath("$.components.probeEngine.status").value("DOWN"));
    }
}
