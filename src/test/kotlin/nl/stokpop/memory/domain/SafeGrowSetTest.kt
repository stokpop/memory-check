package nl.stokpop.memory.domain

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SafeGrowSetTest {
    @Test
    fun checkIsSafeGrowth() {
        val safeGrowSet = SafeGrowSet(setOf("java.lang.String", "java.time.*"))
        assertTrue(safeGrowSet.isSafeToGrow("java.lang.String"))
        assertTrue(safeGrowSet.isSafeToGrow("java.time.Date"))
        assertFalse(safeGrowSet.isSafeToGrow("java.timeDate"))
    }
}