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

import nl.stokpop.memory.domain.*
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.time.LocalDateTime

object HistoReader {

    private val isoDateRegex = """\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?""".toRegex()
    private val spacesRegex = "\\s+".toRegex()

    fun readHistos(histoFiles: List<File>, safeGrowList: SafeList, watchList: WatchList): List<HeapHistogramDump> {
        return histoFiles
            .map {
                val dumpDate = dateForHistoFile(it)
                HeapHistogramDump(it, dumpDate, readHisto(it, safeGrowList, watchList)) }
            .toList()
    }

    /**
     * First check the filename to contain a ISO date and uses that as a timestamp.
     * Filename should contain an ISO formatted date like: 2020-06-17T22:25:38.960921
     * If not found, uses file creation date or file last modified as last resort.
     */
    private fun dateForHistoFile(file: File): LocalDateTime {
        return extractDate(file.name) ?: dateForHistoFileFromAttrinutes(file)
    }

    private fun dateForHistoFileFromAttrinutes(file: File): LocalDateTime {
        println("Unable to parse date time from filename: ${file.name}, will now use file creation date.")
        return try {
            val attr =
                    Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
            val fileTime = attr.creationTime()
            LocalDateTime.ofInstant(fileTime.toInstant(), MemoryCheck.useZoneOffset())
        } catch (ex: IOException) {
            System.err.println("Unable to get creation date for $file, using last modified as work around.")
            LocalDateTime.from(Instant.ofEpochMilli(file.lastModified()))
        }
    }

    private fun readHisto(file: File, safeList: SafeList, watchList: WatchList) : List<HeapHistogramDumpLine> {
        return file.useLines { line ->
            line.map { it.trim() }
                    .filter { it.contains(':') } // very basic check for lines to parse...
                    .map { createHistoLine(it, safeList, watchList) }
                    .toList()
        }
    }

    private fun createHistoLine(line: String, safeList: SafeList, watchList: WatchList): HeapHistogramDumpLine {
        val split = line.split(spacesRegex)
        // 5 elements can be found in java 9+ dumps with packages, example:
        //    4:         88508        2124192  java.lang.String (java.base@11.0.6)
        // the module is ignored
        if (!(split.size == 4 || split.size == 5)) {
            throw InvalidHistoLineException("Cannot read histo line (${split.size} elements, 4 or 5 expected): '$line' and '$split'")
        }
        val lineNumber = skipLastCharacter(split[0])
        val instances = split[1]
        val bytes = split[2]
        val name = split[3]
        return HeapHistogramDumpLine(classInfo = ClassInfo(name, safeList.matches(name), watchList.matches(name)), lineNumber = lineNumber.toLong(), instances = instances.toLong(), bytes = bytes.toLong())
    }

    private fun skipLastCharacter(s: String) = s.substring(0, s.length - 1)

    fun extractDate(s: String): LocalDateTime? {
        if (isoDateRegex.containsMatchIn(s)) {
            val dateString = isoDateRegex.find(s)?.value
            return LocalDateTime.parse(dateString)
        }
        return null
    }
}