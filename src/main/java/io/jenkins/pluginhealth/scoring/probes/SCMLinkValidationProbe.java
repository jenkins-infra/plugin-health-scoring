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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.jenkins.pluginhealth.scoring.config.GithubConfiguration;
import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import com.sun.istack.NotNull;
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

    private final HttpClient httpClient;
    private final GithubConfiguration githubConfiguration;

    public SCMLinkValidationProbe(HttpClient httpClient, GithubConfiguration githubConfiguration) {
        this.httpClient = httpClient;
        this.githubConfiguration = githubConfiguration;
    }

    @Override
    public ProbeResult doApply(Plugin plugin, ProbeContext context) {
        if (plugin.getScm() == null || plugin.getScm().isBlank()) {
            LOGGER.warn("{} has no SCM link", plugin.getName());
            return ProbeResult.failure(key(), "The plugin SCM link is empty");
        }
        return fromSCMLink(plugin.getScm());
    }

    @Override
    public String key() {
        return KEY;
    }

    public int getOrder() {
        return ORDER;
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

    private ProbeResult fromSCMLink(@NotNull String scm) {
        Matcher matcher = GH_PATTERN.matcher(scm);
        if (!matcher.find()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{} is not respecting the SCM URL Template", scm);
            }
            return ProbeResult.failure(key(), "SCM link doesn't match GitHub plugin repositories");
        }

        try {
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/%s".formatted(matcher.group("repo"))))
                .timeout(Duration.ofSeconds(2))
                .header("Authorization", "token %s".formatted(githubConfiguration.getGitAccessToken()))
                .header("User-Agent", "Plugin-Health-Scoring")
                .GET()
                .build();
            final HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return ProbeResult.success(key(), "The plugin SCM link is valid");
            } else {
                return ProbeResult.failure(key(), "The plugin SCM link is invalid");
            }
        } catch (IOException | InterruptedException ex) {
            return ProbeResult.failure(key(), "The plugin SCM cannot be validated");
        }
    }
}
