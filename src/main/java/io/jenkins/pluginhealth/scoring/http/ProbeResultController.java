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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.jenkins.pluginhealth.scoring.probes.Probe;
import io.jenkins.pluginhealth.scoring.service.PluginService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(path = "/results")
public class ProbeResultController {

    private final List<Probe> probes;
    private final PluginService pluginService;

    public ProbeResultController(List<Probe> probes, PluginService pluginService) {
        this.probes = List.copyOf(probes);
        this.pluginService = pluginService;
    }

    @GetMapping(path = "")
    public ModelAndView list() {
        final ModelAndView modelAndView = new ModelAndView("probes/results");

        modelAndView.addObject(
            "probeResults",
            getProbeResultsData()
        ).addObject(
            "pluginsCount",
            pluginService.getPluginsCount()
        );

        return modelAndView;
    }

    public Map<String, Integer> getProbeResultsData() {
        return probes.stream()
            .collect(Collectors.toMap(Probe::key, probe -> pluginService.getProbeRawData(probe.key())));
    }
}
