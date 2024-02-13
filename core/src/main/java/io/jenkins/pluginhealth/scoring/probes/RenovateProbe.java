package io.jenkins.pluginhealth.scoring.probes;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Looks for Renovate bot configuration in a plugin.
 */
@Component
@Order(RenovateProbe.ORDER)
public class RenovateProbe extends AbstractDependencyBotConfigurationProbe {
    public static final String KEY = "renovate";
    public static final int ORDER = AbstractDependencyBotConfigurationProbe.ORDER;

    @Override
    protected String getBotName() {
        return KEY;
    }

    @Override
    protected boolean isPathBotConfigFile(String filename) {
        return "renovate.json".equals(filename);
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public String getDescription() {
        return "Checks if Renovate is configured in a plugin.";
    }

    @Override
    public long getVersion() {
        return 2;
    }
}
