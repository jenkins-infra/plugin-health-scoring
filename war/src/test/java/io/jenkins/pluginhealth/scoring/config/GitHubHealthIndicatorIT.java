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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.Date;

import io.jenkins.pluginhealth.scoring.AbstractDBContainerTest;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GitHub;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "app.github.app-id=test-app-id",
            "app.github.app-installation-name=test-installation",
            "app.github.private-key-path=/dev/null",
            "management.endpoint.health.show-details=always",
            "management.endpoint.health.show-components=always"
        })
class GitHubHealthIndicatorIT extends AbstractDBContainerTest {
    @MockitoBean
    private GitHub github;

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = restTemplate();

    private static RestTemplate restTemplate() {
        RestTemplate t = new RestTemplate();
        t.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }

            @Override
            public void handleError(URI url, HttpMethod method, ClientHttpResponse response) {}
        });
        return t;
    }

    private String healthUrl(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void healthEndpointIsUpWithConnectionDetailsWhenAuthenticated() throws IOException {
        GHRateLimit rateLimit = mock(GHRateLimit.class);
        when(github.isAnonymous()).thenReturn(false);
        when(github.lastRateLimit()).thenReturn(rateLimit);
        when(rateLimit.getLimit()).thenReturn(5000);
        when(rateLimit.getRemaining()).thenReturn(4999);
        when(rateLimit.getResetDate()).thenReturn(new Date(0));

        ResponseEntity<String> response = restTemplate.getForEntity(healthUrl("/actuator/health/gitHub"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("\"status\":\"UP\"")
                .contains("\"connection\"")
                .contains("\"authenticated\":true")
                .contains("\"rateLimit\"")
                .contains("\"limit\":5000")
                .contains("\"remaining\":4999")
                .contains("\"resetDate\"");
    }

    @Test
    void healthEndpointIsOutOfServiceWhenAnonymous() throws IOException {
        GHRateLimit rateLimit = mock(GHRateLimit.class);
        when(github.isAnonymous()).thenReturn(true);
        when(github.lastRateLimit()).thenReturn(rateLimit);
        when(rateLimit.getLimit()).thenReturn(60);
        when(rateLimit.getRemaining()).thenReturn(59);
        when(rateLimit.getResetDate()).thenReturn(new Date(0));

        ResponseEntity<String> response = restTemplate.getForEntity(healthUrl("/actuator/health/gitHub"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).contains("\"status\":\"OUT_OF_SERVICE\"").contains("\"authenticated\":false");
    }

    @Test
    void healthEndpointHasNoRateLimitKeyWhenNotCachedYet() throws IOException {
        when(github.isAnonymous()).thenReturn(false);
        when(github.lastRateLimit()).thenReturn(null);

        ResponseEntity<String> response = restTemplate.getForEntity(healthUrl("/actuator/health/gitHub"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"authenticated\":true").doesNotContain("\"rateLimit\"");
    }
}
