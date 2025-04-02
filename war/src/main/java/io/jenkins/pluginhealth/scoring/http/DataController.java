/*
 * MIT License
 *
 * Copyright (c) 2025 Jenkins Infra
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

import io.jenkins.pluginhealth.scoring.service.ScoreService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(path = "/data")
public class DataController {

    private final ScoreService scoreService;

    public DataController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @ModelAttribute(name = "module")
    /* default */ String module() {
        return "data";
    }

    @GetMapping(path = {"", "/"})
    public ModelAndView index() {
        final ModelAndView modelAndView = new ModelAndView("data/distribution");
        modelAndView.addObject("distribution", scoreService.getScoresDistribution());
        modelAndView.addObject("statistics", scoreService.getScoresStatistics());
        return modelAndView;
    }

    @GetMapping(path = "/pluginsPerScore/{score}")
    public ModelAndView pluginsPerScore(@PathVariable int score) {
        final ModelAndView modelAndView = new ModelAndView("data/pluginsPerScore");
        modelAndView.addObject("scores", scoreService.getAllPluginsWithScore(score));
        return modelAndView;
    }
}
