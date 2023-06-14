package io.jenkins.pluginhealth.scoring.probes;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

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
public class ThirdPartyRepositoryDetectionProbe extends Probe {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThirdPartyRepositoryDetectionProbe.class);
    public static final int ORDER = SCMLinkValidationProbe.ORDER + 100;
    public static final String KEY = "third-party-repository-detection-probe";
    private static final String JENKINS_CI_REPO_URL = "https://repo.jenkins-ci.org";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        MavenXpp3Reader mavenReader = new MavenXpp3Reader();
        Set<Repository> allRepositories = new HashSet<>();

        try(InputStream inputStream = new FileInputStream(context.getScmRepository() + "/pom.xml");
              Reader reader = new InputStreamReader(inputStream, "UTF-8")) {
            Model model = mavenReader.read(reader);
            allRepositories.addAll(model.getRepositories());
            allRepositories.addAll(model.getPluginRepositories());

            if (!model.getParent().getRelativePath().isBlank()) {
                Model parentPomModel = parsePomFromUrl(model.getParent().getRelativePath());
                allRepositories.addAll(parentPomModel.getRepositories());
                allRepositories.addAll(parentPomModel.getPluginRepositories());
            }
            for (Repository repository : allRepositories) {
                if (!repository.getUrl().startsWith(JENKINS_CI_REPO_URL)) {
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
        return allRepositories.size() > 0 ? ProbeResult.success(KEY, "The plugin has no third party repositories")
            : ProbeResult.failure(KEY, "No repositories detected");
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
        return new String[] { SCMLinkValidationProbe.KEY };
    }

    public Model parsePomFromUrl(String pomUrl) {
        Model model = null;

        try {
            if (pomUrl.startsWith(("https"))) {
                URL url = new URL(pomUrl);
                try (InputStream inputStream = url.openStream()) {
                    MavenXpp3Reader mavenReader = new MavenXpp3Reader();
                    model = mavenReader.read(inputStream);
                }
            }
            else {
                // for test cases
                InputStream inputStream = new FileInputStream(pomUrl);
                Reader reader = new InputStreamReader(inputStream, "UTF-8");
                model = new MavenXpp3Reader().read(reader);
            }
        } catch (IOException e) {
            LOGGER.error("File could not be found {}", e.getMessage());
        } catch (XmlPullParserException e) {
            LOGGER.error("Pom file could not be parsed {}", e.getMessage());
        }
        return model;
    }
}
