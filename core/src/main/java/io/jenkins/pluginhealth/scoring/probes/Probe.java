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
     * Should only be called by the {@link ProbeEngine#run()} method.
     *
     * @param plugin  the plugin on which to perform the analyze
     * @param context holder of information passed across the probes executed on a single plugin
     * @return the result of the analyze in a {@link ProbeResult}
     */
    public final ProbeResult apply(Plugin plugin, ProbeContext context) {
        return doApply(plugin, context);
    }

    /**
     * Perform the analyze on a plugin
     *
     * @param plugin  the plugin on which the analyze is done
     * @param context holder of information passed across the probes executed on a single plugin
     * @return a ProbeResult representing the result of the analyze
     */
    protected abstract ProbeResult doApply(Plugin plugin, ProbeContext context);

    public abstract String key();

    public abstract String getDescription();

    protected boolean requiresRelease() {
        return false;
    }

    protected boolean isSourceCodeRelated() {
        return false;
    }
}
