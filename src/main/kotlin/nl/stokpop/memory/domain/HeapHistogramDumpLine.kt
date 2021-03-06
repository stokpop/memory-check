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
package nl.stokpop.memory.domain

/**
 * Representation of one line in a heap histogram dump.
 */
data class HeapHistogramDumpLine(
    val classInfo: ClassInfo,
    val lineNumber: Long?,
    val instances: Long?,
    val bytes: Long?
) {
    companion object {
        fun createGhostLine(name: ClassInfo) = HeapHistogramDumpLine(name, null, null, null)
    }

    fun isGhost(): Boolean {
        return lineNumber == null
    }
}
