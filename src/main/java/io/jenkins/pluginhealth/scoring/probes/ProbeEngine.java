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

import java.util.List;
import java.util.stream.Collectors;

import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;
import io.jenkins.pluginhealth.scoring.service.PluginService;

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

    public ProbeEngine(List<Probe> probes, PluginService pluginService) {
        this.probes = List.copyOf(probes);
        this.pluginService = pluginService;
    }

    /**
     * Starts to apply all the {@link Probe} implementations on all the plugins registered in the database.
     */
    public void run() {
        LOGGER.info("Start running probes on all plugins");
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Registered probes: {}", probes.stream().map(Probe::key).collect(Collectors.joining(", ")));
        }
        pluginService.streamAll().parallel()
            .peek(plugin -> probes.stream()
                .map(probe -> {
                    try {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Running {} on {}", probe.key(), plugin.getName());
                        }
                        return probe.apply(plugin);
                    } catch (Throwable t) {
                        LOGGER.error("Couldn't run {} on {}", probe.key(), plugin.getName(), t);
                        return ProbeResult.error(probe.key(), "Could not run probe " + probe.key());
                    }
                })
                .filter(result -> result.status() != ResultStatus.ERROR)
                .forEach(plugin::addDetails))
            .forEach(pluginService::saveOrUpdate);
        LOGGER.info("Probe engine has finished");
    }
}
