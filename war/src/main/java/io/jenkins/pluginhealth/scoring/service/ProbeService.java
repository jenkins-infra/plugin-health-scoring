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

package io.jenkins.pluginhealth.scoring.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;
import io.jenkins.pluginhealth.scoring.probes.DependabotPullRequestProbe;
import io.jenkins.pluginhealth.scoring.probes.DeprecatedPluginProbe;
import io.jenkins.pluginhealth.scoring.probes.InstallationStatProbe;
import io.jenkins.pluginhealth.scoring.probes.JenkinsCoreProbe;
import io.jenkins.pluginhealth.scoring.probes.KnownSecurityVulnerabilityProbe;
import io.jenkins.pluginhealth.scoring.probes.LastCommitDateProbe;
import io.jenkins.pluginhealth.scoring.probes.Probe;
import io.jenkins.pluginhealth.scoring.probes.ProbeContext;
import io.jenkins.pluginhealth.scoring.probes.PullRequestProbe;
import io.jenkins.pluginhealth.scoring.probes.UpForAdoptionProbe;
import io.jenkins.pluginhealth.scoring.repository.PluginRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public Optional<Probe> getProbeByKey(String key) {
        return probes.stream().filter(p -> p.key().equals(key)).findFirst();
    }

    private static final List<String> IGNORE_RAW_RESULT_PROBES = List.of(
        DependabotPullRequestProbe.KEY,
        InstallationStatProbe.KEY,
        JenkinsCoreProbe.KEY,
        LastCommitDateProbe.KEY,
        PullRequestProbe.KEY
    );

    @Transactional(readOnly = true)
    public Map<String, Long> getProbesFinalResults() {
        return probes.stream()
            .filter(probe -> !IGNORE_RAW_RESULT_PROBES.contains(probe.key()))
            .collect(Collectors.toMap(Probe::key, probe -> getProbesRawResultsFromDatabase(probe.key())));
    }

    private long getProbesRawResultsFromDatabase(String probeID) {
        return switch (probeID) {
            case UpForAdoptionProbe.KEY, KnownSecurityVulnerabilityProbe.KEY, DeprecatedPluginProbe.KEY ->
                pluginRepository.getProbeRawResult(probeID, "FAILURE");
            default -> pluginRepository.getProbeRawResult(probeID, "SUCCESS");
        };
    }

    public ProbeContext getProbeContext(String pluginName, UpdateCenter updateCenter) throws IOException {
        return new ProbeContext(pluginName, updateCenter);
    }

    public Map<String, ProbeView> getProbesView() {
        return getProbes().stream()
            .map(probe -> new ProbeView(probe.key(), probe.getDescription(), probe.getProbeResultRequirement()))
            .collect(Collectors.toMap(ProbeView::key, p -> p));
    }


    public record ProbeView(String key, String description, String[] requirements) {
    }
}
