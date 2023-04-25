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

package io.jenkins.pluginhealth.scoring.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;

import io.jenkins.pluginhealth.scoring.AbstractDBContainerTest;
import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;
import io.jenkins.pluginhealth.scoring.repository.PluginRepository;
import io.jenkins.pluginhealth.scoring.service.PluginService;
import io.jenkins.pluginhealth.scoring.service.UpdateCenterService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class UpdateCenterSchedulerIT extends AbstractDBContainerTest {
    @Autowired private PluginRepository pluginRepository;
    @Autowired private ExecutorEngine executorEngine;
    @Mock private UpdateCenterService ucService;
    private UpdateCenterScheduler upScheduler;

    @BeforeEach
    void setupUpdateCenterContent() throws IOException {
        final UpdateCenter updateCenter = Jackson2ObjectMapperBuilder.json().build()
            .readValue(UpdateCenterSchedulerIT.class.getResourceAsStream("/update-center/update-center.actual.json"), UpdateCenter.class);

        when(ucService.fetchUpdateCenter()).thenReturn(updateCenter);
        upScheduler = new UpdateCenterScheduler(executorEngine, ucService, new PluginService(pluginRepository));
    }

    @Test
    void shouldBeEmpty() {
        assertThat(pluginRepository.count()).isZero();
    }

    @Test
    // TODO: Reduce size of the data set.
    void shouldBeAbleToInsertPluginsIntoDB() throws IOException {
        upScheduler.updateDatabase();
        assertThat(pluginRepository.count()).isEqualTo(1885);
    }

    @Test
    // TODO: Reduce size of the data set.
    void shouldBeAbleToUpdatePluginsInDB() throws IOException {
        upScheduler.updateDatabase();
        assertThat(pluginRepository.count()).isEqualTo(1885);
        upScheduler.updateDatabase();
        assertThat(pluginRepository.count()).isEqualTo(1885);
    }
}
