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
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.Score;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
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
@ImportAutoConfiguration({ProjectInfoAutoConfiguration.class, SecurityConfiguration.class})
@WebMvcTest(
    controllers = ScoreAPI.class
)
class ScoreAPITest {
    @MockBean private ScoreService scoreService;
    @Autowired private MockMvc mockMvc;
    @Autowired ObjectMapper mapper;

    @Test
    void shouldBeAbleToProvideScoresSummary() throws Exception {
        final String probe1Key = "probe-1",
            probe2Key = "probe-2",
            probe3Key = "probe-3",
            scoring1Key = "scoring-1",
            scoring2Key = "scoring-2";

        final Plugin plugin1 = mock(Plugin.class);
        final ProbeResult p1Probe1Result = ProbeResult.success(probe1Key, "The plugin has a Jenkinsfile");
        when(plugin1.getDetails()).thenReturn(Map.of(
            probe1Key, p1Probe1Result
        ));

        final Plugin plugin2 = mock(Plugin.class);
        final ProbeResult p2Probe1Result = ProbeResult.success(probe1Key, "The plugin has a Jenkinsfile");
        final ProbeResult p2Probe2Result = ProbeResult.success(probe2Key, "The plugin does not use dependabot");
        final ProbeResult p2Probe3Result = ProbeResult.success(probe3Key, "The plugin does not use renovate");
        when(plugin2.getDetails()).thenReturn(Map.of(
            probe1Key, p2Probe1Result,
            probe2Key, p2Probe2Result,
            probe3Key, p2Probe3Result
        ));

        // TODO
        final ScoreResult p1sr1 = new ScoreResult(scoring1Key, 1, 1, Set.of());
        final ScoreResult p2sr1 = new ScoreResult(scoring1Key, 1, 1, Set.of());
        final ScoreResult p2sr2 = new ScoreResult(scoring2Key, 0, 1, Set.of());

        final Score score1 = new Score(plugin1, ZonedDateTime.now());
        score1.addDetail(p1sr1);
        final Score score2 = new Score(plugin2, ZonedDateTime.now());
        score2.addDetail(p2sr1);
        score2.addDetail(p2sr2);

        final Map<String, Score> summary = Map.of(
            "plugin-1", score1,
            "plugin-2", score2
        );
        when(scoreService.getLatestScoresSummaryMap()).thenReturn(summary);

        when(scoreService.getScoresStatistics()).thenReturn(new ScoreService.ScoreStatistics(
            50, 0, 100, 100, 100, 100
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
                                        'value': 1,
                                        'weight': 1,
                                        'components': {
                                            'probe-1': { 'passed': true, 'description': 1 }
                                        }
                                    }
                                }
                            },
                            'plugin-2': {
                                'value': 50,
                                'details': {
                                    'scoring-1': {
                                        'value': 1,
                                        'weight': 1,
                                        'components': {
                                            'probe-1': { 'passed': true, 'description': 1 }
                                        }
                                    },
                                    'scoring-2': {
                                        'value': 0,
                                        'weight': 1,
                                        'components': {
                                            'probe-2': { 'passed': false, 'description':  0 },
                                            'probe-3': { 'passed': false, 'description':  0 }
                                        }
                                    }
                                }
                            }
                        },
                        'statistics': {
                            'average': 50,
                            'minimum': 0,
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
