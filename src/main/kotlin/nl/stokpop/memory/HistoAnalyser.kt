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
package nl.stokpop.memory

import nl.stokpop.memory.domain.*
import nl.stokpop.memory.domain.HeapHistogramDumpLine.Companion.createGhostLine

object HistoAnalyser {
    fun analyse(histos: List<HeapHistogramDump>): ClassGrowthTrend {
        if (histos.isEmpty()) return ClassGrowthTrend(emptyList(), emptyMap())

        // what if new classes appear in new histos? create super set!
        val classNames = histos.flatMap { it.findAllClassNames() }.toSet()
        val timestamps = histos.map { it.timestamp.atZone(MemoryCheck.useZoneOffset()).toInstant().toEpochMilli() }.toList()
        // sort on number of bytes in last heap histogram dump, reverse() -> biggest first
        val data = classNames
                .map { it to processClassNameForHistoInfos(it, histos) }
                .toList()
                .sortedBy { (_, value) -> value.histoLines.last().bytes }
                .reversed()
                .toMap()
        return ClassGrowthTrend(timestamps, data)
    }

    private fun processClassNameForHistoInfos(name: ClassName, histos: List<HeapHistogramDump>): ClassGrowth {
        val histosForClassName = histos
            .map { it.histogram.firstOrNull { name == it.className } ?: createGhostLine(name) }
            .toList()
        return ClassGrowth(name, histosForClassName, analyseGrowth(histosForClassName) { it.bytes })
    }

    fun analyseGrowth(
            histoLines: List<HeapHistogramDumpLine>,
            thingToCheck: (HeapHistogramDumpLine) -> Long? = { it.instances }): AnalysisResult {

        if (histoLines.isEmpty()) return AnalysisResult.UNKNOWN

        val histoSize = histoLines.size
        if (histoSize == 1) return AnalysisResult.UNKNOWN
        val compareSize = histoSize - 1

        // assume the lines are ordered in time
        var growthCount = 0
        var shrinkCount = 0
        var stableCount = 0
        var ghostCount = 0
        var lastIsGhost = false
        var lastValue = -1L

        for (line in histoLines) {
            lastIsGhost = false
            if (line.isGhost()) {
                ghostCount++
                lastIsGhost = true
                continue
            }

            // only ghosts have null values
            val currentValue = thingToCheck(line)!!

            if (lastValue != -1L) {
                when {
                    lastValue < currentValue -> growthCount++
                    lastValue > currentValue -> shrinkCount++
                    else -> stableCount++
                }
            }
            lastValue = currentValue
        }

        if (ghostCount == compareSize) return AnalysisResult.UNKNOWN

        if (growthCount > 0 && growthCount + ghostCount + stableCount == compareSize && !lastIsGhost) return AnalysisResult.GROW
        if (growthCount + ghostCount == compareSize && lastIsGhost) return AnalysisResult.UNKNOWN
        if (shrinkCount > 0 && shrinkCount + ghostCount + stableCount == compareSize) return AnalysisResult.SHRINK
        if (stableCount + ghostCount == compareSize) return AnalysisResult.STABLE

        return AnalysisResult.UNKNOWN
    }
}
