package com.example.gsoc.service;

import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.gsoc.model.Plugin;
import com.example.gsoc.repository.PluginRepository;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PluginService {
	@Autowired
	private final PluginRepository pluginRepository;

	@Autowired
	public PluginService(PluginRepository pluginRepository) {
		this.pluginRepository = pluginRepository;
	}

	public List<Plugin> readUpdateCenter(String updateCenterURL) throws StreamReadException, DatabindException, IOException {
		List<Plugin> plugins = new ArrayList<Plugin>();

		URL jsonUrl = new URL(updateCenterURL);

	    ObjectMapper mapper = new ObjectMapper();

		JsonNode updateCenterJsonNode = mapper.readTree(jsonUrl);

		JsonNode pluginListJsonNode = updateCenterJsonNode.get("plugins");

		Iterator<String> pluginListIterator = pluginListJsonNode.fieldNames();

		while(pluginListIterator.hasNext()) {
			String currentPluginName = pluginListIterator.next();
			
			Plugin plugin = new Plugin();

			plugin.setName(pluginListJsonNode.at("/" + currentPluginName + "/name").toString());
			plugin.setScm(pluginListJsonNode.at("/" + currentPluginName + "/scm").toString());

			String releaseTimestampString = pluginListJsonNode.at("/" + currentPluginName + "/releaseTimestamp").toString();

			// Remove double quotes to make it parsable into ZonedDateTime
			plugin.setReleaseTimestamp(ZonedDateTime.parse(releaseTimestampString.substring(1, releaseTimestampString.length() - 1)));

			plugins.add(plugin);
		}

	    return plugins;
	}

	public List<Plugin> getAllPlugins() {
		List<Plugin> plugins = new ArrayList<Plugin>();  
		pluginRepository.findAll().forEach(plugin -> plugins.add(plugin));  
		return plugins;
	}
	
	public void saveOrUpdate(Plugin plugin) {  
		pluginRepository.save(plugin);  
	}

}
