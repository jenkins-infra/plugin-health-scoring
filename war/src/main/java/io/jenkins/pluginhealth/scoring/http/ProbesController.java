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

import java.io.IOException;
import java.util.Comparator;

import io.jenkins.pluginhealth.scoring.probes.Probe;
import io.jenkins.pluginhealth.scoring.probes.ProbeEngine;
import io.jenkins.pluginhealth.scoring.scores.ScoringEngine;
import io.jenkins.pluginhealth.scoring.service.PluginService;
import io.jenkins.pluginhealth.scoring.service.ProbeService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(path = "/probes")
public class ProbesController {
    private final PluginService pluginService;
    private final ProbeService probeService;
    private final ProbeEngine probeEngine;
    private final ScoringEngine scoringEngine;

    public ProbesController(PluginService pluginService, ProbeService probeService, ProbeEngine probeEngine,
                            ScoringEngine scoringEngine) {
        this.pluginService = pluginService;
        this.probeService = probeService;
        this.probeEngine = probeEngine;
        this.scoringEngine = scoringEngine;
    }

    @ModelAttribute(name = "module")
    /* default */ String module() {
        return "probes";
    }

    @GetMapping(path = "")
    public ModelAndView list() {
        final ModelAndView modelAndView = new ModelAndView("probes/listing");

        modelAndView.addObject(
            "probes",
            probeService.getProbes().stream()
                .map(ProbeDetails::map)
                .sorted(Comparator.comparing(ProbeDetails::id))
                .toList()
        );
        return modelAndView;
    }

    @GetMapping(path = "/results")
    public ModelAndView listProbeResults() {
        final ModelAndView modelAndView = new ModelAndView("probes/results");

        modelAndView
            .addObject("probeResults", probeService.getProbesFinalResults())
            .addObject("pluginsCount", pluginService.getPluginsCount());

        return modelAndView;
    }

    // TODO needs to be secured
    @GetMapping(value = "/{pluginName}")
    public ModelAndView executeProbesOnPlugin(@PathVariable String pluginName) {
        return pluginService.findByName(pluginName)
            .map(plugin -> {
                try {
                    probeEngine.runOn(plugin);
                    scoringEngine.runOn(plugin);
                    return new ModelAndView("redirect:/scores/" + pluginName)
                        .addObject("updated", true);
                } catch (IOException e) {
                    return new ModelAndView("redirect:/scores/" + pluginName)
                        .addObject("error", e);
                }
            })
            .orElseGet(() -> new ModelAndView("redirect:/scores/unknown"));
    }

    record ProbeDetails(String id, String description, String[] requirements) {
        static ProbeDetails map(Probe probe) {
            return new ProbeDetails(
                probe.key(),
                probe.getDescription(),
                probe.getProbeResultRequirement()
            );
        }
    }
}
