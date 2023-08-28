package io.jenkins.pluginhealth.scoring.probes;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * This probe checks for deprecated @Nonnull and @CheckForNull annotations in a plugin.
 */
@Component
@Order(value = AbstractJavaImportsCheckProbe.ORDER)
public class JSR305Probe extends AbstractJavaImportsCheckProbe {
    public static final String KEY = "JSR-305";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "The probe checks for deprecated annotations.";
    }

    @Override
    String getSuccessMessage() {
        return "Latest version of imports found";
    }

    @Override
    String getFailureMessage() {
        return "Deprecated imports found";
    }

    @Override
    public List<String> getListOfImports() {
        return List.of(
            "javax.annotation.Nonnull",
            "javax.annotation.CheckForNull"
        );
    }
}
