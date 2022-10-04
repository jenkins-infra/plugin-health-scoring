package io.jenkins.pluginhealth.scoring.probes;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.UpdateCenter;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * This probe aims to see if a specified plugin is declared as up for adoption.
 */
@Component
@Order(value = UpForAdoptionProbe.ORDER)
public class UpForAdoptionProbe extends Probe {
    public static final int ORDER = DeprecatedPluginProbe.ORDER + 1;

    @Override
    public ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final UpdateCenter updateCenter = context.getUpdateCenter();
        if (updateCenter.plugins().get(plugin.getName()).labels().contains("adopt-this-plugin")) {
            return ProbeResult.failure(key(), "This plugin is up for adoption");
        }

        return ProbeResult.success(key(), "This plugin is NOT up for adoption");
    }

    @Override
    public String key() {
        return "up-for-adoption";
    }

    @Override
    public String getDescription() {
        return "This probe detects if a specified plugin is declared as up for adoption.";
    }
}
