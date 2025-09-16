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
package nl.stokpop.memory.domain.json

import kotlinx.serialization.Serializable
import nl.stokpop.memory.domain.AnalysisResult
import nl.stokpop.memory.domain.ClassInfo
import nl.stokpop.memory.report.ReportLimits

@Serializable
data class HeapHistogramDumpReport(
    val reportLimits: ReportLimits,
    val leakResult: AnalysisResult,
    val heapHistogramDumpSummary: HeapHistogramDumpSummary,
    val heapHistogramDumpDetails: HeapHistogramDumpDetails,
    val usedFiles: UsedFilesReport? = null
)

@Serializable
data class HeapHistogramDumpDetails (
        val classHistogramDetails: List<ClassHistogramDetails>,
        val timestamps: List<Long?>
)

@Serializable
class ClassHistogramDetails(
    val analysis: AnalysisResult,
    val classInfo: ClassInfo,
    val bytes: List<Long?>,
    val bytesDiff: List<Long?>,
    val instances: List<Long?>,
    val instancesDiff: List<Long?>
)

@Serializable
data class HeapHistogramDumpSummary (
        val data: Map<AnalysisResult, Int>
)

@Serializable
data class UsedFilesReport(
    val histogramFiles: List<String>,
    val safeListFile: String? = null,
    val watchListFile: String? = null
)
