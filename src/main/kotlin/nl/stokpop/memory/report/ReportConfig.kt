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

import kotlinx.serialization.Serializable
import nl.stokpop.memory.domain.AnalysisResult
import nl.stokpop.memory.domain.AnalysisResult.*

@Serializable
class ReportConfig(
        val settings: Set<AnalysisResult>,
        val histosDirectory: String,
        val reportDirectory: String,
        val extension: String,
        val identifier: String,
        val reportDateTime: String,
        val classLimit: Int,
        val byteLimit: Long,
        val maxGrowthPercentage: Int,
        val safeGrowSet: Set<String>) {

    val doReportGrowCritical = settings.contains(GROW_CRITICAL)
    val doReportGrowMinor = settings.contains(GROW_MINOR)
    val doReportGrowSafe = settings.contains(GROW_SAFE)
    val doReportUnknowns = settings.contains(UNKNOWN)
    val doReportShrinks = settings.contains(SHRINK)
    val doReportStable  = settings.contains(STABLE)

}