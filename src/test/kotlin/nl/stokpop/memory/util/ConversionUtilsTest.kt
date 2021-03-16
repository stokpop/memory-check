/**
 * Copyright (C) 2021 Peter Paul Bakker, Stokpop Software Solutions
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
package nl.stokpop.memory.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

internal class ConversionUtilsTest {

    @Test
    fun `convert 1 to 1`() {
        assertEquals(1, ConversionUtils.convertHumanReadableToBytes(" 1"))
        assertEquals(1, ConversionUtils.convertHumanReadableToBytes(" 1b"))
        assertEquals(1024, ConversionUtils.convertHumanReadableToBytes(" 1024"))
        assertEquals(1024, ConversionUtils.convertHumanReadableToBytes(" 1024b"))
        assertEquals(0, ConversionUtils.convertHumanReadableToBytes(" 0.1 "))
        assertEquals(0, ConversionUtils.convertHumanReadableToBytes("0.9"))
    }

    @Test
    fun `convert 1k to 1024`() {
        assertEquals(1024, ConversionUtils.convertHumanReadableToBytes(" 1k"))
        assertEquals(2048, ConversionUtils.convertHumanReadableToBytes("2k  "))
        assertEquals(102, ConversionUtils.convertHumanReadableToBytes(" 0.1k "))
    }

    @Test
    fun `convert 1m to 1048576`() {
        assertEquals(1048576, ConversionUtils.convertHumanReadableToBytes(" 1m"))
        assertEquals(2097152, ConversionUtils.convertHumanReadableToBytes("2m  "))
        assertEquals(104857, ConversionUtils.convertHumanReadableToBytes(" 0.1m "))
    }

    @Test
    fun `convert 1pb to very large number`() {
        assertEquals(1_125_899_906_842_624, ConversionUtils.convertHumanReadableToBytes("1pb"))
        assertEquals(1_125_899_906_842_624, ConversionUtils.convertHumanReadableToBytes("1 pb"))
    }

    @Test
    fun `convert 1EB to very very large number`() {
        assertEquals(1_152_921_504_606_846_976L, ConversionUtils.convertHumanReadableToBytes("1 EB"))
    }

    @Test
    fun `convert k to error`() {
        assertThrows(ConversionUtils.NumberConversionException::class.java) {  ConversionUtils.convertHumanReadableToBytes("k") }
    }

    @Test
    fun `convert back and forth`() {
        val petabyte = ConversionUtils.convertHumanReadableToBytes("1.0 PB")
        assertEquals("1.0 PB", ConversionUtils.humanReadableMemorySize(petabyte))
    }

}