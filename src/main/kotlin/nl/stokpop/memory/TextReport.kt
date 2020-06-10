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

import nl.stokpop.memory.domain.AnalysisResult
import nl.stokpop.memory.domain.ClassGrowth
import nl.stokpop.memory.domain.ClassGrowthTrend
import nl.stokpop.memory.domain.HistoInfo
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TextReport {

    fun report(histos: List<HistoInfo>, analysis: ClassGrowthTrend) {

        val minSizeInBytes = 1024L

        val header = "Histogram report - ${LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)}"
        val dashes = generateSequence { "-" }.take(header.length).joinToString(separator = "") { it }
        println(dashes)
        println(header)
        println(dashes)

        println()

        histos.forEach { println("File: '${it.histoFile.name}' with date ${it.timestamp.format(DateTimeFormatter.ISO_DATE_TIME)}") }

        val doReportUnknowns = false
        val doReportShrinks = false

        println("\nNumber of GROW ${analysis.statusCount(AnalysisResult.GROW)}")
        println("Number of STABLE ${analysis.statusCount(AnalysisResult.STABLE)}")
        println("Number of SHRINK ${analysis.statusCount(AnalysisResult.SHRINK)}")
        println("Number of UNKNOWN ${analysis.statusCount(AnalysisResult.UNKNOWN)}")

        println("\nBelow only results are printed that have remaining size of at least ${HumanReadable.humanReadableMemorySize(minSizeInBytes)} in last histogram.")

        println("\nFound possible memory leaks:")

        val bytesFilter: (ClassGrowth) -> Boolean = { largerThanBytesInLastHisto(it, minSizeInBytes) }

        analysis.statusFilter(AnalysisResult.GROW).filter(bytesFilter).forEach { reportLine(it) }

        if (doReportShrinks) {
            println("\nFound shrinks:")
            analysis.statusFilter(AnalysisResult.SHRINK).filter(bytesFilter).forEach { reportLine(it) }
        }

        if (doReportUnknowns) {
            println("\nFound unknowns:")
            analysis.statusFilter(AnalysisResult.UNKNOWN).filter(bytesFilter).forEach { reportLine(it) }
        }
    }

    private fun largerThanBytesInLastHisto(it: ClassGrowth, minSizeInBytes: Long) =
            it.histoLines.last().bytes != null && it.histoLines.last().bytes!! > minSizeInBytes

    fun reportLine(line: ClassGrowth) {

        val charForNull = '_'

        val instances = line.histoLines
                .map { it.instances }
                .map { it?.toString() ?: charForNull }

        val instancesDiffs = line.histoLines
                .asSequence()
                .map { it.instances }
                .zipWithNext()
                .map { diffOrNull(it) }
                .map { it?.toString() ?: charForNull }
                .toList()

        val bytes = line.histoLines
                .asSequence()
                .map { it.bytes }
                .map { if (it == null) charForNull else HumanReadable.humanReadableMemorySize(it) }

        val bytesDiff = line.histoLines
                .asSequence()
                .map { it.bytes }
                .zipWithNext()
                .map { diffOrNull(it) }
                .map { if (it == null) charForNull else HumanReadable.humanReadableMemorySize(it) }

        println("${line.className.name} instances: $instances diff: $instancesDiffs size: $bytes diff: $bytesDiff")
    }

    private fun diffOrNull(it: Pair<Long?, Long?>) =
            if (it.first == null || it.second == null) null else it.second!! - it.first!!

}