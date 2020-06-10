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

import java.io.File
import java.lang.System.exit
import java.time.ZoneId

fun main(args: Array<String>) {

    if (args.size != 2) {
        println("please provide a directory and file extension for histograms to read")
        exit(1)
    }

    val directory = args[0]
    val extension = args[1]

    try {
        MemoryCheck().processHistos(directory, extension)
    } catch (e : Exception) {
        println("Error: ${e.message}")
    }

}

class MemoryCheck {

    companion object {
        fun useZoneOffset(): ZoneId {
            // better make this configurable
            return ZoneId.of("Europe/Amsterdam")
        }
    }

    fun processHistos(directory: String, extension: String) {
        val dir = File(directory)
        if (!dir.isDirectory) {
            throw MemoryCheckException("This is not a directory: $directory")
        }

        val files = (dir.listFiles() ?: emptyArray()).asList()
                .filter { file -> file.extension.equals(extension) }
                .filter { file -> file.name.contains("histo") }
                .sorted()
                .toList()

        println("\nChecking files: ")
        files.forEach { println(it) }

        val readHistos = HistoReader().readHistos(files)

        val analysis = HistoAnalyser().analyse(readHistos)

        TextReport().report(readHistos, analysis)

    }
}
