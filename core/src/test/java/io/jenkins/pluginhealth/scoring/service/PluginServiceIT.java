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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;

import io.jenkins.pluginhealth.scoring.AbstractDBContainerTest;
import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.repository.PluginRepository;

import hudson.util.VersionNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class PluginServiceIT extends AbstractDBContainerTest {
    @Autowired private TestEntityManager entityManager;
    @Autowired private PluginRepository pluginRepository;
    private PluginService pluginService;

    @BeforeEach
    void setup() {
        this.pluginService = new PluginService(pluginRepository);
    }

    @Test
    void shouldNotDuplicatePluginWhenNameIsTheSame() {
        final Plugin p1 = entityManager.persist(
            new Plugin("myPlugin", new VersionNumber("1.0"), "https://github.com/jenkinsci/my-plugin", null)
        );
        final Plugin p2 = entityManager.persist(
            new Plugin("bar", new VersionNumber("1.2"), "scm-2", ZonedDateTime.now())
        );

        assertThat(pluginService.streamAll())
            .hasSize(2)
            .containsExactlyInAnyOrder(p2, p1);

        final Plugin copy = new Plugin("myPlugin", new VersionNumber("1.1"), "https://github.com/jenkinsci/my-plugin", null);
        pluginService.saveOrUpdate(copy);
        assertThat(pluginService.streamAll())
            .hasSize(2)
            .containsExactlyInAnyOrder(p2, copy);
    }

    @Test
    void shouldBeAbleToSavePluginWithThreeDigitVersion() {
        final Plugin plugin = entityManager.persist(
            new Plugin("foo-bar", new VersionNumber("1.0.1"), null, ZonedDateTime.now())
        );

        assertThat(pluginService.getPluginsCount()).isEqualTo(1);
        assertThat(pluginService.findByName("foo-bar"))
            .isPresent()
            .get()
            .extracting("version")
            .isNotNull()
            .isEqualTo(new VersionNumber("1.0.1"));
    }

    @Test
    void shouldReturnsNonNullObjectWhenNoPluginWithName() {
        assertThat(pluginService.findByName("not-existing"))
            .isNotNull()
            .isEmpty();
    }
}
