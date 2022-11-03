package io.jenkins.pluginhealth.scoring.http;

import java.util.Map;
import java.util.stream.Collectors;

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
        Map<String, Integer> probeResultsMap;

        probeResultsMap = pluginService.streamAll()
            .map(plugin ->
                plugin.getDetails().values().stream().filter(probeResult -> probeResult.status() == ResultStatus.SUCCESS)
                .collect(Collectors.toMap(probeResult -> probeResult.id(), probeResult -> 1)))
            .flatMap(m -> m.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (currentProbeData, latestProbeData) -> currentProbeData + 1));

        probeResultsMap.put("size", (int) pluginService.streamAll().count());

        return probeResultsMap;
    }
}
