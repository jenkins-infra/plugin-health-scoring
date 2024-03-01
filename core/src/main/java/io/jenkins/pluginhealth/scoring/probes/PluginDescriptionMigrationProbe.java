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
import java.util.Optional;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(PluginDescriptionMigrationProbe.ORDER)
public class PluginDescriptionMigrationProbe extends Probe {
    public static final String KEY = "description-migration";
    public static final int ORDER = DocumentationMigrationProbe.ORDER + 100;

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        final Optional<Path> scmRepositoryOpt = context.getScmRepository();
        if (scmRepositoryOpt.isEmpty()) {
            return error("Cannot access plugin repository.");
        }

        final Path repository = scmRepositoryOpt.get();
        final Path pluginFolder = context.getScmFolderPath().map(repository::resolve).orElse(repository);

        try (final Stream<Path> files = Files.find(
            pluginFolder.resolve("src").resolve("main").resolve("resources"),
            1,
            (path, attributes) -> "index.jelly".equals(path.getFileName().toString())
        )) {
            final Optional<Path> jellyFileOpt = files.findFirst();
            if (jellyFileOpt.isEmpty()) {
                return success("There is no `index.jelly` file in `src/main/resources`.");
            }
            final Path jellyFile = jellyFileOpt.get();
            return Files.readAllLines(jellyFile).stream().map(String::trim).anyMatch(s -> s.contains("TODO")) ?
                    success("Plugin is using description from the plugin archetype.") :
                    success("Plugin seems to have a correct description.");
        } catch (IOException e) {
            return error("Cannot browse plugin source code folder.");
        }
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Checks if the plugin description is located in the `src/main/resources/index.jelly` file.";
    }

    @Override
    public long getVersion() {
        return 1;
    }

    @Override
    protected boolean requiresRelease() {
        return true;
    }
}
