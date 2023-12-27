/*
 * MIT License
 *
 * Copyright (c) 2023 Jenkins Infra
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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

/**
 * This probe checks whether build failed on Default Branch or not.
 */
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
          if(repoContainsJenkins(context).toString().equals("Jenkinsfile found")){

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
