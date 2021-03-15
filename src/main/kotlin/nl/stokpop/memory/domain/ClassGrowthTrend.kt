/**
 * Copyright (C) 2020 Peter Paul Bakker, Stokpop Software Solutions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.stokpop.memory.domain

import nl.stokpop.memory.MemoryCheckException
import kotlin.streams.toList

data class ClassGrowthTrend(
        val timestamps: List<Long>,
        val data: Map<ClassInfo, ClassGrowth>
) {
    init {
        val unequalHistoLinesCountClassnames = data.values.stream().filter { it.histoLines.size != timestamps.size }.map { it.classInfo.name }.toList()
        if (unequalHistoLinesCountClassnames.isNotEmpty())
            throw MemoryCheckException("size of histo lines is not equal to timestamps count (${timestamps.size}) for (${unequalHistoLinesCountClassnames})")
    }

    fun statusCount(
        status: AnalysisResult,
        bytesLimit: Long
    ) = data.values
            .filter { it.analysisResult == status }
            .filter { it.histoLines.last().bytes ?: 0 > bytesLimit }
            .count()

    fun statusFilter(
        status: AnalysisResult
    ) = data.values.filter { it.analysisResult == status }
}