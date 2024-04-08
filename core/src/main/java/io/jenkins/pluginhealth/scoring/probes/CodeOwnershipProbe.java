/*
 * MIT License
 *
 * Copyright (c) 2024 Jenkins Infra
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
@Order(CodeOwnershipProbe.ORDER)
public class CodeOwnershipProbe extends Probe {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDependencyBotConfigurationProbe.class);
    public static final String KEY = "code-ownership";
    public static final int ORDER = AbstractDependencyBotConfigurationProbe.ORDER + 100;

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        if (context.getScmRepository().isEmpty()) {
            return this.error("There is no local repository for plugin " + plugin.getName() + ".");
        }
        final Path scmRepository = context.getScmRepository().get();

        try (Stream<Path> paths = Files.find(
                scmRepository,
                2,
                (path, $) -> Files.isRegularFile(path)
                        && "CODEOWNERS".equals(path.getFileName().toString()))) {
            return paths.findFirst()
                    .map(file -> {
                        try {
                            return Files.readAllLines(file).stream()
                                            .anyMatch(line -> line.contains(
                                                    "@jenkinsci/%s-plugin-developers".formatted(plugin.getName())))
                                    ? this.success("CODEOWNERS file is valid.")
                                    : this.success("CODEOWNERS file is not set correctly.");
                        } catch (IOException ex) {
                            return this.error("Could not read CODEOWNERS file.");
                        }
                    })
                    .orElseGet(() -> this.success("No CODEOWNERS file found in plugin repository."));
        } catch (IOException ex) {
            LOGGER.error("Could not browse the plugin folder during probe {}", key(), ex);
            return this.error("Could not browse the plugin folder.");
        }
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Detects if the code ownership file is set correctly or not.";
    }

    @Override
    public long getVersion() {
        return 2;
    }

    @Override
    protected boolean isSourceCodeRelated() {
        return true;
    }
}
