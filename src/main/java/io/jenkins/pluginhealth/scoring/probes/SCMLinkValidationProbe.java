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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.ResultStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Analyzes the {@link Plugin#getScm()} value to determine if the link is valid or not.
 */
@Component
@Order(value = SCMLinkValidationProbe.ORDER)
public final class SCMLinkValidationProbe extends Probe {
    private static final Logger LOGGER = LoggerFactory.getLogger(SCMLinkValidationProbe.class);

    public static final String GH_REGEXP = "https://(?<server>[^/]*)/(?<repo>jenkinsci/[^/]*)(?:/(?<folder>.*))?";
    public static final Pattern GH_PATTERN = Pattern.compile(GH_REGEXP);
    public static final int ORDER = DeprecatedPluginProbe.ORDER + 20;
    public static final String KEY = "scm";

    @Override
    public ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final ProbeResult deprecatedProbeResult = plugin.getDetails().get(DeprecatedPluginProbe.KEY);
        if (deprecatedProbeResult != null && deprecatedProbeResult.status().equals(ResultStatus.FAILURE)) {
            return ProbeResult.failure(key(), "Plugin is deprecated");
        }
        if (plugin.getScm() == null || plugin.getScm().isBlank()) {
            LOGGER.warn("{} has no SCM link", plugin.getName());
            return ProbeResult.failure(key(), "The plugin SCM link is empty");
        }
        return fromSCMLink(context, plugin.getScm());
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return """
            The SCMLinkValidation probe validates with GitHub API \
            if the known SCM link of a plugin is correct or not.
            """;
    }

    @Override
    protected boolean requiresRelease() {
        return true;
    }

    private ProbeResult fromSCMLink(ProbeContext context, String scm) {
        Matcher matcher = GH_PATTERN.matcher(scm);
        if (!matcher.find()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{} is not respecting the SCM URL Template", scm);
            }
            return ProbeResult.failure(key(), "SCM link doesn't match GitHub plugin repositories");
        }

        try {
            context.getGitHub().getRepository(matcher.group("repo"));
            return ProbeResult.success(key(), "The plugin SCM link is valid");
        } catch (IOException ex) {
            return ProbeResult.failure(key(), "The plugin SCM link is invalid");
        }
    }
}
