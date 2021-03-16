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

import java.util.*

object ConversionUtils {

    /**
     * Convert 1k or 1kb to 1024 bytes, etc. up to 1e or 1pe for exabytes.
     * Ignores lower or uppercase.
     */
    fun convertHumanReadableToBytes(input: String) : Long {
        val trimmed = input.trim()
        val numbers = trimmed.takeWhile { Character.isDigit(it) || it == '.'}
        val whitespace = trimmed.substring(numbers.length).takeWhile { Character.isSpaceChar(it) }
        val letters = trimmed.substring(numbers.length + whitespace.length).takeWhile { Character.isLetter(it) }.toLowerCase()

        if (numbers.isEmpty()) throw NumberConversionException("No numbers found in '$trimmed'")
        val value = numbers.toDouble()

        return when (letters) {
            "", "b" -> value.toLong()
            "k", "kb" -> (value * (1L shl 10)).toLong()
            "m", "mb" -> (value * (1L shl 20)).toLong()
            "g", "gb" -> (value * (1L shl 30)).toLong()
            "t", "tb" -> (value * (1L shl 40)).toLong()
            "p", "pb" -> (value * (1L shl 50)).toLong()
            "e", "eb" -> (value * (1L shl 60)).toLong()
            else -> throw NumberConversionException("Unknown letters to convert to bytes: $letters (from '$trimmed')")
        }

    }

    /**
     * inspired by: https://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java#answer-24805871
     */
    fun humanReadableMemorySize(v: Long): String {
        if (v < 1024) return "$v B"
        val z = (63 - java.lang.Long.numberOfLeadingZeros(v)) / 10
        // locale ENGLISH to force . instead of ,
        return String.format(Locale.ENGLISH, "%.1f %sB", v.toDouble() / (1L shl z * 10), " KMGTPE"[z])
    }

    class NumberConversionException(msg: String) : Exception(msg)
}