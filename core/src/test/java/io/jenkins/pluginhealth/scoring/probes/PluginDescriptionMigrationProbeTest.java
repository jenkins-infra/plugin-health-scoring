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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.junit.jupiter.api.Test;

public class PluginDescriptionMigrationProbeTest extends AbstractProbeTest<PluginDescriptionMigrationProbe> {
    @Override
    PluginDescriptionMigrationProbe getSpy() {
        return spy(PluginDescriptionMigrationProbe.class);
    }

    @Test
    void shouldRequireRelease() {
        assertThat(getSpy().requiresRelease()).isTrue();
    }

    @Test
    void shouldRequiresPluginRepository() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        when(plugin.getName()).thenReturn("plugin-a");

        final PluginDescriptionMigrationProbe probe = getSpy();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result)
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "status", "message")
                .isEqualTo(ProbeResult.error(
                        PluginDescriptionMigrationProbe.KEY, "There is no local repository for plugin plugin-a.", 0));
    }

    @Test
    void shouldFailWhenPluginRepositoryIsEmpty() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        final Path repository = Files.createTempDirectory(getClass().getSimpleName());
        when(ctx.getScmRepository()).thenReturn(Optional.of(repository));

        final PluginDescriptionMigrationProbe probe = getSpy();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result)
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.error(
                        PluginDescriptionMigrationProbe.KEY, "Cannot browse plugin source code folder.", 0));
    }

    @Test
    void shouldDetectMissingJellyFile() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        final Path repository = Files.createTempDirectory(getClass().getSimpleName());
        Files.createDirectories(repository.resolve("src").resolve("main").resolve("resources"));
        when(ctx.getScmRepository()).thenReturn(Optional.of(repository));

        final PluginDescriptionMigrationProbe probe = getSpy();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result)
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.success(
                        PluginDescriptionMigrationProbe.KEY,
                        "There is no `index.jelly` file in `src/main/resources`.",
                        0));
    }

    @Test
    void shouldDetectExampleJellyFile() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        final Path repository = Files.createTempDirectory(getClass().getSimpleName());
        final Path resources = Files.createDirectories(
                repository.resolve("src").resolve("main").resolve("resources"));
        final Path jelly = Files.createFile(resources.resolve("index.jelly"));
        Files.writeString(jelly, """
            <div>
                TODO
            </div>
            """);
        when(ctx.getScmRepository()).thenReturn(Optional.of(repository));

        final PluginDescriptionMigrationProbe probe = getSpy();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result)
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.success(
                        PluginDescriptionMigrationProbe.KEY,
                        "Plugin is using description from the plugin archetype.",
                        0));
    }

    @Test
    void shouldDetectExampleJellyFileWithModule() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        final Path repository = Files.createTempDirectory(getClass().getSimpleName());
        final Path module = Files.createDirectory(repository.resolve("plugin"));
        final Path resources =
                Files.createDirectories(module.resolve("src").resolve("main").resolve("resources"));
        final Path jelly = Files.createFile(resources.resolve("index.jelly"));
        Files.writeString(jelly, """
            <div>
                TODO
            </div>
            """);
        when(ctx.getScmRepository()).thenReturn(Optional.of(repository));
        when(ctx.getScmFolderPath()).thenReturn(Optional.of(module));

        final PluginDescriptionMigrationProbe probe = getSpy();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result)
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.success(
                        PluginDescriptionMigrationProbe.KEY,
                        "Plugin is using description from the plugin archetype.",
                        0));
    }

    @Test
    void shouldDetectCorrectJellyFile() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        final Path repository = Files.createTempDirectory(getClass().getSimpleName());
        final Path resources = Files.createDirectories(
                repository.resolve("src").resolve("main").resolve("resources"));
        final Path jelly = Files.createFile(resources.resolve("index.jelly"));
        Files.writeString(
                jelly,
                """
            <div>
                This is a plugin doing something.
            </div>
            """);
        when(ctx.getScmRepository()).thenReturn(Optional.of(repository));

        final PluginDescriptionMigrationProbe probe = getSpy();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result)
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.success(
                        PluginDescriptionMigrationProbe.KEY, "Plugin seems to have a correct description.", 0));
    }

    @Test
    void shouldDetectCorrectJellyFileWithModule() throws Exception {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        final Path repository = Files.createTempDirectory(getClass().getSimpleName());
        final Path module = Files.createDirectory(repository.resolve("plugin"));
        final Path resources =
                Files.createDirectories(module.resolve("src").resolve("main").resolve("resources"));
        final Path jelly = Files.createFile(resources.resolve("index.jelly"));
        Files.writeString(
                jelly,
                """
            <div>
                This is a plugin doing something.
            </div>
            """);
        when(ctx.getScmRepository()).thenReturn(Optional.of(repository));
        when(ctx.getScmFolderPath()).thenReturn(Optional.of(module));

        final PluginDescriptionMigrationProbe probe = getSpy();
        final ProbeResult result = probe.apply(plugin, ctx);

        assertThat(result)
                .usingRecursiveComparison()
                .comparingOnlyFields("id", "message", "status")
                .isEqualTo(ProbeResult.success(
                        PluginDescriptionMigrationProbe.KEY, "Plugin seems to have a correct description.", 0));
    }
}
