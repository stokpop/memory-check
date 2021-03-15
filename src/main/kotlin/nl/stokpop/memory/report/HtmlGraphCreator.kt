/*
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
package nl.stokpop.memory.report

import nl.stokpop.memory.HumanReadable
import nl.stokpop.memory.domain.AnalysisResult
import nl.stokpop.memory.domain.json.ClassHistogramDetails
import nl.stokpop.memory.domain.json.HeapHistogramDumpReport
import java.io.File

/**
 * Create graphs with google graphs: https://google-developers.appspot.com/chart/
 */
object HtmlGraphCreator {

    private var arrowUpCritical = '\u25b2'
    private var arrowUpMinor = '\u21e7'
    private var arrowUpSafe = '\u2191'
    private var arrowUpDown = '\u2195'
    private var arrowDown = '\u2193'
    private var arrowFlat = '\u2194'
    private var nonBreakSpace = '\u00a0'

    private const val CHART_HTML_TEMPLATE = """
<html>
  <head>
    <meta charset="UTF-8">

    <link rel="stylesheet" href="https://fonts.googleapis.com/css?family=Roboto:300,300italic,700,700italic">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/normalize/8.0.1/normalize.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/milligram/1.4.0/milligram.css">    

    <script type="text/javascript" src="https://www.google.com/jsapi"></script>
    <script type="text/javascript">
      google.load("visualization", "1", {packages:["corechart"]});
      google.setOnLoadCallback(drawChart);
      function drawChart() {
        var date_formatter = new google.visualization.DateFormat({ pattern: "HH:mm:ss.SSS" });
        
        // CHART BYTES
        
        var dataBytes = google.visualization.arrayToDataTable([
          ###TABLE_BYTES###
        ]);
        date_formatter.format(dataBytes, 0);

        var optionsBytes = {
          title: '###TITLE_BYTES###',
          hAxis: {title: '###H_AXIS_TITLE_BYTES###',  titleTextStyle: {color: '#333'}, format: 'dd-MM HH:mm', minorGridlines: {count: 10, format: 'mm'} },
          vAxis: {title: '###V_AXIS_TITLE_BYTES###', scaleType: 'log', viewWindow: {} },
          chartArea: { left:'10%', width:'60%' }
        };

        var chartBytes = new google.visualization.LineChart(document.getElementById('chart_div_bytes'));
        chartBytes.draw(dataBytes, optionsBytes);

        // CHART BYTES DIFF
        
        var dataBytesDiff = google.visualization.arrayToDataTable([
          ###TABLE_BYTES_DIFF###
        ]);
        date_formatter.format(dataBytesDiff, 0);
        
        var optionsBytesDiff = {
          title: '###TITLE_BYTES_DIFF###',
          hAxis: {title: '###H_AXIS_TITLE_BYTES_DIFF###',  titleTextStyle: {color: '#333'}, format: 'dd-MM HH:mm', minorGridlines: {count: 10, format: 'mm'} },
          vAxis: {title: '###V_AXIS_TITLE_BYTES_DIFF###', scaleType: 'mirrorLog', viewWindow: {} },
          chartArea: { left:'10%', width:'60%' }
        };

        var chartBytesDiff = new google.visualization.LineChart(document.getElementById('chart_div_bytes_diff'));
        chartBytesDiff.draw(dataBytesDiff, optionsBytesDiff);

        // CHART INSTANCES
        
        var dataInstances = google.visualization.arrayToDataTable([
          ###TABLE_INSTANCES###
        ]);
        date_formatter.format(dataInstances, 0);

        var optionsInstances = {
          title: '###TITLE_INSTANCES###',
          hAxis: {title: '###H_AXIS_TITLE_INSTANCES###',  titleTextStyle: {color: '#333'}, format: 'dd-MM HH:mm', minorGridlines: {count: 10, format: 'mm'} },
          vAxis: {title: '###V_AXIS_TITLE_INSTANCES###', scaleType: 'log', viewWindow: {} },
          chartArea: { left:'10%', width:'60%' }
        };

        var chartInstances = new google.visualization.LineChart(document.getElementById('chart_div_instances'));
        chartInstances.draw(dataInstances, optionsInstances);

        // CHART INSTANCES DIFF
        
        var dataInstancesDiff = google.visualization.arrayToDataTable([
          ###TABLE_INSTANCES_DIFF###
        ]);
        date_formatter.format(dataInstancesDiff, 0);

        var optionsInstancesDiff = {
          title: '###TITLE_INSTANCES_DIFF###',
          hAxis: {title: '###H_AXIS_TITLE_INSTANCES_DIFF###',  titleTextStyle: {color: '#333'}, format: 'dd-MM HH:mm', minorGridlines: {count: 10, format: 'mm'} },
          vAxis: {title: '###V_AXIS_TITLE_INSTANCES_DIFF###', scaleType: 'mirrorLog', viewWindow: {} },
          chartArea: { left:'10%', width:'60%' }
        };

        var chartBytesDiff = new google.visualization.LineChart(document.getElementById('chart_div_instances_diff'));
        chartBytesDiff.draw(dataInstancesDiff, optionsInstancesDiff);

    }
    </script>
  </head>
  <body>
    <div style="margin: 5%">
    <h1>Stokpop Memory Check</h2>
    <h2>Heap Histogram Dumps Analysis</h2>
    ###ANALYSIS-RESULT-SUMMARY###
    <h2>Heap Histogram Dumps Charts</h2>
    </div>
    <div id="chart_div_bytes" style="width: 96%; min-height: 800px; height: 50%; margin-left: auto; margin-right: auto"></div>
    <div id="chart_div_bytes_diff" style="width: 96%; min-height: 800px; height: 50%; margin-left: auto; margin-right: auto"></div>
    <div id="chart_div_instances" style="width: 96%; min-height: 800px; height: 50%; margin-left: auto; margin-right: auto"></div>
    <div id="chart_div_instances_diff" style="width: 96%; min-height: 800px; height: 50%; margin-left: auto; margin-right: auto"></div>
  </body>
</html>"""

    fun writeHtmlGoogleGraphFile(data: HeapHistogramDumpReport, reportConfig: ReportConfig): File {
        val title = reportConfig.identifier
        val timestamps = data.heapHistogramDumpDetails.timestamps

        var template = CHART_HTML_TEMPLATE
        val size = timestamps.size
        if (size < 3) {
            val message = String.format("Not enough points ([%d]<3) to create html graph for [%s].", size, title)
            println(message)
            template = "<p>$message<p>"
        } else {
            template = "###ANALYSIS-RESULT-SUMMARY###".toRegex().replace(template, analysisResultTable(data, reportConfig))


            val details = data.heapHistogramDumpDetails.classHistogramDetails
            template = createBytesTable(title, timestamps, details, template)
            template = createBytesTableDiff(title, timestamps, details, template)
            template = createInstancesTable(title, timestamps, details, template)
            template = createInstancesTableDiff(title, timestamps, details, template)
        }
        val file = File(reportConfig.reportDirectory, "heapHistogramDumpReport-$title.html")
        file.writeText(template)
        return file
    }

    private fun analysisResultTable(data: HeapHistogramDumpReport, reportConfig: ReportConfig): String {
        val html = StringBuilder(2048)
        val reportLimits = data.reportLimits

        html.append("<table>")
        html.append("<tr><th>key</th><th>value</th></tr>")
        html.append("<tr><td>Test run id</td><td><b>${reportConfig.identifier}</b></td></tr>").append("\n")

        html.append("<tr><td>Report date</td><td><b>${reportConfig.reportDateTime}</b></td></tr>").append("\n")
        html.append("<tr><td>Overall analysis result</td><td><b>${data.leakResult}</b></td></tr>").append("\n")
        html.append("<tr><td>Report settings</td><td><b>${reportConfig.settings}</b></td></tr>").append("\n")
        html.append("<tr><td>Report class limit</td><td><b>${reportLimits.classLimit}</b></td></tr>").append("\n")
        html.append("<tr><td>Report byte limit</td><td><b>${HumanReadable.humanReadableMemorySize(reportLimits.byteLimit)}</b></td></tr>").append("\n")
        html.append("<tr><td>Maximum allowed growth percentage</td><td><b>${reportLimits.maxGrowthPercentage} %</b></td></tr>").append("\n")
        html.append("<tr><td>Minimum growth points percentage</td><td><b>${reportLimits.minGrowthPointsPercentage} %</b></td></tr>").append("\n")
        html.append("<tr><td>Safe list</td><td><b>${reportLimits.safeList}</b></td></tr>").append("\n")
        html.append("<tr><td>Watch list</td><td><b>${reportLimits.watchList}</b></td></tr>").append("\n")
        html.append("<table>")

        html.append("<table>").append("\n")
        html.append("<tr><th>analysis result</th><th>count</th><th>icon</th></tr>")
        html.append(data.heapHistogramDumpSummary.data.map {
            "<tr><td>${it.key}</td><td>${it.value}</td><td>${mapToUnicodeArrow(it.key)}</td></tr>"
        }.joinToString(separator = "\n"))
        html.append("</table>")
        return html.toString()
    }

    private fun createChartDataTable(
        data: List<ClassHistogramDetails>,
        timestamps: List<Long?>,
        detailsMapper: (ClassHistogramDetails) -> List<Long?>
    ): String {

        val table = StringBuilder(2048)

        // no colors in legend names 😔
        val classNames = data.map { "${mapToUnicodeArrow(it.analysis)}$nonBreakSpace${it.classInfo.name}" }.toList()
        val detailsList = data.map(detailsMapper).toList()

        table.append("['Time'")
        if (classNames.isNotEmpty()) {
            val classNamesList = classNames.map { "'${it}'" }.joinToString(separator = ",")
            table.append(", ").append(classNamesList)
        } else {
            table.append(", 'No Data'")
        }
        table.append("],\n")

        var i = 0
        for (timestamp in timestamps) {
            table.append("[ new Date(").append(timestamp).append(")")
            if (detailsList.isNotEmpty()) {
                table.append(", ")
                val bytesForTimestamp = detailsList.map { it[i] ?: 0 }.joinToString(separator = ",")
                table.append(bytesForTimestamp)
            } else {
                table.append(", 0")
            }
            table.append("]").append(",").append("\n")
            i++
        }
        // remove last ,
        table.deleteCharAt(table.length - 2)
        return table.toString()
    }

    private fun mapToUnicodeArrow(analysis: AnalysisResult): Char {
        return when (analysis) {
            AnalysisResult.GROW_CRITICAL -> arrowUpCritical
            AnalysisResult.GROW_MINOR -> arrowUpMinor
            AnalysisResult.GROW_SAFE -> arrowUpSafe
            AnalysisResult.SHRINK -> arrowDown
            AnalysisResult.STABLE -> arrowFlat
            AnalysisResult.UNKNOWN -> arrowUpDown
        }
    }

    private fun mapToColorUnicodeArrow(analysis: AnalysisResult): String {
        val color = when (analysis) {
            AnalysisResult.GROW_CRITICAL -> "red"
            AnalysisResult.GROW_MINOR -> "orange"
            AnalysisResult.GROW_SAFE -> "green"
            AnalysisResult.SHRINK -> "black"
            AnalysisResult.STABLE -> "black"
            AnalysisResult.UNKNOWN -> "gray"
        }
        return "<div style='color:$color'>${mapToUnicodeArrow(analysis)}</div>"
    }

    private fun createBytesTable(
        title: String,
        timestamps: List<Long?>,
        data: List<ClassHistogramDetails>,
        template: String
    ): String {
        var newTemplate = template

        val detailsMapper: (ClassHistogramDetails) -> List<Long?> = { it.bytes }
        val table = createChartDataTable(data, timestamps, detailsMapper)

        newTemplate = "###TABLE_BYTES###".toRegex().replace(newTemplate, Regex.escapeReplacement(table))
        newTemplate = "###TITLE_BYTES###".toRegex().replace(newTemplate, "bytes in heap - $title")
        newTemplate = "###H_AXIS_TITLE_BYTES###".toRegex().replace(newTemplate, "time")
        newTemplate = "###V_AXIS_TITLE_BYTES###".toRegex().replace(newTemplate, "bytes")
        newTemplate = "###BASE_UNIT_FULL_BYTES###".toRegex().replace(newTemplate, "bytes")
        newTemplate = "###BASE_UNIT_SHORT_BYTES###".toRegex().replace(newTemplate, "B")
        return newTemplate
    }

    private fun createBytesTableDiff(title: String, timestamps: List<Long?>, data: List<ClassHistogramDetails>, template: String): String {
        var newTemplate = template

        val detailsMapper: (ClassHistogramDetails) -> List<Long?> = { it.bytesDiff }
        val table = createChartDataTable(data, timestamps, detailsMapper)

        newTemplate = "###TABLE_BYTES_DIFF###".toRegex().replace(newTemplate, Regex.escapeReplacement(table))
        newTemplate = "###TITLE_BYTES_DIFF###".toRegex().replace(newTemplate, "bytes diff in heap - $title")
        newTemplate = "###H_AXIS_TITLE_BYTES_DIFF###".toRegex().replace(newTemplate, "time")
        newTemplate = "###V_AXIS_TITLE_BYTES_DIFF###".toRegex().replace(newTemplate, "bytes diff")
        newTemplate = "###BASE_UNIT_FULL_BYTES_DIFF###".toRegex().replace(newTemplate, "bytes")
        newTemplate = "###BASE_UNIT_SHORT_BYTES_DIFF###".toRegex().replace(newTemplate, "B")
        return newTemplate
    }

    private fun createInstancesTable(title: String, timestamps: List<Long?>, data: List<ClassHistogramDetails>, template: String): String {
        var newTemplate = template

        val detailsMapper: (ClassHistogramDetails) -> List<Long?> = { it.instances }
        val table = createChartDataTable(data, timestamps, detailsMapper)

        newTemplate = "###TABLE_INSTANCES###".toRegex().replace(newTemplate, Regex.escapeReplacement(table))
        newTemplate = "###TITLE_INSTANCES###".toRegex().replace(newTemplate,"instances in heap - $title")
        newTemplate = "###H_AXIS_TITLE_INSTANCES###".toRegex().replace(newTemplate,"time")
        newTemplate = "###V_AXIS_TITLE_INSTANCES###".toRegex().replace(newTemplate,"instances")
        newTemplate = "###BASE_UNIT_FULL_INSTANCES###".toRegex().replace(newTemplate,"instances")
        newTemplate = "###BASE_UNIT_SHORT_INSTANCES###".toRegex().replace(newTemplate,"#")
        return newTemplate
    }

    private fun createInstancesTableDiff(title: String, timestamps: List<Long?>, data: List<ClassHistogramDetails>, template: String): String {
        var newTemplate = template

        val detailsMapper: (ClassHistogramDetails) -> List<Long?> = { it.instancesDiff }
        val table = createChartDataTable(data, timestamps, detailsMapper)

        newTemplate = "###TABLE_INSTANCES_DIFF###".toRegex().replace(newTemplate, Regex.escapeReplacement(table))
        newTemplate = "###TITLE_INSTANCES_DIFF###".toRegex().replace(newTemplate, "instances diff in heap - $title")
        newTemplate = "###H_AXIS_TITLE_INSTANCES_DIFF###".toRegex().replace(newTemplate, "time")
        newTemplate = "###V_AXIS_TITLE_INSTANCES_DIFF###".toRegex().replace(newTemplate, "instances diff")
        newTemplate = "###BASE_UNIT_FULL_INSTANCES_DIFF###".toRegex().replace(newTemplate, "instances")
        newTemplate = "###BASE_UNIT_SHORT_INSTANCES_DIFF###".toRegex().replace(newTemplate, "#")
        return newTemplate
    }

}