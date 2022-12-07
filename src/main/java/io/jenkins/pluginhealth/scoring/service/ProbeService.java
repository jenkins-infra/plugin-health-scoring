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

package io.jenkins.pluginhealth.scoring.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;
import io.jenkins.pluginhealth.scoring.probes.Probe;
import io.jenkins.pluginhealth.scoring.probes.ProbeContext;
import io.jenkins.pluginhealth.scoring.repository.PluginRepository;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ProbeService {
    private final List<Probe> probes;
    private final PluginRepository pluginRepository;

    public ProbeService(List<Probe> probes, PluginRepository pluginRepository) {
        this.probes = List.copyOf(probes);
        this.pluginRepository = pluginRepository;
    }

    public List<Probe> getProbes() {
        return probes;
    }

    @Transactional
    public Map<String, Long> getProbesFinalResults() {
        Map<String, Long> probeResultsMap = probes.stream()
            .collect(Collectors.toMap(Probe::key, probe -> getProbesRawResultsFromDatabase(probe.key())));

        probeResultsMap.remove("last-commit-date");

        return probeResultsMap;
    }

    private long getProbesRawResultsFromDatabase(String probeID) {
        return switch (probeID) {
            case "up-for-adoption", "security", "deprecation" ->
                pluginRepository.getProbeRawResult(probeID, "FAILURE");
            default ->
                pluginRepository.getProbeRawResult(probeID, "SUCCESS");
        };
    }

    public ProbeContext getProbeContext(String pluginName, UpdateCenter updateCenter) throws IOException {
        return new ProbeContext(pluginName, updateCenter);
    }
}
