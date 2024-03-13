/*
 * MIT License
 *
 * Copyright (c) 2022-2023 Jenkins Infra
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

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.jenkins.pluginhealth.scoring.config.SecurityConfiguration;
import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.Score;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
import io.jenkins.pluginhealth.scoring.scores.Scoring;
import io.jenkins.pluginhealth.scoring.scores.ScoringComponent;
import io.jenkins.pluginhealth.scoring.service.ScoreService;
import io.jenkins.pluginhealth.scoring.service.ScoringService;

import hudson.util.VersionNumber;
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
@ImportAutoConfiguration({ProjectInfoAutoConfiguration.class, SecurityConfiguration.class})
@WebMvcTest(controllers = ScoreController.class)
class ScoreControllerTest {
    @MockBean
    private ScoreService scoreService;

    @MockBean
    private ScoringService scoringService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldDisplayListOfScoring() throws Exception {
        when(scoringService.getScoringList()).thenReturn(List.of(new Scoring() {
            @Override
            public String key() {
                return "foo";
            }

            @Override
            public float weight() {
                return 1;
            }

            @Override
            public String description() {
                return "description";
            }

            @Override
            public List<ScoringComponent> getComponents() {
                return List.of();
            }

            @Override
            public int version() {
                return 0;
            }
        }));

        mockMvc.perform(get("/scores"))
                .andExpect(status().isOk())
                .andExpect(view().name("scores/listing"))
                .andExpect(model().attribute("module", "scores"))
                .andExpect(model().attributeExists("scorings"));
    }

    @Test
    void shouldDisplayScoreForSpecificPlugin() throws Exception {
        final String pluginName = "foo-bar";
        final String probeKey = "probe-key";
        final String scoreKey = "score-key";

        final Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn(pluginName);
        when(plugin.getDetails()).thenReturn(Map.of(probeKey, ProbeResult.success(probeKey, "message", 1)));
        when(plugin.getScm()).thenReturn("this-is-the-url-of-the-plugin-location");
        when(plugin.getReleaseTimestamp()).thenReturn(ZonedDateTime.now().minusHours(1));
        when(plugin.getVersion()).thenReturn(new VersionNumber("1.0"));

        final Score score = mock(Score.class);
        when(score.getPlugin()).thenReturn(plugin);
        when(score.getValue()).thenReturn(42L);
        when(score.getDetails()).thenReturn(Set.of(new ScoreResult(scoreKey, 42, 1f, Set.of(), 1)));

        when(scoreService.latestScoreFor(pluginName)).thenReturn(Optional.of(score));

        mockMvc.perform(get("/scores/{pluginName}", pluginName))
                .andExpect(status().isOk())
                .andExpect(view().name("scores/details"))
                .andExpect(model().attribute("module", "scores"))
                .andExpect(model().attributeExists("score"))
        /*.andExpect(model().attribute(
            "score",
            allOf(
                hasProperty("value", equalTo(42L)),
                hasProperty("pluginName", equalTo(pluginName)),
                hasProperty("probeResults", instanceOf(Map.class)),
                hasProperty("details", instanceOf(Collection.class))
            )
        ))*/ ; // TODO see https://github.com/hamcrest/JavaHamcrest/issues/392
    }

    @Test
    void shouldDisplayErrorWhenPluginUnknown() throws Exception {
        when(scoreService.latestScoreFor(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/scores/foo-bar"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("scores/unknown"))
                .andExpect(model().attribute("module", "scores"))
                .andExpect(model().attribute("pluginName", equalTo("foo-bar")));
    }
}
