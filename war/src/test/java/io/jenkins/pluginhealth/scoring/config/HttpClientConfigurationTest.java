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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.service.PluginDocumentationService;
import io.jenkins.pluginhealth.scoring.service.UpdateCenterService;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import tools.jackson.databind.json.JsonMapper;

class HttpClientConfigurationTest {
    @Test
    void shouldFollowRedirectsAndCloseClientWithContext() {
        HttpClient client;
        try (var context = new AnnotationConfigApplicationContext(HttpClientConfiguration.class)) {
            client = context.getBean(HttpClient.class);
            assertThat(client.followRedirects()).isEqualTo(HttpClient.Redirect.NORMAL);
            assertThat(client.isTerminated()).isFalse();
        }
        assertThat(client.isTerminated()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldUseSharedClientForRepeatedFetchesFromBothServices() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<byte[]> documentationResponse = mock(HttpResponse.class);
        when(documentationResponse.statusCode()).thenReturn(200);
        when(documentationResponse.headers())
                .thenReturn(HttpHeaders.of(Map.of("content-type", List.of("application/json")), (name, value) -> true));
        when(documentationResponse.body())
                .thenReturn("{\"example\":{\"url\":\"https://example.org/plugin\"}}".getBytes(StandardCharsets.UTF_8));
        HttpResponse<byte[]> updateCenterResponse = mock(HttpResponse.class);
        when(updateCenterResponse.statusCode()).thenReturn(200);
        try (var input = getClass().getResourceAsStream("/update-center/no-deprecation.json")) {
            assertThat(input).isNotNull();
            when(updateCenterResponse.body()).thenReturn(input.readAllBytes());
        }
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(documentationResponse, updateCenterResponse, documentationResponse, updateCenterResponse);

        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(HttpClient.class, () -> client);
            context.registerBean(JsonMapper.class, () -> new JsonMapper());
            context.registerBean(
                    ApplicationConfiguration.class,
                    () -> new ApplicationConfiguration(
                            new ApplicationConfiguration.Jenkins(
                                    "https://example.org/uc.json", "https://example.org/docs.json"),
                            new ApplicationConfiguration.GitHub("foo", null, "bar")));
            context.register(PluginDocumentationService.class, UpdateCenterService.class);
            context.refresh();

            var documentation = context.getBean(PluginDocumentationService.class);
            var updateCenter = context.getBean(UpdateCenterService.class);
            for (int i = 0; i < 2; i++) {
                assertThat(documentation.fetchPluginDocumentationUrl())
                        .containsEntry("example", "https://example.org/plugin");
                assertThat(updateCenter.fetchUpdateCenter().plugins()).hasSize(25);
            }
            ArgumentCaptor<HttpRequest> requests = ArgumentCaptor.forClass(HttpRequest.class);
            verify(client, times(4)).send(requests.capture(), any(HttpResponse.BodyHandler.class));
            assertThat(requests.getAllValues())
                    .extracting(request -> request.uri().toString())
                    .containsExactly(
                            "https://example.org/docs.json",
                            "https://example.org/uc.json",
                            "https://example.org/docs.json",
                            "https://example.org/uc.json");
        }
        verify(client).close();
    }
}
