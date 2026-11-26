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
package io.jenkins.pluginhealth.scoring.http;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.config.SecurityConfiguration;
import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.service.PluginService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ImportAutoConfiguration({ProjectInfoAutoConfiguration.class, SecurityConfiguration.class})
@WebMvcTest(controllers = ProbeAPI.class)
class ProbeAPITest {
    @MockitoBean
    private PluginService pluginService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsStoredProbeResults() throws Exception {
        Plugin plugin = new Plugin().addDetails(ProbeResult.success("maven", Map.of("version", "3.9"), 2));
        when(pluginService.findByName("git")).thenReturn(Optional.of(plugin));
        mockMvc.perform(get("/api/probes/git"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maven.message.version").value("3.9"))
                .andExpect(jsonPath("$.maven.status").value("SUCCESS"))
                .andExpect(jsonPath("$.maven.probeVersion").value(2));
    }

    @Test
    void returnsEmptyObjectForPluginWithoutResults() throws Exception {
        when(pluginService.findByName("git")).thenReturn(Optional.of(new Plugin()));
        mockMvc.perform(get("/api/probes/git"))
                .andExpect(status().isOk())
                .andExpect(content().string("{}"));
    }

    @Test
    void returnsNotFoundForUnknownPlugin() throws Exception {
        mockMvc.perform(get("/api/probes/unknown")).andExpect(status().isNotFound());
    }
}
