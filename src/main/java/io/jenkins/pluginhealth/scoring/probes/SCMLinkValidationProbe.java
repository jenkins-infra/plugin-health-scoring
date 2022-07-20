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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import com.sun.istack.NotNull;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
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
    private static final String GH_REGEXP = "https://(?<server>[^/]*)/(?<org>[^/]*)/(?<repo>[^/]*)(?:/(?<folder>.*))??";
    private static final Pattern GH_PATTERN = Pattern.compile(GH_REGEXP);

    public static final int ORDER = 1;

    private final GitHub gitHub;

    public SCMLinkValidationProbe(GitHub gitHub) {
        this.gitHub = gitHub;
    }

    @Override
    public ProbeResult doApply(Plugin plugin) {
        if (plugin.getScm() == null || plugin.getScm().isBlank()) {
            LOGGER.warn("{} has no SCM link", plugin.getName());
            return ProbeResult.failure(key(), "No SCM link");
        }
        return fromSCMLink(plugin.getScm())
            .map(repo -> ProbeResult.success(key(), "SCM link is valid"))
            .orElseGet(() -> ProbeResult.failure(key(), "SCM link is invalid"));
    }

    public String key() {
        return "scm";
    }

    private Optional<GHRepository> fromSCMLink(@NotNull String scm) {
        Matcher matcher = GH_PATTERN.matcher(scm);
        if (!matcher.find()) {
            LOGGER.debug("{} is not respecting the SCM URL Template", scm);
            return Optional.empty();
        }
        try {
            GHOrganization org = this.gitHub.getOrganization(matcher.group("org"));
            GHRepository repo = org.getRepository(matcher.group("repo"));
            if (repo == null) {
                return Optional.empty();
            }
            if (matcher.group("folder") != null) {
                LOGGER.debug("{}", matcher.group("folder"));
                // TODO
            }
            return Optional.of(repo);
        } catch (IOException ex) {
            return Optional.empty();
        }
    }
}
