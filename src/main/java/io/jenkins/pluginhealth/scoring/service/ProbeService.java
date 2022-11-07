package io.jenkins.pluginhealth.scoring.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.transaction.Transactional;

import io.jenkins.pluginhealth.scoring.probes.Probe;
import io.jenkins.pluginhealth.scoring.repository.PluginRepository;

import org.springframework.stereotype.Service;

@Service
public class ProbeService {
    private final List<Probe> probes;
    private final PluginService pluginService;
    private final PluginRepository pluginRepository;

    public ProbeService(List<Probe> probes, PluginService pluginService, PluginRepository pluginRepository) {
        this.probes = List.copyOf(probes);
        this.pluginService = pluginService;
        this.pluginRepository = pluginRepository;
    }

    @Transactional
    public Map<String, Long> getProbesFinalResults() {
        Map<String, Long> probeResultsMap = probes.stream()
            .collect(Collectors.toMap(Probe::key, probe -> getProbesRawResultsFromDatabase(probe.key())));

        probeResultsMap.remove("last-commit-date");

        return probeResultsMap;
    }

    @Transactional
    public long getProbesRawResultsFromDatabase(String probeID) {
        return switch (probeID) {
            case "up-for-adoption", "security", "deprecation" ->
                pluginRepository.getProbeRawResult(probeID, "FAILURE");
            default ->
                pluginRepository.getProbeRawResult(probeID, "SUCCESS");
        };
    }
}
