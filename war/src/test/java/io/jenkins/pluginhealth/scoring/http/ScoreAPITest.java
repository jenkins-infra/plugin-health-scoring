/*
 * MIT License
 *
 * Copyright (c) 2023 Jenkins Infra
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.jenkins.pluginhealth.scoring.config.SecurityConfiguration;
import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.Score;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
import io.jenkins.pluginhealth.scoring.model.ScoringComponentResult;
import io.jenkins.pluginhealth.scoring.service.ScoreService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith({ SpringExtension.class, MockitoExtension.class })
@ImportAutoConfiguration({ ProjectInfoAutoConfiguration.class, SecurityConfiguration.class })
@WebMvcTest(
    controllers = ScoreAPI.class
)
class ScoreAPITest {
    @MockBean private ScoreService scoreService;
    @Autowired private MockMvc mockMvc;
    @Autowired ObjectMapper mapper;

    @Test
    void shouldBeAbleToProvideScoresSummary() throws Exception {
        final Plugin p1 = mock(Plugin.class);
        final Plugin p2 = mock(Plugin.class);

        final Score scoreP1 = new Score(p1, ZonedDateTime.now());
        scoreP1.addDetail(new ScoreResult("scoring-1", 100, 1, Set.of(
            new ScoringComponentResult(100, 1, List.of("There is no active security advisory for the plugin."))
        ), 1));

        final Score scoreP2 = new Score(p2, ZonedDateTime.now());
        scoreP2.addDetail(new ScoreResult("scoring-1", 100, 1, Set.of(
            new ScoringComponentResult(100, 1, List.of("There is no active security advisory for the plugin."))
        ), 1));
        scoreP2.addDetail(new ScoreResult("scoring-2", 50, 1, Set.of(
            new ScoringComponentResult(0, 1, List.of("There is no Jenkinsfile detected on the plugin repository.")),
            new ScoringComponentResult(100, .5f, List.of("The plugin documentation was migrated to its repository.")),
            new ScoringComponentResult(100, .5f, List.of("The plugin is using dependabot.", "0 open pull requests from dependency update tool."))
        ), 1));

        when(scoreService.getLatestScoresSummaryMap()).thenReturn(Map.of(
            "plugin-1", scoreP1,
            "plugin-2", scoreP2
        ));
        when(scoreService.getScoresStatistics()).thenReturn(new ScoreService.ScoreStatistics(
            87.5, 50, 100, 100, 100, 100
        ));

        mockMvc.perform(get("/api/scores"))
            .andExpectAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON),
                content().json("""
                    {
                        'plugins': {
                            'plugin-1': {
                                'value': 100,
                                'details': {
                                    'scoring-1': {
                                        'value': 100,
                                        'weight': 1,
                                        'components': [{
                                            'value': 100,
                                            'weight': 1,
                                            'reasons': ['There is no active security advisory for the plugin.']
                                        }]
                                    }
                                }
                            },
                            'plugin-2': {
                                'value': 75,
                                'details': {
                                    'scoring-1': {
                                        'value': 100,
                                        'weight': 1,
                                        'components': [{
                                            'value': 100,
                                            'weight': 1,
                                            'reasons': ['There is no active security advisory for the plugin.']
                                        }]
                                    },
                                    'scoring-2': {
                                        'value': 50,
                                        'weight': 1,
                                        'components': [{
                                            'value': 0,
                                            'weight': 1,
                                            'reasons': ['There is no Jenkinsfile detected on the plugin repository.']
                                        }, {
                                            'value': 100,
                                            'weight': .5,
                                            'reasons': ['The plugin documentation was migrated to its repository.']
                                        }, {
                                            'value': 100,
                                            'weight': .5,
                                            'reasons': [
                                                'The plugin is using dependabot.',
                                                '0 open pull requests from dependency update tool.'
                                            ]
                                        }]
                                    }
                                }
                            }
                        },
                        'statistics': {
                            'average': 87.5,
                            'minimum': 50,
                            'maximum': 100,
                            'firstQuartile': 100,
                            'median': 100,
                            'thirdQuartile': 100
                        }
                    }
                    """, false)
            );
    }
}
