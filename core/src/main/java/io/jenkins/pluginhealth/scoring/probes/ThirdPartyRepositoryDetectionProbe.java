package io.jenkins.pluginhealth.scoring.probes;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    final String hostName = "https://repo.jenkins-ci.org";
//    final String parentPom = "https://raw.githubusercontent.com/jenkinsci/plugin-pom/master/pom.xml";
    final String parentPom = "https://github.com/jenkinsci/plugin-pom/blob/master/pom.xml";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        MavenXpp3Reader mavenReader = new MavenXpp3Reader();
        Set<Repository> allRepositories = new HashSet<>();
        FileReader fileReader = null;

        try {
            fileReader = new FileReader(context.getScmRepository() + "/pom.xml");
            Model model = mavenReader.read(fileReader);
            allRepositories.addAll(model.getRepositories());
            allRepositories.addAll(model.getPluginRepositories());

            if (!model.getParent().getRelativePath().isBlank()) {
                Model parentPomModel = parsePomFromUrl(model.getParent().getRelativePath());
                allRepositories.addAll(parentPomModel.getRepositories());
                allRepositories.addAll(parentPomModel.getPluginRepositories());
            }
            for (Repository repository : allRepositories) {
                if (!repository.getUrl().startsWith(hostName)) {
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

        } finally {
            if (fileReader!=null) {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
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
        return new String[] { SCMLinkValidationProbe.KEY};
    }

    public Model parsePomFromUrl(String pomUrl) {
        Model model = null;
        FileReader reader = null;
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
                Path absolutePath = Paths.get(pomUrl).toAbsolutePath().normalize();
                reader = new FileReader(absolutePath.toString());
                model = new MavenXpp3Reader().read(reader);

            }
        } catch (IOException e) {
            LOGGER.error("File could not be found {}", e.getMessage());
        } catch (XmlPullParserException e) {
            LOGGER.error("Pom file could not be parsed {}", e.getMessage());
        } finally {
            if (reader != null ) {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return model;
    }
}
