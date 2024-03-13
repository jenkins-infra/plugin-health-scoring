/*
 * MIT License
 *
 * Copyright (c) 2022-2023 Jenkins Infra
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
import static org.assertj.core.api.Assertions.entry;

import java.net.URL;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.config.ApplicationConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

@JsonTest
class PluginDocumentationServiceTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldBeAbleToParseAvailableFile() {
        final URL url =
                PluginDocumentationService.class.getResource("/documentation-urls/plugin-documentation-urls.json");
        assertThat(url).isNotNull();

        final ApplicationConfiguration config = new ApplicationConfiguration(
                new ApplicationConfiguration.Jenkins("foo", url.toString()),
                new ApplicationConfiguration.GitHub("foo", null, "bar"));

        final PluginDocumentationService service = new PluginDocumentationService(objectMapper, config);
        final Map<String, String> map = service.fetchPluginDocumentationUrl();

        assertThat(map)
                .contains(entry("foo", "https://wiki.jenkins-ci.org/display/JENKINS/foo+plugin"))
                .contains(entry("bar", "https://github.com/jenkinsci/bar-plugin"));
    }

    @Test
    void shouldBeAbleToParseFileWithNullValue() {
        final URL url = PluginDocumentationService.class.getResource(
                "/documentation-urls/plugin-documentation-urls-with-nulls.json");
        assertThat(url).isNotNull();

        final ApplicationConfiguration config = new ApplicationConfiguration(
                new ApplicationConfiguration.Jenkins("foo", url.toString()),
                new ApplicationConfiguration.GitHub("foo", null, "bar"));

        final PluginDocumentationService service = new PluginDocumentationService(objectMapper, config);
        final Map<String, String> map = service.fetchPluginDocumentationUrl();

        assertThat(map)
                .contains(entry("foo", "https://wiki.jenkins-ci.org/display/JENKINS/foo+plugin"))
                .contains(entry("bar", "https://github.com/jenkinsci/bar-plugin"));
    }

    @Test
    void shouldSurviveIncorrectlyConfiguredDocumentationURL() {
        final ApplicationConfiguration config = new ApplicationConfiguration(
                new ApplicationConfiguration.Jenkins("foo", "this-is-not-a-correct-url"),
                new ApplicationConfiguration.GitHub("foo", null, "bar"));
        final PluginDocumentationService service = new PluginDocumentationService(objectMapper, config);
        final Map<String, String> map = service.fetchPluginDocumentationUrl();

        assertThat(map).isEmpty();
    }
}
