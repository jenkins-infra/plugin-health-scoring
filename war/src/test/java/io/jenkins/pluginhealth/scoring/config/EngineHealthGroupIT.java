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

import java.time.ZonedDateTime;

import io.jenkins.pluginhealth.scoring.AbstractDBContainerTest;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GitHub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
        properties = {
            "app.github.app-id=test",
            "app.github.private-key-path=/nonexistent",
            "app.github.app-installation-name=test"
        })
class EngineHealthGroupIT extends AbstractDBContainerTest {

    @MockitoBean
    private GitHub github;

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

        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
    }

    @Test
    void readinessReturns200WhenBothEnginesAreDown() throws Exception {
        probeEngineHealth.recordFailure(new RuntimeException("probe engine down"));
        scoringEngineHealth.recordFailure(new RuntimeException("scoring engine down"));

        mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    }

    @Test
    void engineIndicatorsAreVisibleInAggregateHealthEndpoint() throws Exception {
        probeEngineHealth.recordFailure(new RuntimeException("probe engine down"));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(jsonPath("$.components.probeEngine.status").value("DOWN"));
    }

    /**
     * Guards the tests below: a path outside the permit-all matcher must be rejected,
     * proving the security filter chain is actually applied in this context.
     */
    @Test
    void nonAllowlistedPathIsDenied() throws Exception {
        mockMvc.perform(get("/not-allowlisted")).andExpect(status().is4xxClientError());
    }

    @Test
    void aggregateHealthEndpointIsAccessibleWithoutAuthentication() throws Exception {
        probeEngineHealth.recordSuccess(ZonedDateTime.now());
        scoringEngineHealth.recordSuccess(ZonedDateTime.now());

        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void aggregateHealthEndpointReturns503WhenAnEngineIsDown() throws Exception {
        probeEngineHealth.recordFailure(new RuntimeException("probe engine down"));
        scoringEngineHealth.recordSuccess(ZonedDateTime.now());

        mockMvc.perform(get("/actuator/health")).andExpect(status().isServiceUnavailable());
    }

    @Test
    void probeEngineHealthEndpointIsAccessibleWithoutAuthentication() throws Exception {
        probeEngineHealth.recordSuccess(ZonedDateTime.now());

        mockMvc.perform(get("/actuator/health/probeEngine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void scoringEngineHealthEndpointIsAccessibleWithoutAuthentication() throws Exception {
        scoringEngineHealth.recordSuccess(ZonedDateTime.now());

        mockMvc.perform(get("/actuator/health/scoringEngine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void enginesGroupIsAccessibleWithoutAuthentication() throws Exception {
        probeEngineHealth.recordSuccess(ZonedDateTime.now());
        scoringEngineHealth.recordSuccess(ZonedDateTime.now());

        mockMvc.perform(get("/actuator/health/engines")).andExpect(status().isOk());
    }

    @Test
    void lastSuccessTimestampIsVisibleInEnginesGroup() throws Exception {
        probeEngineHealth.recordSuccess(ZonedDateTime.now());
        scoringEngineHealth.recordSuccess(ZonedDateTime.now());

        mockMvc.perform(get("/actuator/health/engines"))
                .andExpect(
                        jsonPath("$.components.probeEngine.details.lastSuccess").exists())
                .andExpect(jsonPath("$.components.scoringEngine.details.lastSuccess")
                        .exists());
    }

    @Test
    void engineErrorIsVisibleInEnginesGroup() throws Exception {
        probeEngineHealth.recordFailure(new RuntimeException("update center unreachable"));

        mockMvc.perform(get("/actuator/health/engines"))
                .andExpect(jsonPath("$.components.probeEngine.details.error").value("update center unreachable"));
    }

    /**
     * The engines group is the only place details are exposed: the public aggregate
     * endpoint must keep reporting statuses without leaking component internals.
     */
    @Test
    void aggregateHealthEndpointDoesNotExposeDetails() throws Exception {
        probeEngineHealth.recordSuccess(ZonedDateTime.now());
        scoringEngineHealth.recordSuccess(ZonedDateTime.now());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(jsonPath("$.components.db.details").doesNotExist())
                .andExpect(jsonPath("$.components.probeEngine.details").doesNotExist());
    }
}
