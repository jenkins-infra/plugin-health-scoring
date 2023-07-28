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

import java.time.ZonedDateTime;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the analyze which can be performed on a plugin
 */
public abstract class Probe {
    private static final Logger LOGGER = LoggerFactory.getLogger(Probe.class);

    /**
     * Starts the analysis on a plugin.
     * The implementation of the probe action is deferred to {@link Probe#doApply(Plugin, ProbeContext)}.
     *
     * @param plugin  the plugin on which to perform the analysis
     * @param context holder of information passed across the probes executed on a single plugin
     * @return the result of the analyze in a {@link ProbeResult}
     */
    public final ProbeResult apply(Plugin plugin, ProbeContext context) {
        if (shouldBeExecuted(plugin, context)) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Running {} on {}", this.key(), plugin.getName());
            }
            return doApply(plugin, context);
        }
        return ProbeResult.error(key(), key() + " does not meet the criteria to be executed on " + plugin.getName());
    }

    private boolean shouldBeExecuted(Plugin plugin, ProbeContext context) {
        final ProbeResult previousResult = plugin.getDetails().get(this.key());
        if (previousResult == null) {
            return true;
        }
        if (!this.requiresRelease() && !this.isSourceCodeRelated()) {
            return true;
        }
        if (this.requiresRelease() &&
            (previousResult.timestamp() != null && previousResult.timestamp().isBefore(plugin.getReleaseTimestamp()))) {
            return true;
        }
        final Optional<ZonedDateTime> optionalLastCommit = context.getLastCommitDate();
        if (this.isSourceCodeRelated() && optionalLastCommit.isEmpty()) {
            LOGGER.error(
                "{} requires last commit date for {} but was executed before the date time is registered in execution context",
                this.key(), plugin.getName()
            );
        }
        if (this.isSourceCodeRelated() &&
            optionalLastCommit
                .map(date -> previousResult.timestamp() != null && previousResult.timestamp().isBefore(date))
                .orElse(false)) {
            return true;
        }

        LOGGER.debug("{} does not meet requirement to be executed on {}", this.key(), plugin.getName());
        return false;
    }

    /**
     * Performs the analysis on a plugin.
     * Based on the provided plugin and context, the method returns a non-null {@link ProbeResult}.
     *
     * @param plugin  the plugin on which the analysis is done
     * @param context holder of information passed across the probes executed on a single plugin
     * @return a ProbeResult representing the result of the analysis
     */
    protected abstract ProbeResult doApply(Plugin plugin, ProbeContext context);

    /**
     * Returns the key identifier for the probe.
     * This is how the different probes can be identified in the {@link Plugin#getDetails()} map.
     *
     * @return the identifier of the probe
     */
    public abstract String key();

    /**
     * Returns a description of the action of the probe.
     *
     * @return the description of the probe
     */
    public abstract String getDescription();

    /**
     * Returns a boolean value that specifies if the probe result can only be modified by a release of the plugin.
     * If the probe result can be altered by a new release of the plugin, returns true, otherwise returns false.
     *
     * @return true if a release of the plugin is required to change the result of the probe execution. Otherwise, false.
     */
    protected boolean requiresRelease() {
        return false;
    }

    /**
     * Determines if the probe requires modification of the plugin source code to change the result of its previous
     * execution on the plugin.
     *
     * @return true if the probe result can only be changed from the previous execution if the source code of the
     * plugin was changed. Otherwise, false.
     */
    protected boolean isSourceCodeRelated() {
        return false;
    }
}
