package nl.stokpop.memory.domain

import java.util.regex.Pattern

class SafeGrowSet(safeGrowSet: Set<String>) {

    private val regexps: Set<Pattern>
    private val safeGrows: Set<String>

    init {
        regexps = safeGrowSet
            .asSequence()
            .filter { isWildcard(it) }
            .map { it.replace(".", "\\.") }
            .map { it.replace("*", ".*") }
            .map { Pattern.compile(it) }
            .toSet()

        safeGrows = safeGrowSet
            .filter { !isWildcard(it) }
            .toSet()
    }

    private fun isWildcard(it: String) = it.contains("*")

    fun isSafeToGrow(name: String): Boolean {
        return safeGrows.contains(name) || regexps.any { it.matcher(name).find() }
    }
}