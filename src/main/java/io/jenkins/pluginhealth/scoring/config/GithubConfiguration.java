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

package io.jenkins.pluginhealth.scoring.config;

import java.io.IOException;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "github")
@Validated
public class GithubConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(GithubConfiguration.class);
    private static final long DEFAULT_TTL = 60 * 1000;

    private final String appId;
    private final long ttl;

    @ConstructorBinding
    public GithubConfiguration(String appId, long ttl) {
        this.appId = appId;
        if (ttl > 10 * 60 * 1000) { /* TTL must be less than 10min */
            LOGGER.warn("GitHub App Token cannot have a TTL of more than 10min, using default value of {}ms", DEFAULT_TTL);
            this.ttl = DEFAULT_TTL;
        } else {
            this.ttl = ttl == 0 ? DEFAULT_TTL : ttl;
            LOGGER.info("GitHub JWT TTL: {}ms", this.ttl);
        }
    }

    public GitHub getGitHub() throws IOException {
        return new GitHubBuilder()
            .withJwtToken(createJWT())
            .build();
    }

    // TODO
    private String createJWT() {
        return "";
    }
}
