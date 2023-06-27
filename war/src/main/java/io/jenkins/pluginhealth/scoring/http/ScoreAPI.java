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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;
import io.jenkins.pluginhealth.scoring.model.Score;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
import io.jenkins.pluginhealth.scoring.scores.Scoring;
import io.jenkins.pluginhealth.scoring.service.ScoreService;
import io.jenkins.pluginhealth.scoring.service.ScoringService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scores")
public class ScoreAPI {
    private final ScoreService scoreService;
    private final ScoringService scoringService;

    public ScoreAPI(ScoreService scoreService, ScoringService scoringService) {
        this.scoreService = scoreService;
        this.scoringService = scoringService;
    }

    @GetMapping(value = { "", "/" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ScoreReport getReport() {
        final ScoreService.ScoreStatistics stats = scoreService.getScoresStatistics();
        record Tuple(String name, PluginScore summary) {
        }

        final Map<String, PluginScore> plugins = scoreService.getLatestScoresSummaryMap()
            .entrySet().stream()
            .map(pluginNameScoreEntry -> {
                final Score score = pluginNameScoreEntry.getValue();
                final String pluginName = pluginNameScoreEntry.getKey();

                return new Tuple(
                    pluginName,
                    new PluginScore(
                        score.getValue(),
                        score.getDetails().stream()
                            .map(scoreResult -> new PluginScoreDetail(
                                scoreResult.key(),
                                scoreResult.value(),
                                scoreResult.coefficient(),
                                getScoringComponents(scoreResult, score.getPlugin().getDetails()),
                                scoringService.get(scoreResult.key()).map(Scoring::description).orElse("")
                            ))
                            .collect(Collectors.toList())
                    )
                );
            })
            .collect(Collectors.toMap(Tuple::name, Tuple::summary));
        return new ScoreReport(plugins, stats);
    }

    private List<PluginScoreDetailComponent> getScoringComponents(ScoreResult scoreResult, Map<String, ProbeResult> pluginProbeResults) {
        return scoringService.get(scoreResult.key())
            .map(Scoring::getScoreComponents)
            .orElse(Map.of())
            .entrySet().stream()
            .map(component -> {
                final ProbeResult result = pluginProbeResults.get(component.getKey());
                var val = result == null || result.status().equals(ResultStatus.FAILURE) ?
                    0 : component.getValue();// TODO this is not ideal, we shouldn't have to recompute those.
                return new PluginScoreDetailComponent(
                    component.getKey(),
                    val,
                    component.getValue()
                );
            })
            .toList();
    }

    private record ScoreReport(Map<String, PluginScore> plugins, ScoreService.ScoreStatistics statistics) {
    }

    private record PluginScore(long value, List<PluginScoreDetail> details) {
    }

    private record PluginScoreDetail(String name, float value, float weight, List<PluginScoreDetailComponent> components, String description) {
    }

    private record PluginScoreDetailComponent(String name, float value, float max) {
    }
}
