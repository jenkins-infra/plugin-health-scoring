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

package io.jenkins.pluginhealth.scoring.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;

import jakarta.validation.constraints.NotEmpty;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.authorization.OrgAppInstallationAuthorizationProvider;
import org.kohsuke.github.extras.authorization.JWTTokenProvider;
import org.kohsuke.github.extras.okhttp3.OkHttpGitHubConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "github")
@Validated
public class GithubConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(GithubConfiguration.class);

    @NotEmpty
    private final String appId;
    private final Path privateKeyPath;
    @NotEmpty
    private final String appInstallationName;

    @ConstructorBinding
    public GithubConfiguration(String appId, Path privateKeyPath, String appInstallationName) {
        this.appId = appId;
        this.privateKeyPath = privateKeyPath;
        this.appInstallationName = appInstallationName;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Private Key Path: {} [valid: {}]", privateKeyPath, Files.exists(privateKeyPath) && Files.isReadable(privateKeyPath));
        }
    }

    @Bean
    public GitHub getGitHub() throws IOException, GeneralSecurityException {
        final OkHttpClient httpClient = new OkHttpClient.Builder().cache(
            new Cache(Files.createTempDirectory("http_cache").toFile(), 50 * 1024 * 1024)
        ).build();
        return new GitHubBuilder()
            .withAuthorizationProvider(createAuthorizationProvider())
            .withConnector(new OkHttpGitHubConnector(httpClient))
            .build();
    }

    private OrgAppInstallationAuthorizationProvider createAuthorizationProvider() throws GeneralSecurityException, IOException {
        final JWTTokenProvider jwtTokenProvider = new JWTTokenProvider(appId, privateKeyPath);
        return new OrgAppInstallationAuthorizationProvider(appInstallationName, jwtTokenProvider);
    }
}
