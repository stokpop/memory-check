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

    fun createHeapHistogramDumpReport(classGrowthTrend: ClassGrowthTrend, reportLimits: ReportLimits) : HeapHistogramDumpReport {

        val analysisResultToCount = AnalysisResult.values().asSequence()
                .map { it to classGrowthTrend.statusCount(it, reportLimits.byteLimit) }
                .toMap()

        val heapHistogramDumpSummary = HeapHistogramDumpSummary(analysisResultToCount)

        val leakResult = when {
            (heapHistogramDumpSummary.data[AnalysisResult.GROW_CRITICAL] ?: 0) > 0 -> AnalysisResult.GROW_CRITICAL
            (heapHistogramDumpSummary.data[AnalysisResult.GROW_MINOR] ?: 0) > 0 -> AnalysisResult.GROW_MINOR
            (heapHistogramDumpSummary.data[AnalysisResult.GROW_SAFE] ?: 0) > 0 -> AnalysisResult.GROW_SAFE
            (heapHistogramDumpSummary.data[AnalysisResult.SHRINK] ?: 0) > 0 -> AnalysisResult.SHRINK
            (heapHistogramDumpSummary.data[AnalysisResult.STABLE] ?: 0) > 0 -> AnalysisResult.STABLE
            else -> AnalysisResult.UNKNOWN
        }

        val analysisFilter: (Map.Entry<ClassInfo, ClassGrowth>) -> Boolean = {
            (reportLimits.doReportGrowCritical && it.value.analysisResult == AnalysisResult.GROW_CRITICAL)
                    || (reportLimits.doReportGrowMinor && it.value.analysisResult == AnalysisResult.GROW_MINOR)
                    || (reportLimits.doReportGrowSafe && it.value.analysisResult == AnalysisResult.GROW_SAFE)
                    || (reportLimits.doReportShrinks && it.value.analysisResult == AnalysisResult.SHRINK)
                    || (reportLimits.doReportStable && it.value.analysisResult == AnalysisResult.STABLE)
                    || (reportLimits.doReportUnknowns && it.value.analysisResult == AnalysisResult.UNKNOWN)}

        // split on isOnWatchList and skip filters for classes on watch list
        // note that the class limit is only applied to non watch list members, so actual list
        // of classes in the report can be bigger
        val classHistogramDetailsByWatchList = classGrowthTrend.data.entries.groupBy { it.key.isOnWatchList }

        val detailsNonWatchList = classHistogramDetailsByWatchList.getOrDefault(false, emptyList())
        val detailsWatchList = classHistogramDetailsByWatchList.getOrDefault(true, emptyList())

        val classHistogramDumpDetailsNonWatchList = detailsNonWatchList.asSequence()
                .filter(analysisFilter)
                .filter { isLargerThanBytesInLastHisto(it.value, reportLimits.byteLimit) }
                .take(reportLimits.classLimit)
                .map { ClassHistogramDetails(
                        classInfo = it.key,
                        analysis = it.value.analysisResult,
                        bytes = createBytesList(it.value.histoLines),
                        bytesDiff = createBytesDiffList(it.value.histoLines),
                        instances = createInstancesList(it.value.histoLines),
                        instancesDiff = createInstancesDiffList(it.value.histoLines)
                )  }
                .toList()

        val classHistogramDumpDetailsWatchList = detailsWatchList.asSequence()
                .filter(analysisFilter)
                .map { ClassHistogramDetails(
                        classInfo = it.key,
                        analysis = it.value.analysisResult,
                        bytes = createBytesList(it.value.histoLines),
                        bytesDiff = createBytesDiffList(it.value.histoLines),
                        instances = createInstancesList(it.value.histoLines),
                        instancesDiff = createInstancesDiffList(it.value.histoLines)
                )  }
                .toList()

        val totalDetailsToReport = (classHistogramDumpDetailsNonWatchList + classHistogramDumpDetailsWatchList).sortedByDescending { it.bytes.last() }

        val heapHistogramDumpDetails = HeapHistogramDumpDetails(totalDetailsToReport, classGrowthTrend.timestamps)

        return HeapHistogramDumpReport(
                reportLimits = reportLimits,
                leakResult = leakResult,
                heapHistogramDumpSummary = heapHistogramDumpSummary,
                heapHistogramDumpDetails = heapHistogramDumpDetails)
    }

    private fun isLargerThanBytesInLastHisto(it: ClassGrowth, minSizeInBytes: Long): Boolean {
        val bytes = it.histoLines.last().bytes
        return bytes != null && bytes > minSizeInBytes
    }

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