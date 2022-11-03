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
        this.probes = List.of(probes);
        this.pluginService = pluginService;
    }

    @GetMapping(path = "")
    public ModelAndView list() {
        final ModelAndView modelAndView = new ModelAndView("probes/results");

        modelAndView.addObject(
            "probeResults",
            getProbeResultsData()
        );

        return modelAndView;
    }

    public Map<String, Integer> getProbeResultsData() {
        return probes.stream()
            .collect(Collectors.toMap(probe -> probe.key(), probe -> pluginService.getProbeRawData(probe.key())));
    }
}
