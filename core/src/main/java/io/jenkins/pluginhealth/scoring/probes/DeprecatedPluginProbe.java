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
import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * This probe detects if a specified plugin is deprecated from the update-center.
 */
@Component
@Order(DeprecatedPluginProbe.ORDER)
public class DeprecatedPluginProbe extends Probe {
    public static final int ORDER = 0;
    public static final String KEY = "deprecation";

    @Override
    public ProbeResult doApply(Plugin plugin, ProbeContext ctx) {
        final UpdateCenter updateCenter = ctx.getUpdateCenter();
        if (updateCenter.deprecations().containsKey(plugin.getName())) {
            return ProbeResult.success(key(), updateCenter.deprecations().get(plugin.getName()).url(), this.getVersion());
        }
        final io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin updateCenterPlugin = updateCenter.plugins().get(plugin.getName());
        if (updateCenterPlugin == null) {
            return ProbeResult.error(key(), "This plugin is not in update-center.", this.getVersion());
        }
        return updateCenterPlugin.labels().contains("deprecated") ?
            ProbeResult.success(key(), "This plugin is marked as deprecated.", this.getVersion()) :
            ProbeResult.success(key(), "This plugin is NOT deprecated.", this.getVersion());
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "This probe detects if a specified plugin is deprecated from the update-center.";
    }

    @Override
    public long getVersion() {
        return 1;
    }
}
