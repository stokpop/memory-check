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

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import nl.stokpop.memory.domain.AnalysisResult
import nl.stokpop.memory.domain.SafeList
import nl.stokpop.memory.domain.WatchList
import nl.stokpop.memory.report.*
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun main(args: Array<String>) = MemoryCheckCli().main(args)

class MemoryCheckCli : CliktCommand() {
    private val directory: String by option("-d", "--dir", help = "Look in this directory for heap histogram dumps.").default(".")
    private val extension: String by option("-e", "--ext", help = "Only process files with this extension, example: 'histo'").default("histo")
    private val identifier: String by option("-i", "--id", help = "Identifier for the report, example: 'test-run-1234'. Include #ts# for a timestamp.").default("anonymous-#ts#")
    private val reportDirectory: String by option("-r", "--report-dir", help = "Full or relative path to directory for the reports, example: '.' for current directory").default(".")
    private val classLimit: Int by option("-c", "--class-limit", help = "Report only the top 'limit' classes, example: '128'.").int().default(128)
    private val bytesLimit: Long by option("-b", "--bytes-limit", help = "Report class only when last dump has at least x bytes, example: '2048'").long().default(2048)
    private val settings: String by option("-s", "--settings", help = "Comma separated file with categories to report: grow_critical,grow_minor,grow_safe,shrink,unknown,stable. Default: 'grow_critical,grow_minor'").default("grow_critical,grow_minor")
    private val maxGrowthPercentage: Double by option("-mgp", "--max-growth-percentage", help = "Maximum allowed growth in percentage before reporting a critical growth. Default: 5.0").double().default(5.0)
    private val minGrowthPointsPercentage: Double by option("-mgpp", "--min-growth-points-percentage", help = "Minimum percentage of growth points to be considered growth. Default: 50.0").double().default(50.0)
    private val safeList: String by option("-sl", "--safe-list", help = "Comma separated list of fully qualified classnames that are 'safe to growth'. The asterisk (*) can be used as wildcard. Default: \"\"").default("")
    private val watchList: String by option("-wl", "--watch-list", help = "Comma separated list of fully qualified classnames that are 'always watched' irrelevant of other settings. The asterisk (*) can be used as wildcard. Default: \"\"").default("")
    private val safeListFile by option("-slf", "--safe-list-file", help = "The safe list file. Should contain one fully qualified classname per line.")
        .file(mustExist=true, canBeDir=false)
    private val watchListFile by option("-wlf", "--watch-list-file", help = "The safe list file. Should contain one fully qualified classname per line.")
        .file(mustExist=true, canBeDir=false)

    override fun run() {
        val reportDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))

        val timestampRegex = "#ts#".toRegex()
        val processedId = if (timestampRegex.containsMatchIn(identifier)) timestampRegex.replace(identifier, reportDateTime) else identifier

        val reportSettings = settings.split(',')
            .map { s -> s.toUpperCase() }
            .mapNotNull { s -> parseAnalysisResult(s) }
            .toSet()

        val safeGrowSetOption = safeList.split(",").filter { it.isNotEmpty() }.toSet()
        val watchSetOption = HashSet(watchList.split(",").filter { it.isNotEmpty() }.toSet())

        val safeGrowSetFile = filterClassnamesFile(safeListFile)
        val watchSetFile = filterClassnamesFile(watchListFile)

        val safeGrowSet = safeGrowSetOption + safeGrowSetFile
        val watchSet = watchSetOption + watchSetFile

        val reportLimits = ReportLimits(
            classLimit = classLimit,
            byteLimit = bytesLimit,
            maxGrowthPercentage = maxGrowthPercentage,
            minGrowthPointsPercentage = minGrowthPointsPercentage,
            safeList = safeGrowSet,
            watchList = watchSet,
            doReportGrowCritical = reportSettings.contains(AnalysisResult.GROW_CRITICAL),
            doReportGrowMinor = reportSettings.contains(AnalysisResult.GROW_MINOR),
            doReportGrowSafe = reportSettings.contains(AnalysisResult.GROW_SAFE),
            doReportGrowHickUps = reportSettings.contains(AnalysisResult.GROW_HICK_UPS),
            doReportShrinkAndGrow = reportSettings.contains(AnalysisResult.SHRINK_AND_GROW),
            doReportShrinks = reportSettings.contains(AnalysisResult.SHRINK),
            doReportStable  = reportSettings.contains(AnalysisResult.STABLE)
        )

        val reportConfig = ReportConfig(
                settings = reportSettings,
                histosDirectory = directory,
                reportDirectory = reportDirectory,
                extension = extension,
                identifier = processedId,
                reportDateTime = reportDateTime,
                reportLimits = reportLimits
        )

        try {
            MemoryCheck().processHistos(reportConfig)
        } catch (e : Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun filterClassnamesFile(classNamesFile: File?): Set<String> {
        return classNamesFile?.readLines()?.map { it.trim() }?.filter { it.isNotEmpty() }?.filter { !it.startsWith("#") }?.toHashSet() ?: emptySet()
    }

    /**
     * Returns null when an unknown analysis result is given. Prints message to std error.
     */
    private fun parseAnalysisResult(analysisResultAsString: String): AnalysisResult? {
        return try {
            AnalysisResult.valueOf(analysisResultAsString)
        } catch (e: java.lang.IllegalArgumentException) {
            System.err.println("Unknown analysis result in settings: $analysisResultAsString")
            return null
        }
    }

}

class MemoryCheck {

    companion object {
        fun useZoneOffset(): ZoneId {
            // better make this configurable
            return ZoneId.systemDefault()
        }
    }

    fun processHistos(reportConfig: ReportConfig) {

        val dir = File(reportConfig.histosDirectory)
        if (!dir.isDirectory) {
            throw MemoryCheckException("This is not a directory: ${dir.absoluteFile}")
        }
        val reportDir = File(reportConfig.reportDirectory)
        if (!reportDir.exists()) {
            println("Info: creating non-existent report directory: ${reportDir.absoluteFile}")
            reportDir.mkdirs()
        }
        if (!reportDir.canWrite()) {
            throw MemoryCheckException("Cannot write to report directory: ${reportDir.absoluteFile}")
        }

        val files = (dir.listFiles() ?: emptyArray()).asList()
                .filter { file -> file.extension == reportConfig.extension }
                .sorted()
                .toList()

        System.err.println("\nChecking files: ")
        files.forEach { System.err.println(it) }

        val reportLimits = reportConfig.reportLimits
        val readHistos = HistoReader.readHistos(files, SafeList(reportLimits.safeList), WatchList(reportLimits.watchList))

        val analysis = HistoAnalyser.analyse(readHistos, reportConfig)

        val reportData = ReportAnalyser.createHeapHistogramDumpReport(analysis, reportLimits)
        TextReport.report(readHistos, reportData, reportConfig)
        val jsonReportFile = JsonReport.report(reportData, reportConfig)
        val htmlReportFile = HtmlGraphCreator.writeHtmlGoogleGraphFile(reportData, reportConfig)
        println("json report: ${jsonReportFile.absoluteFile}")
        println("html report: ${htmlReportFile.absoluteFile}")
    }
}
