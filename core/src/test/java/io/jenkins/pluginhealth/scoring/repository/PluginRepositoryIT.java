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

package io.jenkins.pluginhealth.scoring.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.AbstractDBContainerTest;
import io.jenkins.pluginhealth.scoring.model.Plugin;

import hudson.util.VersionNumber;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class PluginRepositoryIT extends AbstractDBContainerTest {
    @Autowired private PluginRepository repository;
    @Autowired private TestEntityManager entityManager;

    @Test
    void shouldBeEmpty() {
        assertThat(repository.count()).isZero();
    }

    @Test
    void shouldBeAbleToSaveOnePlugin() {
        final Plugin myPlugin = repository.save(new Plugin("myPlugin", new VersionNumber("1.0"), "this-is-ok", ZonedDateTime.now()));
        assertThat(myPlugin).extracting("name").isEqualTo("myPlugin");
        assertThat(myPlugin).extracting("version").isEqualTo(new VersionNumber("1.0"));
        assertThat(myPlugin).extracting("scm").isEqualTo("this-is-ok");
        assertThat(myPlugin).extracting("releaseTimestamp").isNotNull();
    }

    @Test
    void shouldBeAbleToFindAll() {
        final Plugin plugin1 = new Plugin("plugin-1", new VersionNumber("1.0"), "scm", ZonedDateTime.now());
        entityManager.persist(plugin1);
        final Plugin plugin2 = new Plugin("plugin-2", new VersionNumber("1.0"), "scm", ZonedDateTime.now());
        entityManager.persist(plugin2);
        final Plugin plugin3 = new Plugin("plugin-3", new VersionNumber("1.1"), "scm", ZonedDateTime.now());
        entityManager.persist(plugin3);

        assertThat(repository.findAll()).hasSize(3).contains(plugin1, plugin2, plugin3);
    }

    @Test
    void shouldBeAbleToFindByName() {
        final Plugin plugin1 = new Plugin("plugin-1", new VersionNumber("1.0"), "scm", ZonedDateTime.now());
        entityManager.persist(plugin1);
        final Plugin plugin2 = new Plugin("plugin-2", new VersionNumber("1.0"), "scm", ZonedDateTime.now());
        entityManager.persist(plugin2);

        final Optional<Plugin> pluginFromDB = repository.findByName("plugin-1");
        assertThat(pluginFromDB).isPresent().contains(plugin1);
    }

    @Test
    void shouldBeAbleToUpdatePlugin() {
        final Plugin plugin1 = new Plugin("plugin-1", new VersionNumber("1.0"), "scm", ZonedDateTime.now().minusMinutes(10));
        entityManager.persist(plugin1);
        final Plugin plugin2 = new Plugin("plugin-2", new VersionNumber("1.0"), "scm", ZonedDateTime.now());
        entityManager.persist(plugin2);

        final Plugin plugin1Updated = new Plugin("plugin-1", new VersionNumber("1.1"), "update-scm", ZonedDateTime.now().minusMinutes(2));

        final Optional<Plugin> pluginFromDBOpt = repository.findByName(plugin1Updated.getName());
        assertThat(pluginFromDBOpt).isPresent();

        final Plugin pluginFromDB = pluginFromDBOpt.get();
        pluginFromDB.setScm(plugin1Updated.getScm());
        pluginFromDB.setVersion(plugin1Updated.getVersion());
        pluginFromDB.setReleaseTimestamp(plugin1Updated.getReleaseTimestamp());

        repository.save(pluginFromDB);

        final Optional<Plugin> pluginToCheck = repository.findByName(plugin1.getName());
        assertThat(pluginToCheck)
            .isPresent()
            .get()
            .extracting(
                Plugin::getName,
                Plugin::getScm,
                Plugin::getVersion,
                Plugin::getReleaseTimestamp)
            .containsExactly(
                plugin1Updated.getName(),
                plugin1Updated.getScm(),
                plugin1Updated.getVersion(),
                plugin1Updated.getReleaseTimestamp());
    }
}
