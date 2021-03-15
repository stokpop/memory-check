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

import nl.stokpop.memory.HumanReadable
import nl.stokpop.memory.domain.AnalysisResult.*
import nl.stokpop.memory.domain.HeapHistogramDump
import nl.stokpop.memory.domain.json.ClassHistogramDetails
import nl.stokpop.memory.domain.json.HeapHistogramDumpReport
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

        println("\nNumber of GROW_CRITICAL ${data.heapHistogramDumpSummary.data[GROW_CRITICAL]}")
        println("Number of GROW_MINOR ${data.heapHistogramDumpSummary.data[GROW_MINOR]}")
        println("Number of GROW_SAFE ${data.heapHistogramDumpSummary.data[GROW_SAFE]}")
        println("Number of STABLE ${data.heapHistogramDumpSummary.data[STABLE]}")
        println("Number of SHRINK ${data.heapHistogramDumpSummary.data[SHRINK]}")
        println("Number of UNKNOWN ${data.heapHistogramDumpSummary.data[UNKNOWN]}")

        println("\nBelow only results are printed that have remaining size of at least ${HumanReadable.humanReadableMemorySize(minSizeInBytes)} in last histogram.")

        val details = data.heapHistogramDumpDetails.classHistogramDetails.asSequence()

        if (data.reportLimits.doReportGrowCritical) {
            println("\n\nFound possible critical memory leaks:")
            details.filter { it.analysis == GROW_CRITICAL }
                    .forEach { reportLine(it) }
        }

        if (data.reportLimits.doReportGrowMinor) {
            println("\n\nFound possible minor memory leaks:")
            details.filter { it.analysis == GROW_MINOR }
                .forEach { reportLine(it) }
        }

        if (data.reportLimits.doReportGrowSafe) {
            println("\n\nFound possible safe memory leaks:")
            details.filter { it.analysis == GROW_SAFE }
                .forEach { reportLine(it) }
        }

        if (data.reportLimits.doReportShrinks) {
            println("\n\nFound shrinks:")
            details.filter { it.analysis == SHRINK }
                    .forEach { reportLine(it) }
        }

        if (data.reportLimits.doReportUnknowns) {
            println("\n\nFound unknowns:")
            details.filter { it.analysis == UNKNOWN }
                    .forEach { reportLine(it) }
        }

        if (data.reportLimits.doReportStable) {
            println("\n\nFound stable:")
            details.filter { it.analysis == STABLE }
                    .forEach { reportLine(it) }
        }

        println("\n")
    }

    fun reportLine(details: ClassHistogramDetails) {

//        val charForNull = "_"
//
//        val instances = details.instances.asSequence()
//                .map { it?.toString() ?: charForNull }
//                .toList()
//
//        val instancesDiffs = details.instancesDiff.asSequence()
//                .map { it?.toString() ?: charForNull }
//                .toList()
//
//        val bytes = details.bytes.asSequence()
//                .map { if (it == null) charForNull else HumanReadable.humanReadableMemorySize(it) }
//                .toList()
//
//        val bytesDiff = details.bytesDiff.asSequence()
//                .asSequence()
//                .map { if (it == null) charForNull else HumanReadable.humanReadableMemorySize(it) }
//                .toList()

        print("${details.classInfo.name} ")
    }

}