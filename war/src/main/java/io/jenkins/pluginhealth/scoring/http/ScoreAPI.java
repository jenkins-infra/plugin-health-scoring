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

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
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
        record Tuple(String name, PluginScoreSummary summary) {
        }

        final Map<String, PluginScoreSummary> plugins = scoreService.getLatestScoresSummaryMap()
            .entrySet().stream()
            .map(entry -> {
                final var score = entry.getValue();
                return new Tuple(
                    entry.getKey(),
                    new PluginScoreSummary(
                        score.getValue(),
                        score.getDetails().stream()
                            .collect(Collectors.toMap(
                                ScoreResult::key,
                                scoreResult -> new PluginScoreDetail(
                                    scoreResult.value(),
                                    scoreResult.coefficient(),
                                    getScoringComponents(scoreResult, score.getPlugin().getDetails())
                                )
                            ))
                    )
                );
            })
            .collect(Collectors.toMap(Tuple::name, Tuple::summary));
        return new ScoreReport(plugins, stats);
    }

    private Map<String, PluginScoreDetailComponent> getScoringComponents(ScoreResult result,
                                                                         Map<String, ProbeResult> probeResults) {
        record Tuple(String name, Float value) {
        }

        return scoringService.get(result.key())
            .map(scoring -> {
                final Map<String, Float> scoreComponents = scoring.getScoreComponents();
                return scoreComponents.entrySet().stream().map(e -> new Tuple(e.getKey(), e.getValue()));
            })
            .orElse(Stream.empty())
            .collect(Collectors.toMap(
                Tuple::name,
                tuple -> new PluginScoreDetailComponent(
                    probeResults.containsKey(tuple.name()) && probeResults.get(tuple.name()).status().equals(ProbeResult.Status.SUCCESS) ? tuple.value() : 0,
                    tuple.value()
                )
            ));
    }

    private record ScoreReport(Map<String, PluginScoreSummary> plugins, ScoreService.ScoreStatistics statistics) {
    }

    private record PluginScoreSummary(long value, Map<String, PluginScoreDetail> details) {
    }

    private record PluginScoreDetail(float value, float weight, Map<String, PluginScoreDetailComponent> components) {
    }

    private record PluginScoreDetailComponent(float value, float max) {
    }
}
