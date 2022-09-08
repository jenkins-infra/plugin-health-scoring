package io.jenkins.pluginhealth.scoring.probes;

import io.jenkins.pluginhealth.scoring.model.Plugin;

public interface UpdateCenterProbe {
    public void runProbe(Plugin plugin);
}
