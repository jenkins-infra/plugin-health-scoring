/*
 * MIT License
 *
 * Copyright (c) 2025 Jenkins Infra
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

import java.util.List;

import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(MavenDependenciesProbe.ORDER)
public class MavenDependenciesProbe extends AbstractMavenProbe {
    public static final int ORDER = MavenDependenciesManagementProbe.ORDER + 10;
    public static final String KEY = "maven-dependencies";

    @Override
    protected ProbeResult getMavenDetails(Model pom) {
        final List<Dependency> dependencies = pom.getDependencies();
        if (dependencies == null || dependencies.isEmpty()) {
            return success(List.of());
        }
        return success(dependencies.stream()
                .filter(dep -> !"provided".equals(dep.getScope()) && !"runtime".equals(dep.getScope()))
                .map(dep -> String.format(
                        "%s:%s:%s:%b", dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dep.isOptional()))
                .toList());
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return "List of dependencies used by the plugin";
    }

    @Override
    public long getVersion() {
        return 2;
    }
}
