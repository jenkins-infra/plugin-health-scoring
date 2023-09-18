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

@Component
@Order(JenkinsCoreProbe.ORDER)
public class JenkinsCoreProbe extends Probe {
    public static final String KEY = "jenkins-version";
    public static final int ORDER = UpdateCenterPluginPublicationProbe.ORDER + 100;

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final UpdateCenter uc = context.getUpdateCenter();
        final io.jenkins.pluginhealth.scoring.model.updatecenter.Plugin ucPlugin = uc.plugins().get(plugin.getName());
        return ucPlugin != null ?
            ProbeResult.success(key(), ucPlugin.requiredCore(), this.getVersion()) :
            ProbeResult.error(key(), "Could not find plugin " + plugin.getName() + " in Update Center.", this.getVersion());
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "This probe registers the Jenkins core version requirement for each plugin.";
    }

    @Override
    protected boolean requiresRelease() {
        return true;
    }

    @Override
    public long getVersion() {
        return 1;
    }
}
