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
import nl.stokpop.memory.domain.AnalysisResult.*
import nl.stokpop.memory.domain.HeapHistogramDumpLine.Companion.createGhostLine
import nl.stokpop.memory.report.ReportConfig

object HistoAnalyser {

    fun analyse(dumps: List<HeapHistogramDump>, config: ReportConfig): ClassGrowthTrend {
        if (dumps.isEmpty()) return ClassGrowthTrend(emptyList(), emptyMap())

        // what if new classes appear in new histos? create super set!
        val classNames = dumps.flatMap { it.findAllClassNames() }.toSet()
        val timestamps = dumps.map { it.timestamp.atZone(MemoryCheck.useZoneOffset()).toInstant().toEpochMilli() }.toList()
        // sort on number of bytes in last heap histogram dump, reverse() -> biggest first
        val data = classNames
                .map { className -> className to processClassNameForHistoInfos(className, dumps, config) }
                .toList()
                .sortedBy { (_, value) -> value.histoLines.last().bytes }
                .reversed()
                .toMap()
        return ClassGrowthTrend(timestamps, data)
    }

    private fun processClassNameForHistoInfos(name: ClassInfo, dumps: List<HeapHistogramDump>, config: ReportConfig): ClassGrowth {
        val histosForClassName = dumps
            .map { dump -> dump.histogram.firstOrNull { name == it.classInfo } ?: createGhostLine(name) }
            .toList()

        val analyseGrowth = analyseGrowth(
            histosForClassName,
            name.isOnSafeList,
            config.reportLimits.maxGrowthPercentage,
            config.reportLimits.minGrowthPointsPercentage
        ) { it.bytes }

        return ClassGrowth(name, histosForClassName, analyseGrowth)
    }

    fun analyseGrowth(
        histoLines: List<HeapHistogramDumpLine>,
        isSafeToGrow: Boolean = false,
        maxGrowthPercentage: Double = 10.0,
        minGrowthPointPercentage: Double = 50.0,
        thingToCheck: (HeapHistogramDumpLine) -> Long? = { it.bytes }): AnalysisResult {

        if (histoLines.isEmpty()) return UNKNOWN

        val histoSize = histoLines.size
        if (histoSize == 1) return UNKNOWN
        val compareSize = histoSize - 1

        // assume the lines are ordered in time
        var growthCriticalCount = 0
        var growthMinorCount = 0
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
                val percentageGrowth = 100 * ((currentValue - lastValue) / lastValue.toDouble())
                when {
                    lastValue < currentValue && percentageGrowth > maxGrowthPercentage -> growthCriticalCount++
                    lastValue < currentValue && percentageGrowth <= maxGrowthPercentage -> growthMinorCount++
                    lastValue > currentValue -> shrinkCount++
                    else -> stableCount++
                }
            }
            lastValue = currentValue
        }

        if (ghostCount == compareSize) return UNKNOWN

        val totalGrowthCount = growthCriticalCount + growthMinorCount
        val growthPointsPercentage = (totalGrowthCount/compareSize.toDouble()) * 100.0
        val hasEnoughGrowthPoints = growthPointsPercentage >= minGrowthPointPercentage

        if (growthCriticalCount > 0
            && totalGrowthCount + ghostCount + stableCount == compareSize
            && !lastIsGhost) {
            return if (hasEnoughGrowthPoints) { if (isSafeToGrow) GROW_SAFE else GROW_CRITICAL } else GROW_HICK_UPS
        }
        if (growthMinorCount > 0
            && growthMinorCount + ghostCount + stableCount == compareSize
            && !lastIsGhost) {
            return if (hasEnoughGrowthPoints) { if (isSafeToGrow) GROW_SAFE else GROW_MINOR } else GROW_HICK_UPS
        }

        if (totalGrowthCount + ghostCount == compareSize && lastIsGhost) return UNKNOWN
        if (shrinkCount > 0 && shrinkCount + ghostCount + stableCount == compareSize) return SHRINK
        if (stableCount + ghostCount == compareSize) return STABLE

        return SHRINK_AND_GROW
    }
}
