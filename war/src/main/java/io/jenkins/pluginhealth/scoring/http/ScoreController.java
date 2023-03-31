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
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.scores.Scoring;
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
    private final ScoreService scoreService;
    private final ScoringService scoringService;

    public ScoreController(ScoreService scoreService, ScoringService scoringService) {
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
                final List<ScoreView> details = score.getDetails().stream()
                    .map(results -> new ScoreView(results.key(), results.value(), results.coefficient()))
                    .map(view -> {
                        final Optional<Scoring> scoring = scoringService.get(view.getKey());
                        return scoring
                            .map(s -> view
                                .withComponents(s.getScoreComponents())
                                .withDescription(s.description())
                            )
                            .orElse(view);
                    })
                    .toList();
                return new ModelAndView("scores/details", Map.of("score", score, "details", details));
            })
            .orElseGet(() -> new ModelAndView("scores/unknown", Map.of("pluginName", pluginName), HttpStatus.NOT_FOUND));
    }

    private static class ScoreView {
        private final String key;
        private final float value, coefficient;
        private String description;
        private Map<String, Float> components;

        public ScoreView(String key, float value, float coefficient) {
            this.key = key;
            this.value = value;
            this.coefficient = coefficient;
        }

        public String getKey() {
            return key;
        }

        public float getValue() {
            return value;
        }

        public float getCoefficient() {
            return coefficient;
        }

        public String getDescription() {
            return description;
        }

        public ScoreView withDescription(String description) {
            this.description = description;
            return this;
        }

        public Map<String, Float> getComponents() {
            return components;
        }

        public ScoreView withComponents(Map<String, Float> components) {
            this.components = components;
            return this;
        }
    }
}
