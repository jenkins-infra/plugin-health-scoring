package io.jenkins.pluginhealth.scoring.http;

import java.util.HashMap;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.ResultStatus;
import io.jenkins.pluginhealth.scoring.service.PluginService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(path = "/results")
public class ProbeResultController {

    private final PluginService pluginService;

    public ProbeResultController(PluginService pluginService) {
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
        Map<String, Integer> probeResultsMap = new HashMap<>();

        pluginService.streamAll().forEach(
            plugin -> {
                plugin.getDetails().values().forEach(probeResult -> {
                    if (probeResult.status() == ResultStatus.SUCCESS) {
                        probeResultsMap.put(probeResult.id(), probeResultsMap
                            .containsKey(probeResult.id()) ? probeResultsMap
                            .get(probeResult.id()) + 1 : 1);
                    }
                });
            });

        return probeResultsMap;
    }
}
