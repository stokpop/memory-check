package nl.stokpop.memory.report

import nl.stokpop.memory.domain.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ReportAnalyserTest {

    @Test
    fun `create Limited Report`() {

        val reportLimits = ReportLimits(
            classLimit = 0,
            byteLimit = 0,
            maxGrowthPercentage = 0.0,
            minGrowthPointsPercentage = 0.0,
            safeList = setOf(),
            watchList = setOf()
        )

        val heapHistogramDumpReport =
            ReportAnalyser.createHeapHistogramDumpReport(ClassGrowthTrend(listOf(), mapOf()), reportLimits)

        assertEquals(AnalysisResult.UNKNOWN, heapHistogramDumpReport.leakResult)

    }

    @Test
    fun `create Limited Report Count Without WatchList`() {

        val reportLimits = ReportLimits(
            classLimit = 1,
            byteLimit = 0,
            maxGrowthPercentage = 0.0,
            minGrowthPointsPercentage = 0.0,
            safeList = setOf(),
            watchList = setOf()
        )

        val classGrowthTrend = classGrowthTrendFixture(reportLimits)

        val heapHistogramDumpReport =
            ReportAnalyser.createHeapHistogramDumpReport(classGrowthTrend, reportLimits)

        assertEquals(AnalysisResult.GROW_CRITICAL, heapHistogramDumpReport.leakResult)
        assertEquals(1, heapHistogramDumpReport.heapHistogramDumpDetails.classHistogramDetails.size)
        assertEquals("abc", heapHistogramDumpReport.heapHistogramDumpDetails.classHistogramDetails[0].classInfo.name)
    }

    @Test
    fun `create Limited Report Count With WatchList`() {

        val reportLimits = ReportLimits(
            classLimit = 1,
            byteLimit = 0,
            maxGrowthPercentage = 0.0,
            minGrowthPointsPercentage = 0.0,
            safeList = setOf(),
            watchList = setOf("myClass")
        )

        val classGrowthTrend = classGrowthTrendFixture(reportLimits)

        val heapHistogramDumpReport =
            ReportAnalyser.createHeapHistogramDumpReport(classGrowthTrend, reportLimits)

        assertEquals(AnalysisResult.GROW_CRITICAL, heapHistogramDumpReport.leakResult)
        assertEquals(2, heapHistogramDumpReport.heapHistogramDumpDetails.classHistogramDetails.size)
        assertEquals("abc", heapHistogramDumpReport.heapHistogramDumpDetails.classHistogramDetails[0].classInfo.name)
        assertEquals("myClass", heapHistogramDumpReport.heapHistogramDumpDetails.classHistogramDetails[1].classInfo.name)
    }

    private fun classGrowthTrendFixture(reportLimits: ReportLimits): ClassGrowthTrend {
        val timestamps = listOf<Long>(1234)

        val classInfo1 = ClassInfo("abc")
        val classGrowth1 = ClassGrowth(
            classInfo1,
            listOf(HeapHistogramDumpLine(classInfo1, 1, 10, 1024)),
            AnalysisResult.GROW_CRITICAL
        )
        val classInfo2 = ClassInfo("xyz")
        val classGrowth2 = ClassGrowth(
            classInfo2,
            listOf(HeapHistogramDumpLine(classInfo2, 2, 9, 999)),
            AnalysisResult.GROW_MINOR
        )
        val classInfo3 = ClassInfo("myClass", isOnWatchList =  reportLimits.watchList.contains("myClass"))
        val classGrowth3 = ClassGrowth(
            classInfo3,
            listOf(HeapHistogramDumpLine(classInfo3, 3, 8, 888)),
            AnalysisResult.GROW_MINOR
        )

        val data = mapOf(classInfo1 to classGrowth1, classInfo2 to classGrowth2, classInfo3 to classGrowth3)

        return ClassGrowthTrend(timestamps, data)
    }
}