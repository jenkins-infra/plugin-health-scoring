package io.jenkins.pluginhealth.scoring.probes;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Looks for Renovate bot configuration in a plugin.
 */
@Component
@Order(AbstractDependencyBotConfigurationProbe.ORDER)
public class RenovateProbe extends AbstractDependencyBotConfigurationProbe {
    public static final String KEY = "renovate";

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
    public String getDescription() {
        return "Checks if Renovate is configured in a plugin.";
    }

    @Override
    public long getVersion() {
        return 2;
    }
}
