/*
 * MIT License
 *
 * Copyright (c) 2025 Jenkins Infra
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.config.SecurityConfiguration;
import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.service.PluginService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ImportAutoConfiguration({ProjectInfoAutoConfiguration.class, SecurityConfiguration.class})
@WebMvcTest(controllers = IndexController.class)
public class IndexControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PluginService pluginService;

    @Test
    void shouldBeAbleToAccessIndexPage() throws Exception {
        mockMvc.perform(get("/"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(model().attribute("module", "index"))
                .andExpect(view().name("index"));
    }

    @Test
    void shouldRequireCsrf() throws Exception {
        mockMvc.perform(post("/")).andExpect(status().isForbidden());
        mockMvc.perform(post("/").with(csrf().useInvalidToken())).andExpect(status().isForbidden());
    }

    @Test
    void shouldBeAbleToSearch() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("mailer");
        when(pluginService.search("mailer")).thenReturn(List.of(plugin));

        mockMvc.perform(post("/").with(csrf()).formField("search", "mailer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/scores/mailer"));
    }

    @Test
    void shouldBeAbleToSearchWithMultipleResult() throws Exception {
        final Plugin p1 = mock(Plugin.class);
        final Plugin p2 = mock(Plugin.class);
        final List<Plugin> expectedResult = List.of(p1, p2);

        when(pluginService.search("ma")).thenReturn(expectedResult);

        mockMvc.perform(post("/").with(csrf()).formField("search", "ma"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("results", expectedResult));
    }

    @Test
    void shouldStayOnIndexWithIncorrectPlugin() throws Exception {
        when(pluginService.findByName(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(post("/").with(csrf()).formField("search", "invalid-plugin-name "))
                .andExpect(status().isNotFound())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("error", "No plugin found with query 'invalid-plugin-name'"));
    }
}
