/*
 * MIT License
 *
 * Copyright (c) 2023-2025 Jenkins Infra
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import io.jenkins.pluginhealth.scoring.config.ApplicationConfiguration;
import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;

import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class UpdateCenterService {
    private final ObjectMapper objectMapper;
    private final ApplicationConfiguration configuration;

    public UpdateCenterService(ObjectMapper objectMapper, ApplicationConfiguration configuration) {
        this.objectMapper = objectMapper;
        this.configuration = configuration;
    }

    public UpdateCenter fetchUpdateCenter() throws IOException {
        try (HttpClient client = HttpClient.newBuilder().build()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(configuration.jenkins().updateCenter()))
                .GET()
                .build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            return objectMapper.readValue(response.body(), UpdateCenter.class);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
