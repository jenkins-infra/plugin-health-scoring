/*
 * MIT License
 *
 * Copyright (c) 2022 Jenkins Infra
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(ContinuousDeploymentProbe.ORDER)
public class ContinuousDeploymentProbe extends Probe {
    public static final int ORDER = LastCommitDateProbe.ORDER + 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(ContinuousDeploymentProbe.class);

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final Path repo = context.getScmRepository();
        final Path githubWorkflow = repo.resolve(".github/workflows");
        if (Files.notExists(githubWorkflow)) {
            return ProbeResult.failure(key(), "Plugin has no GitHub Action configured");
        }
        try (Stream<Path> files = Files.find(githubWorkflow, 1, (path, basicFileAttributes) -> Files.isRegularFile(path) && "cd.yml".equals(path.getFileName().toString()))) {
            return files.findFirst().isPresent() ?
                ProbeResult.success(key(), "JEP-229 workflow definition found") :
                ProbeResult.failure(key(), "Could not find JEP-229 workflow definition");
        } catch (IOException ex) {
            LOGGER.warn("Could not walk {} Git clone in {}", plugin.getName(), repo, ex);
            return ProbeResult.error(key(), "Could not work plugin repository");
        }
    }

    @Override
    public String key() {
        return "jep-229";
    }

    @Override
    public String getDescription() {
        return "Checks if JEP-229 (Continuous Deployment) has been activated on the plugin";
    }
}
