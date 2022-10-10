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

package io.jenkins.pluginhealth.scoring.probes;

import java.io.IOException;
import java.util.List;

import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;
import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;
import io.jenkins.pluginhealth.scoring.service.PluginService;
import io.jenkins.pluginhealth.scoring.service.UpdateCenterService;

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
public class ProbeEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProbeEngine.class);

    private final List<Probe> probes;
    private final PluginService pluginService;
    private final UpdateCenterService updateCenterService;

    public ProbeEngine(List<Probe> probes, PluginService pluginService, UpdateCenterService updateCenterService) {
        this.probes = List.copyOf(probes);
        this.pluginService = pluginService;
        this.updateCenterService = updateCenterService;
    }

    /**
     * Starts to apply all the {@link Probe} implementations on all the plugins registered in the database.
     */
    public void run() throws IOException {
        LOGGER.info("Start running probes on all plugins");
        final UpdateCenter updateCenter = updateCenterService.fetchUpdateCenter();
        pluginService.streamAll().parallel()
            .peek(plugin -> {
                try {
                    final ProbeContext probeContext = new ProbeContext(plugin.getName(), updateCenter);
                    probes.forEach(probe -> {
                        try {
                            final ProbeResult previousResult = plugin.getDetails().get(probe.key());
                            if (previousResult == null || !probe.requiresRelease() ||
                                (probe.requiresRelease()
                                    && previousResult.timestamp() != null
                                    && previousResult.timestamp().isBefore(plugin.getReleaseTimestamp()))
                            ) {
                                if (LOGGER.isTraceEnabled()) {
                                    LOGGER.trace("Running {} on {}", probe.key(), plugin.getName());
                                }
                                final ProbeResult result = probe.apply(plugin, probeContext);
                                if (result.status() != ResultStatus.ERROR) {
                                    plugin.addDetails(result);
                                }
                            } else {
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug("{} requires a release of {} to process it again.", probe.key(), plugin.getName());
                                }
                            }
                        } catch (Throwable t) {
                            LOGGER.error("Couldn't run {} on {}", probe.key(), plugin.getName(), t);
                        }
                    });
                    probeContext.cleanUp();
                } catch (IOException ex) {
                    // TODO
                }
            })
            .forEach(pluginService::saveOrUpdate);
        LOGGER.info("Probe engine has finished");
    }
}
