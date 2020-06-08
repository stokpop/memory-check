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

class TextReport {
    fun report(analysis: ClassGrowthTrend) {

        println("\nNumber of GROW ${analysis.statusCount(AnalysisResult.GROW)}")
        println("Number of STABLE ${analysis.statusCount(AnalysisResult.STABLE)}")
        println("Number of SHRINK ${analysis.statusCount(AnalysisResult.SHRINK)}")
        println("Number of UNKNOWN ${analysis.statusCount(AnalysisResult.UNKNOWN)}")

        println("\nFound possible memory leaks:")
        analysis.statusFilter(AnalysisResult.GROW).forEach { reportLine(it) }

        println("\nFound shrinks:")
        analysis.statusFilter(AnalysisResult.SHRINK).forEach { reportLine(it) }

        println("\nFound unknowns:")
        analysis.statusFilter(AnalysisResult.UNKNOWN).forEach { reportLine(it) }

    }

    fun reportLine(line: ClassGrowth) {

        val instances = line.histoLines.map { it.instances }.map { if (it == -1L) "-" else it.toString() }.toList()
        val bytes = line.histoLines.map { it.bytes }.map { if (it == -1L) "-" else HumanReadable.humanReadableMemorySize(it) }.toList()

        println("${line.className.name} instances: ${instances} size: ${bytes}")
    }
}