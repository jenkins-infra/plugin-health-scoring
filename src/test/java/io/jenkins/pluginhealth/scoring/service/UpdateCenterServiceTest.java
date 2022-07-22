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

import static io.jenkins.pluginhealth.scoring.service.UpdateCenterService.DEPRECATION_KEY;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import io.jenkins.pluginhealth.scoring.model.Plugin;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.junit4.SpringRunner;

@JsonTest
@RunWith(SpringRunner.class)
class UpdateCenterServiceTest {

    @Test
    public void shouldBeAbleToParseUpdateCenterWithNoDeprecations() throws Exception {
        URL updateCenter = UpdateCenterServiceTest.class.getResource("/update-center/no-deprecation.json");
        assertThat(updateCenter).isNotNull();
        UpdateCenterService updateCenterService = new UpdateCenterService(updateCenter.toString());

        List<Plugin> plugins = updateCenterService.readUpdateCenter();

        assertThat(plugins).hasSize(25);
        assertThat(
            plugins.stream()
                .filter(plugin -> plugin.getDetails().containsKey(DEPRECATION_KEY))
                .collect(Collectors.toList())
        )
            .hasSize(0);
    }

    @Test
    public void shouldBeAbleToParseUpdateCenterWithDeprecations() throws Exception {
        URL updateCenter = UpdateCenterServiceTest.class.getResource("/update-center/with-deprecations.json");
        assertThat(updateCenter).isNotNull();
        UpdateCenterService updateCenterService = new UpdateCenterService(updateCenter.toString());

        List<Plugin> plugins = updateCenterService.readUpdateCenter();

        assertThat(plugins).hasSize(25);
        assertThat(
            plugins.stream()
                .filter(plugin -> plugin.getDetails().containsKey(DEPRECATION_KEY))
                .collect(Collectors.toList())
        )
            .hasSize(1)
            .contains(new Plugin("BlameSubversion", null, null));
    }
}
