/*
 * MIT License
 *
 * Copyright (c) 2023-2026 Jenkins Infra
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
package io.jenkins.pluginhealth.scoring.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GitHub;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

@Component
public class GitHubHealthIndicator extends AbstractHealthIndicator {
    private final GitHub github;

    public GitHubHealthIndicator(GitHub github) {
        this.github = github;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            if (Objects.isNull(github)) {
                builder.down().withDetail("error", "GitHub object is null");
                return;
            }
            github.checkApiUrlValidity();
            boolean authenticated = !github.isAnonymous();
            Map<String, Object> connection = new HashMap<>();
            connection.put("authenticated", authenticated);
            GHRateLimit rateLimit = github.lastRateLimit();
            if (rateLimit != null) {
                connection.put(
                        "rateLimit",
                        Map.of(
                                "limit", rateLimit.getLimit(),
                                "remaining", rateLimit.getRemaining(),
                                "resetDate", rateLimit.getResetDate()));
            }
            if (authenticated) {
                builder.up();
            } else {
                builder.outOfService();
            }
            builder.withDetail("connection", connection);
        } catch (IOException ex) {
            builder.down(ex);
        }
    }
}
