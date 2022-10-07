package io.jenkins.pluginhealth.scoring.probes;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.model.updatecenter.UpdateCenter;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * This probe detects if a specified plugin is deprecated from the update-center.
 */
@Component
@Order(DeprecatedPluginProbe.ORDER)
public class DeprecatedPluginProbe extends Probe {
    public static final int ORDER = 1;

    @Override
    public ProbeResult doApply(Plugin plugin, ProbeContext ctx) {
        final UpdateCenter updateCenter = ctx.getUpdateCenter();
        if (updateCenter.deprecations().containsKey(plugin.getName())) {
            return ProbeResult.failure(key(), updateCenter.deprecations().get(plugin.getName()).url());
        }
        return ProbeResult.success(key(), "This plugin is NOT deprecated");
    }

    @Override
    public String key() {
        return "deprecation";
    }

    @Override
    public String getDescription() {
        return "This probe detects if a specified plugin is deprecated from the update-center.";
    }
}
