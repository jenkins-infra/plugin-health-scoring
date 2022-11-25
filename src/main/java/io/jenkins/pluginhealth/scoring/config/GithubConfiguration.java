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
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "github")
@ConstructorBinding
@Validated
public class GithubConfiguration {
    @NotBlank private final String oauth;

    public GithubConfiguration(String oauth) {
        this.oauth = oauth;
    }

    public String getGitAccessToken() {
        return oauth;
    }

    private HttpRequest generateRequest(String endpoint) {
        return HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/%s".formatted(endpoint)))
            .timeout(Duration.ofSeconds(2))
            .header("Authorization", "token %s".formatted(this.getGitAccessToken()))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Plugin-Health-Scoring")
            .GET()
            .build();
    }

    public HttpResponse<String> request(String endpoint) throws IOException, InterruptedException {
        return this.request(endpoint, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    public <T> HttpResponse<T> request(String endpoint, HttpResponse.BodyHandler<T> bodyHandler)
        throws IOException, InterruptedException {
        return this.getHttpClient().send(generateRequest(endpoint), bodyHandler);
    }

    private HttpClient getHttpClient() {
        return HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_NONE))
            .build();
    }
}
