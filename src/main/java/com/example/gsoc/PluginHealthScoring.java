package com.example.gsoc;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.example.gsoc.model.Plugin;
import com.example.gsoc.service.PluginService;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;

@SpringBootApplication
@EnableEncryptableProperties
public class PluginHealthScoring implements CommandLineRunner {

	@Autowired
	private PluginService pluginService;

	@Value("${jenkins.update.center}")
	private String updateCenterURL;

	public static void main(String[] args) {
		SpringApplication.run(PluginHealthScoring.class, args);
	}

	@Override
	public void run(String... args) throws StreamReadException, DatabindException, IOException {
		List<Plugin> pluginList = pluginService.readUpdateCenter(updateCenterURL);

		for (Plugin plugin : pluginList) {			
			pluginService.saveOrUpdate(plugin);
		}
	}
}
