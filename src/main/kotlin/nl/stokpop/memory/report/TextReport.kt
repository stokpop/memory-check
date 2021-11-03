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

import nl.stokpop.memory.domain.AnalysisResult.*
import nl.stokpop.memory.domain.ClassInfo
import nl.stokpop.memory.domain.HeapHistogramDump
import nl.stokpop.memory.domain.json.ClassHistogramDetails
import nl.stokpop.memory.domain.json.HeapHistogramDumpReport
import nl.stokpop.memory.util.ConversionUtils
import java.time.format.DateTimeFormatter

object TextReport {

    fun report(histos: List<HeapHistogramDump>, data: HeapHistogramDumpReport, reportConfig: ReportConfig) {

        val minSizeInBytes = data.reportLimits.byteLimit

        val header = "Histogram report - ${reportConfig.reportDateTime}"
        val dashes = generateSequence { "-" }.take(header.length).joinToString(separator = "") { it }
        println(dashes)
        println(header)
        println(dashes)

        println()

        histos.forEach { println("File: '${it.histoFile.name}' with date ${it.timestamp.format(DateTimeFormatter.ISO_DATE_TIME)}") }

        println("\nBelow only results are printed that have remaining size of at least ${ConversionUtils.humanReadableMemorySize(minSizeInBytes)} in last histogram\n" +
                "or are on the watch list.")

        println("\nNumber of GROW_CRITICAL ${data.heapHistogramDumpSummary.data[GROW_CRITICAL]}")
        println("Number of GROW_MINOR ${data.heapHistogramDumpSummary.data[GROW_MINOR]}")
        println("Number of GROW_SAFE ${data.heapHistogramDumpSummary.data[GROW_SAFE]}")
        println("Number of GROW_HICK_UPS ${data.heapHistogramDumpSummary.data[GROW_HICK_UPS]}")
        println("Number of SHRINK_AND_GROW ${data.heapHistogramDumpSummary.data[SHRINK_AND_GROW]}")
        println("Number of STABLE ${data.heapHistogramDumpSummary.data[STABLE]}")
        println("Number of SHRINK ${data.heapHistogramDumpSummary.data[SHRINK]}")
        println("Number of SINGLE ${data.heapHistogramDumpSummary.data[SINGLE]}")
        println("Number of UNKNOWN ${data.heapHistogramDumpSummary.data[UNKNOWN]}")

        val details = data.heapHistogramDumpDetails.classHistogramDetails.asSequence()

        if (data.reportLimits.doReportGrowCritical) {
            println("\n\nFound critical memory leaks:")
            details.filter { it.analysis == GROW_CRITICAL }
                    .forEach { reportLine(it) }
        }

        if (data.reportLimits.doReportGrowMinor) {
            println("\n\nFound minor memory leaks:")
            details.filter { it.analysis == GROW_MINOR }
                .forEach { reportLine(it) }
        }

        if (data.reportLimits.doReportGrowSafe) {
            println("\n\nFound 'safe' memory leaks:")
            details.filter { it.analysis == GROW_SAFE }
                .forEach { reportLine(it) }
        }

        if (data.reportLimits.doReportGrowHickUps) {
            println("\n\nFound grow hick ups:")
            details.filter { it.analysis == GROW_HICK_UPS }
                .forEach { reportLine(it) }
        }

        if (data.reportLimits.doReportShrinkAndGrow) {
            println("\n\nFound shrink and grow:")
            details.filter { it.analysis == SHRINK_AND_GROW }
                .forEach { reportLine(it) }
        }

        if (data.reportLimits.doReportShrinks) {
            println("\n\nFound shrink:")
            details.filter { it.analysis == SHRINK }
                    .forEach { reportLine(it) }
        }

        if (data.reportLimits.doReportStable) {
            println("\n\nFound stable:")
            details.filter { it.analysis == STABLE }
                    .forEach { reportLine(it) }
        }

        if (data.reportLimits.doReportSingle) {
            println("\n\nFound singles:")
            details.filter { it.analysis == SINGLE }
                    .forEach { reportLine(it) }
        }

        println("\n")
    }

    private fun reportLine(details: ClassHistogramDetails) {
        val classInfo = details.classInfo
        print("${ClassInfo.prefixWatchListAndSafeList(classInfo)}${classInfo.name} ")
    }

}