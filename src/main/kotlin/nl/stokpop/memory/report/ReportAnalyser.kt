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
package nl.stokpop.memory.report

import nl.stokpop.memory.domain.*
import nl.stokpop.memory.domain.json.ClassHistogramDetails
import nl.stokpop.memory.domain.json.HeapHistogramDumpDetails
import nl.stokpop.memory.domain.json.HeapHistogramDumpReport
import nl.stokpop.memory.domain.json.HeapHistogramDumpSummary

object ReportAnalyser {

    fun createHeapHistogramDumpReport(classGrowthTrend: ClassGrowthTrend, reportConfig: ReportConfig) : HeapHistogramDumpReport {

        val analysisResultToCount = AnalysisResult.values().asSequence()
                .map { it to classGrowthTrend.statusCount(it, reportConfig.byteLimit) }
                .toMap()

        val heapHistogramDumpSummary = HeapHistogramDumpSummary(analysisResultToCount)

        val leakResult = when {
            (heapHistogramDumpSummary.data.get(AnalysisResult.GROW) ?: 0) > 0 -> AnalysisResult.GROW
            (heapHistogramDumpSummary.data.get(AnalysisResult.SHRINK) ?: 0) > 0 -> AnalysisResult.SHRINK
            (heapHistogramDumpSummary.data.get(AnalysisResult.STABLE) ?: 0) > 0 -> AnalysisResult.STABLE
            else -> AnalysisResult.UNKNOWN
        }

        val analysisFilter: (Map.Entry<ClassName, ClassGrowth>) -> Boolean = {
            (reportConfig.doReportGrow && it.value.analysisResult == AnalysisResult.GROW)
                    || (reportConfig.doReportShrinks && it.value.analysisResult == AnalysisResult.SHRINK)
                    || (reportConfig.doReportStable && it.value.analysisResult == AnalysisResult.STABLE)
                    || (reportConfig.doReportUnknowns && it.value.analysisResult == AnalysisResult.UNKNOWN)}

        val classHistogramDumpDetails = classGrowthTrend.data.entries.asSequence()
                .filter(analysisFilter)
                .map { ClassHistogramDetails(
                        className = it.key.name,
                        analysis = it.value.analysisResult,
                        bytes = createBytesList(it.value.histoLines),
                        bytesDiff = createBytesDiffList(it.value.histoLines),
                        instances = createInstancesList(it.value.histoLines),
                        instancesDiff = createInstancesDiffList(it.value.histoLines)
                )  }
                .filter { largerThanBytesInLastHisto(it, reportConfig.byteLimit) }
                .toList()

        val heapHistogramDumpDetails = HeapHistogramDumpDetails(classHistogramDumpDetails, classGrowthTrend.timestamps)

        return HeapHistogramDumpReport(
                reportConfig = reportConfig,
                leakResult = leakResult,
                heapHistogramDumpSummary = heapHistogramDumpSummary,
                heapHistogramDumpDetails = heapHistogramDumpDetails)
    }

    private fun largerThanBytesInLastHisto(it: ClassHistogramDetails, minSizeInBytes: Long) =
            it.bytes.last() != null && it.bytes.last()!! > minSizeInBytes

    private fun createInstancesList(histoLines: List<HeapHistogramDumpLine>): List<Long?> {
        return histoLines.asSequence().map { it.instances }.toList()
    }

    private fun createInstancesDiffList(histoLines: List<HeapHistogramDumpLine>): List<Long?> {
        return listOf(0L) + histoLines.asSequence().map { it.instances }.zipWithNext().map { diffOrNull(it) }.toList()
    }

    private fun createBytesDiffList(histoLines: List<HeapHistogramDumpLine>): List<Long?> {
        return listOf(0L) + histoLines.asSequence().map { it.bytes }.zipWithNext().map { diffOrNull(it) }.toList()
    }

    private fun createBytesList(histoLines: List<HeapHistogramDumpLine>): List<Long?> {
        return histoLines.asSequence().map { it.bytes }.toList()
    }

    private fun diffOrNull(it: Pair<Long?, Long?>) =
            if (it.first == null || it.second == null) null else it.second!! - it.first!!

}