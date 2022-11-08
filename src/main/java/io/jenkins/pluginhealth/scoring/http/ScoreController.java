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

import java.util.Map;
import java.util.stream.Collectors;

import io.jenkins.pluginhealth.scoring.scores.Scoring;
import io.jenkins.pluginhealth.scoring.service.ScoreService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(path = "/scores")
public class ScoreController {
    private final ScoreService scoreService;
    private final Map<String, ScoringView> scoringsMap;

    public ScoreController(ScoreService scoreService, List<Scoring> scorings) {
        this.scoreService = scoreService;
        this.scoringsMap = scorings.stream().map(ScoringView::fromScoring).collect(Collectors.toUnmodifiableMap(ScoringView::key, scoring -> scoring));
    }

    @GetMapping(path = "")
    public ModelAndView list() {
        return new ModelAndView("scores/listing", Map.of("scorings", scoringsMap.values()));
    }

    @GetMapping(path = "/{pluginName}")
    public ModelAndView forPlugin(@PathVariable String pluginName) {
        return scoreService.latestScoreFor(pluginName)
            .map(score -> new ModelAndView("scores/details", Map.of("score", score)))
            .orElseGet(() -> new ModelAndView("scores/unknown", Map.of("pluginName", pluginName)));
    }

    record ScoringView(String key, String name, float coefficient, String description) {
        public static ScoringView fromScoring(Scoring scoring) {
            return new ScoringView(scoring.key(), scoring.name(), scoring.coefficient(), scoring.description());
        }
    }
}
