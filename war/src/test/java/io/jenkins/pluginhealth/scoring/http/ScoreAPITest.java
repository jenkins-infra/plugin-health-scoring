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
package io.jenkins.pluginhealth.scoring.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ImportAutoConfiguration({ProjectInfoAutoConfiguration.class, SecurityConfiguration.class})
@WebMvcTest(controllers = ScoreAPI.class)
class ScoreAPITest {
    @MockitoBean
    private ScoreService scoreService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    ObjectMapper mapper;

    @Test
    void shouldBeAbleToProvideScoresSummary() throws Exception {
        final Plugin p1 = mock(Plugin.class);
        final Plugin p2 = mock(Plugin.class);

        final ZonedDateTime scoreP1Date = ZonedDateTime.now().minusMinutes(1);
        final Score scoreP1 = new Score(p1, scoreP1Date);
        scoreP1.addDetail(new ScoreResult(
                "scoring-1",
                100,
                1,
                Set.of(new ScoringComponentResult(
                        100, 1, List.of("There is no active security advisory for the plugin."))),
                1));

        final ZonedDateTime scoreP2Date = ZonedDateTime.now().minusDays(1);
        final Score scoreP2 = new Score(p2, scoreP2Date);
        scoreP2.addDetail(new ScoreResult(
                "scoring-1",
                100,
                1,
                Set.of(new ScoringComponentResult(
                        100, 1, List.of("There is no active security advisory for the plugin."))),
                1));
        scoreP2.addDetail(new ScoreResult(
                "scoring-2",
                50,
                1,
                Set.of(
                        new ScoringComponentResult(
                                0, 1, List.of("There is no Jenkinsfile detected on the plugin repository.")),
                        new ScoringComponentResult(
                                100, .5f, List.of("The plugin documentation was migrated to its repository.")),
                        new ScoringComponentResult(
                                100,
                                .5f,
                                List.of(
                                        "The plugin is using dependabot.",
                                        "0 open pull requests from dependency update tool."))),
                1));

        when(scoreService.getLatestScoresSummaryMap())
                .thenReturn(Map.of(
                        "plugin-1", scoreP1,
                        "plugin-2", scoreP2));
        when(scoreService.getScoresStatistics())
                .thenReturn(new ScoreService.ScoreStatistics(87.5, 50, 100, 100, 100, 100));

        // @formatter:off
        mockMvc.perform(get("/api/scores"))
            .andExpectAll(
                status().isOk(),
                header().string("ETag", equalTo("\"" + scoreP1Date.toEpochSecond() + "\"")),
                content().contentType(MediaType.APPLICATION_JSON),
                content().json("""
                    {
                        'plugins': {
                            'plugin-1': {
                                'value': 100,
                                'date': "%s",
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
                                'date': "%s",
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
                    """.formatted(
                        scoreP1Date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        scoreP2Date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    ))
            );
        // @formatter:on
    }

    @Test
    void shouldGet304WhenNoNewScoring() throws Exception {
        final Plugin plugin = mock(Plugin.class);

        final ZonedDateTime computedAt = ZonedDateTime.now().minusHours(2);
        final Score scoreP1 = new Score(plugin, computedAt);
        scoreP1.addDetail(new ScoreResult(
                "scoring-1",
                100,
                1,
                Set.of(new ScoringComponentResult(
                        100, 1, List.of("There is no active security advisory for the plugin."))),
                1));

        when(scoreService.getLatestScoresSummaryMap()).thenReturn(Map.of("plugin-1", scoreP1));

        MvcResult mvcResult = mockMvc.perform(get("/api/scores"))
                .andExpectAll(
                        status().isOk(),
                        header().string("ETag", equalTo("\"%s\"".formatted(computedAt.toEpochSecond()))),
                        content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        final HttpHeaders httpHeaders = new HttpHeaders();
        String responseETag = mvcResult.getResponse().getHeader("ETag");
        assertThat(responseETag).isNotNull();
        httpHeaders.setIfNoneMatch(responseETag);

        mvcResult = mockMvc.perform(get("/api/scores").headers(httpHeaders))
                .andExpectAll(
                        status().isNotModified(),
                        header().string("ETag", equalTo("\"%s\"".formatted(computedAt.toEpochSecond()))))
                .andReturn();

        responseETag = mvcResult.getResponse().getHeader("ETag");
        assertThat(responseETag).isNotNull();
        httpHeaders.setIfNoneMatch(responseETag);

        final ZonedDateTime newScoreComputedAt = ZonedDateTime.now().minusMinutes(5);
        final Score newScoreP1 = new Score(plugin, newScoreComputedAt);
        scoreP1.addDetail(new ScoreResult(
                "scoring-1",
                100,
                1,
                Set.of(new ScoringComponentResult(
                        100, 1, List.of("There is no active security advisory for the plugin."))),
                1));

        when(scoreService.getLatestScoresSummaryMap()).thenReturn(Map.of("plugin-1", newScoreP1));

        mockMvc.perform(get("/api/scores").headers(httpHeaders))
                .andExpectAll(
                        status().isOk(),
                        header().string("ETag", equalTo("\"%s\"".formatted(newScoreComputedAt.toEpochSecond()))),
                        content().contentType(MediaType.APPLICATION_JSON));
    }
}
