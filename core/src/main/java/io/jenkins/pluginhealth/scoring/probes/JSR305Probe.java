package io.jenkins.pluginhealth.scoring.probes;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * This probe checks for deprecated @Nonnull and @CheckForNull annotations in a plugin.
 */
@Component
@Order(value = AbstractDeprecatedAnnotationImportsCheckProbe.ORDER)
public class JSR305Probe extends AbstractDeprecatedAnnotationImportsCheckProbe {
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
        return "JSR305 annotation is updated";
    }

    @Override
    String getFailureMessage() {
        return "Deprecated annotations found";
    }

    @Override
    public List<String> getListOfDeprecatedImports() {
        return List.of(
            "import javax.annotation.Nonnull;",
            "import javax.annotation.CheckForNull;"
        );
    }
}
