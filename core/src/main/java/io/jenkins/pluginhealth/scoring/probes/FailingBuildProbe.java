package io.jenkins.pluginhealth.scoring.probes;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import org.kohsuke.github.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Component
@Order(FailingBuildProbe.ORDER)
public class FailingBuildProbe extends Probe{

    public static final int ORDER = LastCommitDateProbe.ORDER + 100;
    public static final String KEY = "failing-buildingProbe";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context){

      if (context.getRepositoryName(plugin.getScm()).isPresent()) {
            return ProbeResult.success(key(),"There is no local repository for plugin " + plugin.getName() + ".");
        }
      try {
          if(repoContainsJenkins(context).equals("Jenkinsfile found")){

            final GitHub gh = context.getGitHub();
            final GHRepository repository = gh.getRepository(context.getRepositoryName(plugin.getScm()).orElseThrow());
            repository.getDefaultBranch();

            GHCommit commit = repository.getCommit(repository.getDefaultBranch());
            GHCheckRun checkRun = commit.getCheckRuns().iterator().next();
            GHCheckRun.Conclusion conclusion = checkRun.getConclusion();

            if(conclusion == GHCheckRun.Conclusion.FAILURE){

                return ProbeResult.success(key(),"Build Failed in Default Branch");
            }
            else{
                return ProbeResult.success(key(),"Build is Success in Default Branch");
            }
        }
        else{

         return ProbeResult.failure(key(),"No JenkinsFile found");
        }
      }catch (IOException e) {
            return ProbeResult.error(key(), "Could not get failingBuilding Check");
        }

    }

    public ProbeResult repoContainsJenkins(ProbeContext context){
       final Path repository = context.getScmRepository();
        try (Stream<Path> paths = Files.find(repository, 1, (file, $) ->
            Files.isReadable(file) && "JenkinsFile".equals(file.getFileName().toString()))) {
            return paths.findFirst()
                .map(file -> ProbeResult.success (key(),"Jenkinsfile found"))
                .orElseGet(() -> ProbeResult.failure(key(),"No Jenkinsfile found"));
        } catch (IOException e) {
            return ProbeResult.failure(key(),e.getMessage());
        }
    }
    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Return whether the build is failed on default branch or not";
    }


}
