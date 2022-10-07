package io.jenkins.pluginhealth.scoring.model.updatecenter;

import java.util.List;

public record SecurityWarning(
    String id,
    String name,
    List<SecurityWarningVersion> versions
) {
}
