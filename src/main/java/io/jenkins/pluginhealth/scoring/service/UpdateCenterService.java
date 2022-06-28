package io.jenkins.pluginhealth.scoring.service;

import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jenkins.pluginhealth.scoring.model.Plugin;

@Service
public class UpdateCenterService {
    @Autowired
    ObjectMapper objectMapper;
    
    PluginService pluginService;
        
    public UpdateCenterService(PluginService pluginService) {
        this.pluginService = pluginService;
    }

    public void readUpdateCenter(String updateCenterURL) throws StreamReadException, DatabindException, IOException {
        record UpdateCenterPlugin(String name, String scm, ZonedDateTime releaseTimestamp) {
        }

		record UpdateCenter(Map<String, UpdateCenterPlugin> plugins) {
		}

		URL jsonUrl = new URL(updateCenterURL);

		UpdateCenter updateCenter = objectMapper.readValue(jsonUrl, UpdateCenter.class);

		updateCenter.plugins.keySet().forEach(k -> pluginService.saveOrUpdate(new Plugin(updateCenter.plugins.get(k).name, updateCenter.plugins.get(k).scm, updateCenter.plugins.get(k).releaseTimestamp)));

	}
    
}
