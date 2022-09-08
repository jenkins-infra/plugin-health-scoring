package io.jenkins.pluginhealth.scoring.probes;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;

@Component
@Order(value = DeprecatedPluginProbe.ORDER)
public class DeprecatedPluginProbe extends Probe implements UpdateCenterProbe {
    public static final int ORDER = CompoundUpdateCenterProbes.ORDER + 1;

    public static final String DEPRECATION_KEY = "deprecation";

    private final ObjectMapper objectMapper;
    private final String updateCenterURL;
    private UpdateCenter updateCenter;

    public DeprecatedPluginProbe(@Value("${jenkins.update-center}") String updateCenterURL) {
        this.objectMapper = Jackson2ObjectMapperBuilder.json().build();
        this.updateCenterURL = updateCenterURL;
    }

    record UpdateCenterDeprecations(String url) {
    }

    record UpdateCenter(Map<String, UpdateCenterDeprecations> deprecations) {
    }

    @Override
    public void readUpdateCenter(Plugin plugin) throws IOException {
        updateCenter = objectMapper.readValue(new URL(updateCenterURL), UpdateCenter.class);
    }

    @Override
    protected ProbeResult doApply(Plugin plugin) {
        if (updateCenter.deprecations().containsKey(plugin.getName())) {
            return ProbeResult.failure(key(), updateCenter.deprecations().get(plugin.getName()).url());
        }
        return ProbeResult.success(key(), "This plugin is NOT deprecated");
    }

    @Override
    public String key() {
        return DEPRECATION_KEY;
    }
}
