package io.jenkins.pluginhealth.scoring.probes;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

public class DeprecatedPluginProbe implements UpdateCenterProbe {
    public static final String DEPRECATION_KEY = "deprecation";

    private final ObjectMapper objectMapper;
    private final String updateCenterURL;

    public DeprecatedPluginProbe(@Value("${jenkins.update-center}") String updateCenterURL) {
        this.objectMapper = Jackson2ObjectMapperBuilder.json().build();
        this.updateCenterURL = updateCenterURL;
    }

    public String key() {
        return DEPRECATION_KEY;
    }

    @Override
    public void runProbe(Plugin plugin) {
        record UpdateCenterDeprecations(String url) {
        }

        record UpdateCenter(Map<String, UpdateCenterDeprecations> deprecations) {
        }

        try {
            UpdateCenter updateCenter = objectMapper.readValue(new URL(updateCenterURL), UpdateCenter.class);
            if (updateCenter.deprecations().containsKey(plugin.getName())) {
                plugin.addDetails(ProbeResult.failure(key(), updateCenter.deprecations().get(plugin.getName()).url()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
