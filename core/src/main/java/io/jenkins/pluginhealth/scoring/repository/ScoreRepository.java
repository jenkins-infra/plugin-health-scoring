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

package io.jenkins.pluginhealth.scoring.repository;

import java.util.List;
import java.util.Optional;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.Score;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Long> {
    Optional<Score> findFirstByPluginOrderByComputedAtDesc(Plugin plugin);

    @Query(
        value = """
                SELECT DISTINCT ON (s.plugin_id)
                    s.plugin_id,
                    s.value,
                    s.computed_at,
                    s.details,
                    s.id
                FROM scores s
                JOIN plugins p on s.plugin_id = p.id
                ORDER BY s.plugin_id, s.computed_at DESC;
            """,
        nativeQuery = true
    )
    List<Score> findLatestScoreForAllPlugins();

    @Query(
        value = """
            SELECT DISTINCT ON (s.plugin_id)
                s.value
            FROM scores s
            ORDER BY s.plugin_id, s.computed_at DESC, s.value;
            """,
        nativeQuery = true
    )
    int[] getLatestScoreValueOfEveryPlugin();
}
