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

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

/**
 * Represents the analyze which can be performed on a plugin
 */
public abstract class Probe {

    /**
     * Starts the analyze on a plugin.
     * The implementation of the probe action is deferred to {@link Probe#doApply(Plugin, ProbeContext)}.
     *
     * @param plugin  the plugin on which to perform the analyze
     * @param context holder of information passed across the probes executed on a single plugin
     * @return the result of the analyze in a {@link ProbeResult}
     */
    public final ProbeResult apply(Plugin plugin, ProbeContext context) {
        return doApply(plugin, context);
    }

    /**
     * Performs the analysis on a plugin.
     * Based on the provided plugin and context, the method returns a non-null {@link ProbeResult}.
     *
     * @param plugin  the plugin on which the analyze is done
     * @param context holder of information passed across the probes executed on a single plugin
     * @return a ProbeResult representing the result of the analyze
     */
    protected abstract ProbeResult doApply(Plugin plugin, ProbeContext context);

    /**
     * Returns the key identifier for the probe.
     * This is how the different probes can be identified in the {@link Plugin#details} map.
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
     * @return true if a release of the plugin is required to change the result of the probe execution, otherwise false.
     */
    protected boolean requiresRelease() {
        return false;
    }

    /**
     * Determines that the probe requires a modification of the source code of the plugin to change its previous
     * execution on the plugin.
     *
     * @return true if the result of the probe can only be change from the previous execution if the source code of the
     * plugin was change. Otherwise false.
     */
    protected boolean isSourceCodeRelated() {
        return false;
    }
}
