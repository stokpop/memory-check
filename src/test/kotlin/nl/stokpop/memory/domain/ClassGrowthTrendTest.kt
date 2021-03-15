package nl.stokpop.memory.domain

import nl.stokpop.memory.MemoryCheckException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ClassGrowthTrendTest {

    @Test
    fun expectExceptionSizesNotEqual() {
        val exception = assertThrows(MemoryCheckException::class.java) {
            val timestamps = listOf<Long>(0, 1, 2)
            val classInfo = ClassInfo("abc")
            val data = mapOf(classInfo to ClassGrowth(classInfo, listOf(), AnalysisResult.GROW_CRITICAL))
            ClassGrowthTrend(timestamps, data)
        }
        exception.message?.let { assertTrue(it.contains("abc")) }
    }
}