package io.jenkins.pluginhealth.scoring.service;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import io.jenkins.pluginhealth.scoring.AbstractDBContainerTest;
import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.repository.PluginRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest(includeFilters = @ComponentScan.Filter(Service.class))
public class PluginServiceIT extends AbstractDBContainerTest {
    @Autowired
    private PluginRepository pluginRepository;
    @Autowired
    private PluginService pluginService;

    @Test
    public void shouldNotDuplicatePluginWhenNameIsTheSame() {
        Plugin plugin = new Plugin("myPlugin", "https://github.com/jenkinsci/my-plugin", null);

        pluginService.saveOrUpdate(plugin);
        assertThat(pluginRepository.findAll())
            .hasSize(1)
            .contains(plugin);

        Plugin copy = new Plugin("myPlugin", "https://github.com/jenkinsci/my-plugin", null);
        pluginService.saveOrUpdate(copy);
        assertThat(pluginRepository.findAll())
            .hasSize(1)
            .contains(plugin);
    }

    @Test
    public void testBatchUpdates() {
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());

        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());
        pluginRepository.save(new Plugin());

//        List<Plugin> pluginList = new ArrayList<>();
//
//        for (int i = 0; i < 50; i++) {
//            pluginList.add(new Plugin());
//        }
//

        List<Plugin> pluginList = pluginRepository.findAll();
        pluginRepository.saveAll(pluginList);
    }
}
