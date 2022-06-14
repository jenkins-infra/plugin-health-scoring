package com.example.gsoc.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.gsoc.repository.PluginRepository;
import com.example.gsoc.model.Plugin;

@Service
public class PluginService {
	@Autowired
	PluginRepository pluginRepository;
	
	public List<Plugin> getAllPlugins() {
		List<Plugin> plugins = new ArrayList<Plugin>();  
		pluginRepository.findAll().forEach(plugin -> plugins.add(plugin));  
		return plugins;
	}
	
	public void saveOrUpdate(Plugin plugin) {  
		pluginRepository.save(plugin);  
	}

}
