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
package io.jenkins.pluginhealth.scoring.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import io.jenkins.pluginhealth.scoring.config.ApplicationConfiguration;
import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import tools.jackson.databind.ObjectMapper;

@JsonTest
class UpdateCenterServiceTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldBeAbleToParseUpdateCenterWithNoDeprecations() throws Exception {
        URL updateCenterURL = UpdateCenterServiceTest.class.getResource("/update-center/no-deprecation.json");
        assertThat(updateCenterURL).isNotNull();

        final ApplicationConfiguration configuration = new ApplicationConfiguration(
                new ApplicationConfiguration.Jenkins(updateCenterURL.toString(), "foo"),
                new ApplicationConfiguration.GitHub("foo", null, "bar"));

        UpdateCenterService updateCenterService = new UpdateCenterService(objectMapper, configuration);

        UpdateCenter updateCenter = updateCenterService.fetchUpdateCenter();
        assertThat(updateCenter.plugins()).hasSize(25);
    }

    @Test
    void shouldThrowIOExceptionOnNonOkHttpResponse() throws Exception {
        byte[] htmlBody = "<html>Service Unavailable</html>".getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/uc.json", exchange -> {
            exchange.sendResponseHeaders(503, htmlBody.length);
            try (var os = exchange.getResponseBody()) {
                os.write(htmlBody);
            }
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            ApplicationConfiguration configuration = new ApplicationConfiguration(
                    new ApplicationConfiguration.Jenkins("http://localhost:%d/uc.json".formatted(port), "foo"),
                    new ApplicationConfiguration.GitHub("foo", null, "bar"));
            UpdateCenterService service = new UpdateCenterService(objectMapper, configuration);
            assertThatThrownBy(service::fetchUpdateCenter)
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("503");
        } finally {
            server.stop(0);
        }
    }
}
