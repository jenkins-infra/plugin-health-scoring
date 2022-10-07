package io.jenkins.pluginhealth.scoring.model.updatecenter;

import hudson.util.VersionNumber;

public record SecurityWarningVersion(
    VersionNumber lastVersion,
    String pattern
) {
}
