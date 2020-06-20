package nl.stokpop.memory

import nl.stokpop.memory.domain.AnalysisResult

class ReportConfig(settings: String) {
    private val uppercaseSettings = settings.toUpperCase()
    val doReportGrow = uppercaseSettings.contains(AnalysisResult.GROW.name)
    val doReportUnknowns = uppercaseSettings.contains(AnalysisResult.UNKNOWN.name)
    val doReportShrinks = uppercaseSettings.contains(AnalysisResult.SHRINK.name)
    val doReportStable  = uppercaseSettings.contains(AnalysisResult.STABLE.name)

}