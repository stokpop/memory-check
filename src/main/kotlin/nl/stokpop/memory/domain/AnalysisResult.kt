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

enum class AnalysisResult(val description: String) {
    GROW_CRITICAL("critical growth detected (above critical threshold)"),
    GROW_MINOR("minor growth detected (below critical threshold)"),
    GROW_SAFE("growth detected in white list of known growing classes"),
    STABLE("no growth detected"),
    SHRINK("opposite of growth detected, might indicate reduction in load"),
    UNKNOWN("both growth and shrink detected")
}
