package io.jenkins.pluginhealth.scoring.probes;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;

@Component
@Order(value = UpForAdoptionProbe.ORDER)
public class UpForAdoptionProbe extends Probe implements UpdateCenterProbe {
    public static final int ORDER = CompoundUpdateCenterProbes.ORDER + 1;

    private final ObjectMapper objectMapper;
    private final String updateCenterURL;
    private UpdateCenter updateCenter;

    public UpForAdoptionProbe(@Value("${jenkins.update-center}") String updateCenterURL) {
        this.objectMapper = Jackson2ObjectMapperBuilder.json().build();
        this.updateCenterURL = updateCenterURL;
    }

    record UpdateCenterPlugin(List<String> labels) {
    }

    record UpdateCenter(Map<String, UpdateCenterPlugin> plugins) {
    }

    @Override
    public void readUpdateCenter(Plugin plugin) throws IOException {
        updateCenter = objectMapper.readValue(new URL(updateCenterURL), UpdateCenter.class);
    }

    @Override
    protected ProbeResult doApply(Plugin plugin) {
        if (updateCenter.plugins().get(plugin.getName()).labels().contains("adopt-this-plugin"))
            return ProbeResult.success(key(), "This plugin is up for adoption");

        return ProbeResult.failure(key(), "This plugin is NOT up for adoption");
    }

    @Override
    public String key() {
        return "up-for-adoption";
    }
}
