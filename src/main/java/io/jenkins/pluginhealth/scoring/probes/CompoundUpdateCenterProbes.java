package io.jenkins.pluginhealth.scoring.probes;

import java.io.IOException;
import java.util.List;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;

@Component
@Order(value = CompoundUpdateCenterProbes.ORDER)
public class CompoundUpdateCenterProbes extends Probe implements UpdateCenterProbe {
    public static final int ORDER = 1;

    private final List<UpdateCenterProbe> updateCenterProbeList;
    private final ObjectMapper objectMapper;
    private final String updateCenterURL;

    public CompoundUpdateCenterProbes(List<UpdateCenterProbe> updateCenterProbeList,@Value("${jenkins.update-center}") String updateCenterURL) {
        this.updateCenterProbeList = List.copyOf(updateCenterProbeList);
        this.objectMapper = Jackson2ObjectMapperBuilder.json().build();;
        this.updateCenterURL = updateCenterURL;
    }

    @Override
    public void readUpdateCenter(Plugin plugin) throws IOException {
        for (UpdateCenterProbe updateCenterProbe : updateCenterProbeList) {
                updateCenterProbe.readUpdateCenter(plugin);
        }
    }

    @Override
    protected ProbeResult doApply(Plugin plugin) {
        try {
            readUpdateCenter(plugin);

            return ProbeResult.success(key(), "Read the update center for all the probes");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ProbeResult.failure(key(), "Error in reading the update-center");
    }

    @Override
    public String key() {
        return "update-center-probes";
    }
}
