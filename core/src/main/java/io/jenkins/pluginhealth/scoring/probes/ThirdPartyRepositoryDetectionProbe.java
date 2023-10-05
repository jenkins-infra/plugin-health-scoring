package io.jenkins.pluginhealth.scoring.probes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(value = ThirdPartyRepositoryDetectionProbe.ORDER)
public class ThirdPartyRepositoryDetectionProbe extends Probe {
    public static final int ORDER = SCMLinkValidationProbe.ORDER + 100;
    public static final String KEY = "third-party-repository-detection-probe";
    private static final Logger LOGGER = LoggerFactory.getLogger(ThirdPartyRepositoryDetectionProbe.class);
    private static final String JENKINS_CI_REPO_URL = "https://repo.jenkins-ci.org";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        MavenXpp3Reader mavenReader = new MavenXpp3Reader();
        Set<Repository> allRepositories = new HashSet<>();

        if (!generateEffectivePom(context.getScmRepository() + "/pom.xml")) {
            return ProbeResult.failure(KEY, "Failure in generating effective-pom in the plugin.");
        }

        try (InputStream inputStream = new FileInputStream(context.getScmRepository() + "/effective-pom.xml");
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            Model model = mavenReader.read(reader);
            allRepositories.addAll(model.getRepositories());
            allRepositories.addAll(model.getPluginRepositories());

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
    public String[] getProbeResultRequirement() {
        return new String[]{SCMLinkValidationProbe.KEY};
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Detects third-party repositories in a plugin.";
    }

    /**
     * This method generates {@code effective-pom} based on the root {@code pom} in a plugin repository.
     *
     * @param effectivePomPath path of the effective pom file
     * @return true if the {@code effective-pom} is generated successfully. False otherwise.
     */
    public boolean generateEffectivePom(String effectivePomPath) {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(effectivePomPath));  // setting the parent pom that will be at the root. Parent of all the modules (the super parent)
        request.setGoals(Collections.singletonList("help:effective-pom -Doutput=effective-pom.xml"));
        try {
            Invoker invoker = new DefaultInvoker();
            invoker.setMavenHome(new File(System.getenv("MAVEN_HOME")));
            InvocationResult result = invoker.execute(request);

            if (result.getExitCode() != 0) {
                if (result.getExecutionException() != null) {
                    LOGGER.error("Exception occurred when invoking maven request {}", result.getExecutionException());
                } else {
                    LOGGER.error("Exception occurred when invoking maven request. The exit code is {}", result.getExitCode());
                }
                return false;
            }
            return true;
        } catch (MavenInvocationException e) {
            LOGGER.error("Exception occurred when invoking maven command {}", e);
            return false;
        }
    }
}
