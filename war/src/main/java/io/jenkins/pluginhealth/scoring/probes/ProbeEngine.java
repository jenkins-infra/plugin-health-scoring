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

package io.jenkins.pluginhealth.scoring.probes;

import java.io.IOException;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;
import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;
import io.jenkins.pluginhealth.scoring.service.PluginDocumentationService;
import io.jenkins.pluginhealth.scoring.service.PluginService;
import io.jenkins.pluginhealth.scoring.service.ProbeService;
import io.jenkins.pluginhealth.scoring.service.UpdateCenterService;

import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Represents the entrypoint to run probes on plugins.
 * <p>
 * The engine gets its probes through injection.
 * The {@link Probe} implementations must be flagged as {@link Component}.
 * Each implementation can choose to placed before or after another implementation using {@link org.springframework.core.annotation.Order} flag.
 */
@Component
public final class ProbeEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProbeEngine.class);

    private final ProbeService probeService;
    private final PluginService pluginService;
    private final UpdateCenterService updateCenterService;
    private final GitHub gitHub;
    private final PluginDocumentationService pluginDocumentationService;

    public ProbeEngine(ProbeService probeService, PluginService pluginService, UpdateCenterService updateCenterService, GitHub gitHub, PluginDocumentationService pluginDocumentationService) {
        this.probeService = probeService;
        this.pluginService = pluginService;
        this.updateCenterService = updateCenterService;
        this.gitHub = gitHub;
        this.pluginDocumentationService = pluginDocumentationService;
    }

    /**
     * Starts to apply all the {@link Probe} implementations on all the plugins registered in the database.
     */
    public void run() throws IOException {
        LOGGER.info("Start running probes on all plugins");
        final UpdateCenter updateCenter = updateCenterService.fetchUpdateCenter();
        pluginService.streamAll().parallel()
            .forEach(plugin -> this.runOn(plugin, updateCenter));
        LOGGER.info("Probe engine has finished");
    }

    /**
     * Runs all the probes on a specific plugin
     *
     * @param plugin the selected plugin to run all probes on
     * @throws IOException thrown when the update-center cannot be retrieved
     */
    public void runOn(Plugin plugin) throws IOException {
        LOGGER.info("Start running probes on {}", plugin.getName());
        final UpdateCenter updateCenter = updateCenterService.fetchUpdateCenter();
        runOn(plugin, updateCenter);
        LOGGER.info("Probe engine has finished");
    }

    private void runOn(Plugin plugin, UpdateCenter updateCenter) {
        final ProbeContext probeContext;
        try {
            probeContext = probeService.getProbeContext(plugin.getName(), updateCenter);
        } catch (IOException ex) {
            LOGGER.error("Cannot create temporary plugin for {}", plugin.getName(), ex);
            return;
        }

        probeContext.setGitHub(gitHub);

        probeService.getProbes().forEach(probe -> {
            try {
                final ProbeResult result = probe.apply(plugin, probeContext);
                if (result.status() != ResultStatus.ERROR) {
                    plugin.addDetails(result);
                }
            } catch (Throwable t) {
                LOGGER.error("Couldn't run {} on {}", probe.key(), plugin.getName(), t);
            }
        });

        try {
            pluginService.saveOrUpdate(plugin);
        } catch (Throwable e) {
            LOGGER.error("Could not save result of probe engine for plugin {}", plugin.getName(), e);
        }

        try {
            probeContext.cleanUp();
        } catch (IOException ex) {
            LOGGER.error("Failed to cleanup {} for {}", probeContext.getScmRepository(), plugin.getName(), ex);
        }
    }
}
