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

import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.repository.PluginRepository;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PluginService {
	private final PluginRepository pluginRepository;

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
