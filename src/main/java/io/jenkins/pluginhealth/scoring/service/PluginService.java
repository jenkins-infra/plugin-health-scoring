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

import java.util.stream.Stream;
import javax.transaction.Transactional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.repository.PluginRepository;

import org.springframework.stereotype.Service;

@Service
public class PluginService {
    private final PluginRepository pluginRepository;

    public PluginService(PluginRepository pluginRepository) {
        this.pluginRepository = pluginRepository;
    }

    @Transactional
    public Plugin saveOrUpdate(Plugin plugin) {
        return pluginRepository.findByName(plugin.getName())
            .map(pluginFromDatabase -> pluginFromDatabase
                .setScm(plugin.getScm())
                .setReleaseTimestamp(plugin.getReleaseTimestamp())
                .setVersion(plugin.getVersion())
                .addDetails(plugin.getDetails()))
            .map(pluginRepository::save)
            .orElseGet(() -> pluginRepository.save(plugin));
    }

    @Transactional
    public Stream<Plugin> streamAll() {
        return pluginRepository.findAll().stream();
    }

    @Transactional
    public int getPluginsCount() {
        return pluginRepository.findAll().size();
    }

    @Transactional
    public int getProbeRawData(String probeID) {
        switch (probeID) {
            case "scm":
                return pluginRepository.getProbeRawData("scm", "SUCCESS");
            case "last-commit-date":
                return pluginRepository.getProbeRawData("last-commit-date", "SUCCESS");
            case "up-for-adoption":
                return pluginRepository.getProbeRawData("up-for-adoption", "FAILURE");
            case "security":
                return pluginRepository.getProbeRawData("security", "FAILURE");
            case "deprecation":
                return pluginRepository.getProbeRawData("deprecation", "FAILURE");
            case "dependabot":
                return pluginRepository.getProbeRawData("dependabot", "SUCCESS");
            case "jep-229":
                return pluginRepository.getProbeRawData("jep-229", "SUCCESS");
            case "jenkinsfile":
                return pluginRepository.getProbeRawData("jenkinsfile", "SUCCESS");
            default:
                return 0;
        }
    }
}
