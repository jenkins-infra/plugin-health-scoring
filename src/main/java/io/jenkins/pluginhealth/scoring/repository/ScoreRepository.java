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

package io.jenkins.pluginhealth.scoring.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.Score;
import io.jenkins.pluginhealth.scoring.model.ScoreDTO;
import io.jenkins.pluginhealth.scoring.model.ScoreResult;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Long> {
    Optional<Score> findFirstByPluginOrderByComputedAtDesc(Plugin plugin);

    @Query(
        value = """
                SELECT DISTINCT
                    new io.jenkins.pluginhealth.scoring.model.ScoreDTO(
                        s.id,
                        p.name,
                        p.version,
                        s.value
                    ),
                    s.computedAt
                FROM Score s
                JOIN Plugin p on s.plugin = p
                ORDER BY p.name ASC, s.computedAt DESC
            """
    )
    List<ScoreDTO> getLatestScoreForPlugins();

    @Query(
        value = """
                SELECT
                    s.details
                FROM Score s
                WHERE s.id = :id
            """
    )
    Set<ScoreResult> findDetailsById(long id);
}
