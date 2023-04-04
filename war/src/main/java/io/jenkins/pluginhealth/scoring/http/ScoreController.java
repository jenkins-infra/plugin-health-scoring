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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.service.ProbeService;
import io.jenkins.pluginhealth.scoring.service.ScoreService;
import io.jenkins.pluginhealth.scoring.service.ScoringService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(path = "/scores")
public class ScoreController {
    private final ProbeService probeService;
    private final ScoreService scoreService;
    private final ScoringService scoringService;

    public ScoreController(ProbeService probeService, ScoreService scoreService, ScoringService scoringService) {
        this.probeService = probeService;
        this.scoreService = scoreService;
        this.scoringService = scoringService;
    }

    @ModelAttribute(name = "module")
    /* default */ String module() {
        return "scores";
    }

    @GetMapping(path = "")
    public ModelAndView list() {
        return new ModelAndView("scores/listing", Map.of("scorings", scoringService.getScoringList()));
    }

    @GetMapping(path = "/{pluginName}")
    public ModelAndView getScoreOf(@PathVariable String pluginName) {
        return scoreService.latestScoreFor(pluginName)
            .map(score -> {
                final Plugin plugin = score.getPlugin();
                final Map<String, ProbeResultView> probeResultViews = plugin.getDetails().values().stream()
                    .map(result -> new ProbeResultView(
                        result.id(), result.status().name(), result.message()
                    ))
                    .collect(Collectors.toMap(ProbeResultView::key, value -> value));
                final List<ScoreResultView> scoreComponents = score.getDetails().stream()
                    .map(result -> new ScoreResultView(result.key(), result.value(), result.coefficient()))
                    .toList();
                final Map<String, ScoringService.ScoreView> scores = scoringService.getScoringsView();
                final Map<String, ProbeService.ProbeView> probes = probeService.getProbesView();

                final PluginScoreView view = new PluginScoreView(
                    plugin.getName(),
                    score.getValue(),
                    probeResultViews,
                    scoreComponents
                );
                return new ModelAndView("scores/details", Map.of(
                    "score", view,
                    "scores", scores,
                    "probes", probes
                ));
            })
            .orElseGet(() -> new ModelAndView("scores/unknown", Map.of("pluginName", pluginName), HttpStatus.NOT_FOUND));
    }

    private record PluginScoreView(String pluginName,
                                   long value,
                                   Map<String, ProbeResultView> probeResults,
                                   Collection<ScoreResultView> details) {
    }

    private record ProbeResultView(String key, String status, String message) {
    }

    private record ScoreResultView(String key, float value, float coefficient) {
    }
}
