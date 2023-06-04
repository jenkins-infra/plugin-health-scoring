package io.jenkins.pluginhealth.scoring.probes;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(value = ThirdPartyRepositoryDetectionProbe.ORDER)
public class ThirdPartyRepositoryDetectionProbe extends Probe{
    private static final Logger LOGGER = LoggerFactory.getLogger(ThirdPartyRepositoryDetectionProbe.class);

    public static final int ORDER = SCMLinkValidationProbe.ORDER + 100;
    public static final String KEY = "third-party-repository-detection-probe";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final String path = "https://repo.jenkins-ci.org";
        MavenXpp3Reader mavenReader = new MavenXpp3Reader();
        try {
            Model model = mavenReader.read(new FileReader(context.getScmRepository()+"/pom.xml"));
            for (Repository repository : getRepositories(model)) {
                if(!(repository.getUrl().startsWith(path))) {
                    return ProbeResult.failure(KEY, "Third party repositories detected in the plugin");
                }
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("File not found at {}", plugin.getName());
            return ProbeResult.error(KEY, e.getMessage());
        } catch (XmlPullParserException e) {
            LOGGER.error("Pom file could not be parsed at {}", plugin.getName());
            return ProbeResult.error(KEY, e.getMessage());
        } catch (IOException e) {
            LOGGER.error("File reading exception at {}", plugin.getName());
            return ProbeResult.error(KEY, e.getMessage());

        }
        return ProbeResult.success(KEY, "The plugin has no third party repositories");
    }

    private static List<Repository> getRepositories(Model model) {
        return model.getRepositories();
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Detects third-party repositories in a plugin.";
    }

    @Override
    public String[] getProbeResultRequirement() {
        return new String[] { SCMLinkValidationProbe.KEY};
    }
}
