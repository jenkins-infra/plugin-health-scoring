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

package io.jenkins.pluginhealth.scoring.http;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.probes.Probe;
import io.jenkins.pluginhealth.scoring.service.PluginService;
import io.jenkins.pluginhealth.scoring.service.ProbeService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ImportAutoConfiguration(ProjectInfoAutoConfiguration.class)
@WebMvcTest(
    controllers = ProbesController.class
)
public class ProbeControllerTest {
    @MockBean private PluginService pluginService;
    @MockBean private ProbeService probeService;
    @Autowired private MockMvc mockMvc;

    @Test
    public void shouldDisplayListOfProbes() throws Exception {
        final Probe probe = mock(Probe.class);
        when(probeService.getProbes()).thenReturn(List.of(probe));

        mockMvc.perform(get("/probes"))
            .andExpect(status().isOk())
            .andExpect(view().name("probes/listing"))
            .andExpectAll(
                model().attributeExists("probes"),
                model().attribute("probes", hasSize(1))
            );

        verify(probeService).getProbes();
    }

    @Test
    public void shouldDisplayRawResultOfProbes() throws Exception {
        when(pluginService.getPluginsCount()).thenReturn(1L);
        when(probeService.getProbesFinalResults()).thenReturn(
            Map.of()
        );

        mockMvc.perform(get("/probes/results"))
            .andExpect(status().isOk())
            .andExpect(view().name("probes/results"))
            .andExpectAll(
                model().attributeExists("probeResults", "pluginsCount")
            );

        verify(pluginService).getPluginsCount();
        verify(probeService).getProbesFinalResults();
    }
}
