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
import nl.stokpop.memory.domain.ClassName
import nl.stokpop.memory.domain.HeapHistogramDumpLine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class HistoAnalyserTest {

    @Test
    fun analyseGrowthNoElement() {
        val histoLines: List<HeapHistogramDumpLine> = emptyList()
        assertEquals(AnalysisResult.UNKNOWN, HistoAnalyser.analyseGrowth(histoLines))
    }

    @Test
    fun analyseGrowthOneElement() {
        val histoLines = listOf(HeapHistogramDumpLine(ClassName("abc"), 1, 1, 1))
        assertEquals(AnalysisResult.UNKNOWN, HistoAnalyser.analyseGrowth(histoLines))
    }

    @Test
    fun analyseGrowthTwoElementGrow() {
        val histoLines = listOf(
                HeapHistogramDumpLine(ClassName("abc"), 1, 1, 1),
                HeapHistogramDumpLine(ClassName("abc"), 1, 2, 2)
        )
        assertEquals(AnalysisResult.GROW, HistoAnalyser.analyseGrowth(histoLines))
    }

    @Test
    fun analyseGrowthTwoElementStable() {
        val histoLines = listOf(
                HeapHistogramDumpLine(ClassName("abc"), 1, 1, 1),
                HeapHistogramDumpLine(ClassName("abc"), 1, 1, 1)
        )
        assertEquals(AnalysisResult.STABLE, HistoAnalyser.analyseGrowth(histoLines))
    }

    @Test
    fun analyseGrowthTwoElementsShrink() {
        val histoLines = listOf(
                HeapHistogramDumpLine(ClassName("abc"), 1, 2, 1),
                HeapHistogramDumpLine(ClassName("abc"), 1, 1, 1)
        )
        assertEquals(AnalysisResult.SHRINK, HistoAnalyser.analyseGrowth(histoLines))
    }

    @Test
    fun analyseGrowthThreeElementsUnknown() {
        val histoLines = listOf(
                HeapHistogramDumpLine(ClassName("abc"), 1, 1, 1),
                HeapHistogramDumpLine(ClassName("abc"), 1, 2, 1),
                HeapHistogramDumpLine(ClassName("abc"), 1, 1, 1)
        )
        assertEquals(AnalysisResult.UNKNOWN, HistoAnalyser.analyseGrowth(histoLines))
    }

    @Test
    fun analyseGrowthTwoElementsOneGhost() {
        val histoLines = listOf(
                HeapHistogramDumpLine(ClassName("abc"), 1, 1, 1),
                HeapHistogramDumpLine.createGhostLine(ClassName("abc"))
        )
        assertEquals(AnalysisResult.UNKNOWN, HistoAnalyser.analyseGrowth(histoLines))
    }

    @Test
    fun analyseGrowthThreeElementsOneGhostInMiddle() {
        // should this be UNKNOWN? seems that there are no classes in middle histo, so
        // no leak?
        val histoLines = listOf(
                HeapHistogramDumpLine(ClassName("abc"), 1, 1, 1),
                HeapHistogramDumpLine.createGhostLine(ClassName("abc")),
                HeapHistogramDumpLine(ClassName("abc"), 1, 2, 1)
        )
        assertEquals(AnalysisResult.GROW, HistoAnalyser.analyseGrowth(histoLines))
    }

    @Test
    fun analyseGrowthThreeElementsOneGhostInAtEnd() {
        // should this test fail? if we interpret that class in totally not there at last step: no leak?
        val histoLines = listOf(
                HeapHistogramDumpLine(ClassName("abc"), 1, 1, 1),
                HeapHistogramDumpLine(ClassName("abc"), 1, 2, 1),
                HeapHistogramDumpLine.createGhostLine(ClassName("abc"))
        )
        assertEquals(AnalysisResult.UNKNOWN, HistoAnalyser.analyseGrowth(histoLines))
    }

}