package com.example.gsoc;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.example.gsoc.controller.PluginController;
import com.example.gsoc.model.Plugin;
import com.example.gsoc.repository.PluginRepository;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

@SpringBootApplication
public class PluginHealthScoring implements CommandLineRunner {

	@Autowired
	private PluginRepository pluginRepository;

	public static void main(String[] args) {
		SpringApplication.run(PluginHealthScoring.class, args);
	}

	@Override
	public void run(String... args) throws StreamReadException, DatabindException, IOException {
		PluginController pluginController = new PluginController();
		List<Plugin> pluginList = pluginController.readUpdateCenter();
		
		for (Plugin plugin : pluginList) {			
			pluginRepository.save(plugin);
		}
	}
}
