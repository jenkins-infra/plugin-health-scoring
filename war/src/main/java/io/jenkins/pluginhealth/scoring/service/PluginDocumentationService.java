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

package io.jenkins.pluginhealth.scoring.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PluginDocumentationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginDocumentationService.class);

    private final ObjectMapper objectMapper;
    private final String pluginDocumentationUrls;

    public PluginDocumentationService(ObjectMapper objectMapper,
                                      @Value("${jenkins.documentation-urls}") String pluginDocumentationUrls) {
        this.objectMapper = objectMapper;
        this.pluginDocumentationUrls = pluginDocumentationUrls;
    }

    public Map<String, String> fetchPluginDocumentationUrl() {
        try {
            final Map<String, Link> foo = objectMapper.readValue(new URL(pluginDocumentationUrls), new TypeReference<>() {});
            return foo.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().url()
                ));
        } catch (MalformedURLException e) {
            LOGGER.error("URL to documentation link is incorrect.", e);
            return Map.of();
        } catch (IOException e) {
            LOGGER.error("Could not fetch plugin documentation.", e);
            return Map.of();
        }
    }

    record Link(String url) {}
}
