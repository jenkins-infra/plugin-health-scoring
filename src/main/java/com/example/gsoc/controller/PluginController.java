package com.example.gsoc.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;

import com.example.gsoc.model.Plugin;
import com.example.gsoc.model.UpdateCenter;
import com.example.gsoc.service.PluginService;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class PluginController {
	@Autowired
	PluginService pluginService;
	
	public PluginController() throws StreamReadException, DatabindException, IOException {
		readUpdateCenter();
	}
	
	public List<Plugin> readUpdateCenter() throws StreamReadException, DatabindException, IOException {
		List<Plugin> pluginList = new ArrayList<Plugin>();

		URL jsonUrl = new URL("https://updates.jenkins.io/current/update-center.actual.json");

	    ObjectMapper mapper = new ObjectMapper();

	    UpdateCenter updateCenter = mapper.readValue(jsonUrl, UpdateCenter.class);
	    
	    Iterator<Entry<String, JsonNode>> fields = updateCenter.getPlugins().fields();
	    
	    while (fields.hasNext()) {
	      Map.Entry<String, JsonNode> field = fields.next();
	      Plugin currentPlugin = new Plugin();
	      
	      Iterator<Entry<String, JsonNode>> innerfields = field.getValue().fields();
	      
	      while (innerfields.hasNext()) {
	    	  Map.Entry<String, JsonNode> innerfieldsMapEntry = innerfields.next();
	    	  
	    	  if (innerfieldsMapEntry.getKey() == "name") {
	    		  currentPlugin.setName(innerfieldsMapEntry.getValue().toString());
	    	  }
	    	  
	    	  if (innerfieldsMapEntry.getKey() == "releaseTimestamp") {
	    		  currentPlugin.setReleaseTimestamp(innerfieldsMapEntry.getValue().toString());
	    	  }
	    	  
	    	  if (innerfieldsMapEntry.getKey() == "scm") {
	    		  currentPlugin.setScm(innerfieldsMapEntry.getValue().toString());
	    	  }
	      }
	      
	      pluginList.add(currentPlugin);
	    }

	    return pluginList;
	}

}
