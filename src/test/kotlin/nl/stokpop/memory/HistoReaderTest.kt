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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class HistoReaderTest {

    @Test
    fun extractDateLegacy() {
        // used to have : in time for ISO standard 8601
        val dateTime = HistoReader.extractDate("lsdkfjgldfk 2020-06-17T22:25:38.960921 ldkfgldjkfs")
        assertNotNull(dateTime)
        assertEquals(2020, dateTime?.year)
        assertEquals(22, dateTime?.hour)
        assertEquals(38, dateTime?.second)
        assertEquals(960921000, dateTime?.nano)
    }

    @Test
    fun extractDateUnderscores() {
        // seems : were replaced by _ in filenames when going from linux to windows
        val dateTime = HistoReader.extractDate("lsdkfjgldfk 2020-06-17T22_25_38.960921 ldkfgldjkfs")
        assertNotNull(dateTime)
        assertEquals(2020, dateTime?.year)
        assertEquals(22, dateTime?.hour)
        assertEquals(38, dateTime?.second)
        assertEquals(960921000, dateTime?.nano)
    }

    @Test
    fun extractDateWithoutNanos() {
        val dateTime = HistoReader.extractDate("lsdkfjgldfk 2020-06-17T22-25-38 ldkfgldjkfs")
        assertNotNull(dateTime)
        assertEquals(2020, dateTime?.year)
        assertEquals(22, dateTime?.hour)
        assertEquals(38, dateTime?.second)
        assertEquals(0, dateTime?.nano)
    }
}