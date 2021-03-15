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
package nl.stokpop.memory.domain

import java.util.regex.Pattern

sealed class ClassPatternSet(classSet: Set<String>) {

    private val regexps: Set<Pattern>
    private val classPatterns: Set<String>

    init {
        regexps = classSet
            .asSequence()
            .filter { isWildcard(it) }
            .map { it.replace(".", "\\.") }
            .map { it.replace("*", ".*") }
            .map { Pattern.compile(it) }
            .toSet()

        classPatterns = classSet
            .filter { !isWildcard(it) }
            .toSet()
    }

    private fun isWildcard(it: String) = it.contains("*")

    fun matches(name: String): Boolean {
        return classPatterns.contains(name) || regexps.any { it.matcher(name).find() }
    }
}

class SafeList(safeClassPatterns: Set<String>) : ClassPatternSet(safeClassPatterns)
class WatchList(watchClassPatterns: Set<String>) : ClassPatternSet(watchClassPatterns)
