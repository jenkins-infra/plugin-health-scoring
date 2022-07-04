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

import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.repository.PluginRepository;

import org.springframework.stereotype.Service;

@Service
public class PluginService {
    private final PluginRepository pluginRepository;

    public PluginService(PluginRepository pluginRepository) {
        this.pluginRepository = pluginRepository;
    }

    public void saveOrUpdate(Plugin plugin) {
        Optional<Plugin> pluginFromDatabase = pluginRepository.findByName(plugin.getName());

        if (pluginFromDatabase.isPresent()) {
            if (pluginFromDatabase.get().getReleaseTimestamp() != plugin.getReleaseTimestamp()) {
                pluginFromDatabase.get().setReleaseTimestamp(plugin.getReleaseTimestamp());
                saveOrUpdate(pluginFromDatabase.get());
            }
            if (pluginFromDatabase.get().getScm() != null && !pluginFromDatabase.get().getScm().equals(plugin.getScm())) {
                pluginFromDatabase.get().setScm(plugin.getScm());
                saveOrUpdate(pluginFromDatabase.get());
            }
        }
        else {
            saveOrUpdate(plugin);
        }
    }

}
