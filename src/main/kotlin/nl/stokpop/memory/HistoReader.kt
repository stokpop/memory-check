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

import nl.stokpop.memory.domain.ClassName
import nl.stokpop.memory.domain.HistoInfo
import nl.stokpop.memory.domain.HistoLine
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.time.LocalDateTime
import kotlin.streams.asStream
import kotlin.streams.toList

class HistoReader {

    fun readHistos(histoFiles: List<File>): List<HistoInfo> {
        return histoFiles
            .map { HistoInfo(it, dateForHistoFile(it), readHisto(it)) }
            .toList()
    }

    /**
     * Now uses file creation date. We might also parse filename for a date.
     */
    private fun dateForHistoFile(file: File): LocalDateTime {
        try {
            val attr =
                Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
            val fileTime = attr.creationTime()
            return LocalDateTime.ofInstant(fileTime.toInstant(), MemoryCheck.useZoneOffset())
        } catch (ex: IOException) {
            System.err.println("Unable to get creation date for $file, using last modified as work around.");
            return LocalDateTime.from(Instant.ofEpochMilli(file.lastModified()))
        }
    }

    fun readHisto(file: File) : List<HistoLine> {
        return file.useLines {
            it.asStream()
                .map { it.trim() }
                .filter { it.contains(':') }
                .map { createHistoLine(it) }
                .toList()
        }
    }

    private fun createHistoLine(line: String): HistoLine {
        val split = line.split("\\s+".toRegex())
        if (split.size != 4) {
            throw InvalidHistoLineException("Cannot read histo line (${split.size} elements, 4 expected): '$line' and '$split'");
        }
        val num = skipLastCharacter(split[0])
        val instances = split[1]
        val bytes = split[2]
        val name = split[3]
        return HistoLine(ClassName(name), num.toLong(), instances.toLong(), bytes.toLong())
    }

    private fun skipLastCharacter(s: String) = s.substring(0, s.length - 1)
}