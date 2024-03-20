/*
 * MIT License
 *
 * Copyright (c) 2022-2024 Jenkins Infra
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

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.jenkins.pluginhealth.scoring.model.Resolution;
import io.jenkins.pluginhealth.scoring.model.Score;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;
import io.jenkins.pluginhealth.scoring.model.ScoringComponentResult;
import io.jenkins.pluginhealth.scoring.service.ScoreService;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scores")
public class ScoreAPI {
    private final ScoreService scoreService;

    public ScoreAPI(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @GetMapping(
            value = {"", "/"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ScoreReport> getReport() {
        final Map<String, Score> latestScoresSummaryMap = scoreService.getLatestScoresSummaryMap();
        final Optional<String> optETag = latestScoresSummaryMap.values().stream()
                .max(Comparator.comparing(Score::getComputedAt))
                .map(Score::getComputedAt)
                .map(ZonedDateTime::toEpochSecond)
                .map(String::valueOf);

        record Tuple(String name, PluginScoreSummary summary) {}

        final Map<String, PluginScoreSummary> plugins = latestScoresSummaryMap.entrySet().stream()
                .map(entry -> {
                    final var score = entry.getValue();
                    return new Tuple(
                            entry.getKey(),
                            new PluginScoreSummary(
                                    score.getValue(),
                                    score.getComputedAt(),
                                    score.getDetails().stream()
                                            .collect(Collectors.toMap(ScoreResult::key, PluginScoreDetail::new))));
                })
                .collect(Collectors.toMap(Tuple::name, Tuple::summary));

        final ResponseEntity.BodyBuilder bodyBuilder =
                ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS));
        optETag.ifPresent(bodyBuilder::eTag);

        return bodyBuilder.body(new ScoreReport(plugins, scoreService.getScoresStatistics()));
    }

    public record ScoreReport(Map<String, PluginScoreSummary> plugins, ScoreService.ScoreStatistics statistics) {}

    private record PluginScoreSummary(long value, ZonedDateTime date, Map<String, PluginScoreDetail> details) {}

    private record PluginScoreDetail(float value, float weight, List<PluginScoreDetailComponent> components) {
        private PluginScoreDetail(ScoreResult result) {
            this(
                    result.value(),
                    result.weight(),
                    result.componentsResults().stream()
                            .map(PluginScoreDetailComponent::new)
                            .collect(Collectors.toList()));
        }
    }

    private record PluginScoreDetailComponent(
            int value, float weight, List<String> reasons, List<Resolution> resolutions) {
        private PluginScoreDetailComponent(ScoringComponentResult result) {
            this(result.score(), result.weight(), result.reasons(), result.resolutions());
        }
    }
}
