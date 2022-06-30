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

package io.jenkins.pluginhealth.scoring.service;

import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jenkins.pluginhealth.scoring.model.Plugin;

@Service
public class UpdateCenterService {
    private final ObjectMapper objectMapper;
    private final String updateCenterURL;

    public UpdateCenterService(ObjectMapper objectMapper, @Value("${jenkins.update.center}") String updateCenterURL) {
        this.objectMapper = objectMapper;
        this.updateCenterURL = updateCenterURL;
    }

    public List<Plugin> readUpdateCenter() throws IOException {

        record UpdateCenterPlugin(String name, String scm, ZonedDateTime releaseTimestamp) {
            Plugin toPlugin() {
                return new Plugin(this.name, this.scm, this.releaseTimestamp);
            }
        }

        record UpdateCenter(Map<String, UpdateCenterPlugin> plugins) {
        }

        URL jsonUrl = new URL(updateCenterURL);

        UpdateCenter updateCenter = objectMapper.readValue(jsonUrl, UpdateCenter.class);

        return updateCenter.plugins.values().stream()
            .map(UpdateCenterPlugin::toPlugin)
            .collect(Collectors.toList());
    }

}
