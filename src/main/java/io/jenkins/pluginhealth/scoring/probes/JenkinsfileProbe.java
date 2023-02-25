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
@Order(value = JenkinsfileProbe.ORDER)
public class JenkinsfileProbe extends Probe {
    private static final Logger LOGGER = LoggerFactory.getLogger(JenkinsfileProbe.class);
    public static final int ORDER = LastCommitDateProbe.ORDER + 100;
    public static final String KEY = "jenkinsfile";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        if (plugin.getDetails().get(SCMLinkValidationProbe.KEY) == null) {
            LOGGER.error("Couldn't run {} on {} because previous SCMLinkValidationProbe has null value in database", key(), plugin.getName());
            return ProbeResult.error(key(), "SCM link has not been validated yet");
        }
        final Path repository = context.getScmRepository();
        try (Stream<Path> paths = Files
            .find(repository, 1, (file, basicFileAttributes) -> Files.isReadable(file) && "Jenkinsfile".equals(file.getFileName().toString()))
        ) {
            return paths.findFirst()
                .map(file -> ProbeResult.success(key(), "Jenkinsfile found"))
                .orElseGet(() -> ProbeResult.failure(key(), "No Jenkinsfile found"));
        } catch (IOException e) {
            return ProbeResult.error(key(), e.getMessage());
        }
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public String getDescription() {
        return """
            Validates the existence of a `Jenkinsfile` file in the repository.
            This file is used to configure the plugin Continuous Integration on ci.jenkins.io.
            """;
    }

    @Override
    protected boolean isSourceCodeRelated() {
        return true;
    }
}
