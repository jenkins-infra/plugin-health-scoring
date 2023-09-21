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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(value = ContributingGuidelinesProbe.ORDER)
public class ContributingGuidelinesProbe extends Probe {
    public static final int ORDER = LastCommitDateProbe.ORDER + 100;
    public static final String KEY = "contributing-guidelines";

    @Override
    protected ProbeResult doApply(Plugin plugin, ProbeContext context) {
        if (context.getScmRepository().isEmpty()) {
            return this.error("There is no local repository for plugin " + plugin.getName() + ".");
        }

        final Path repository = context.getScmRepository().get();
        try (Stream<Path> paths = Files.find(repository, 2,
            (file, basicFileAttributes) -> Files.isReadable(file)
                && ("CONTRIBUTING.md".equalsIgnoreCase(file.getFileName().toString())
                || "CONTRIBUTING.adoc".equalsIgnoreCase(file.getFileName().toString())))) {
            return paths.findAny()
                .map(file -> this.success("Contributing guidelines found."))
                .orElseGet(() -> this.success("No contributing guidelines found."));
        } catch (IOException e) {
            return this.error(e.getMessage());
        }
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "Validates the existence of a `CONTRIBUTING.adoc` or `CONTRIBUTING.md` file in the repository.";
    }

    @Override
    protected boolean isSourceCodeRelated() {
        return true;
    }

    @Override
    public long getVersion() {
        return 1;
    }
}
