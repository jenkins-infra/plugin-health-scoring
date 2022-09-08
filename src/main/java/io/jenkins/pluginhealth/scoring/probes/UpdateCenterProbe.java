package io.jenkins.pluginhealth.scoring.probes;

import java.io.IOException;

import io.jenkins.pluginhealth.scoring.model.Plugin;

public interface UpdateCenterProbe {
    public void readUpdateCenter(Plugin plugin) throws IOException;
}
