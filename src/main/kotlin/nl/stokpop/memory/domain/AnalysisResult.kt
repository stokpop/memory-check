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
    GROW_CRITICAL("critical growth detected (above 'maximum allowed growth percentage')"),
    GROW_MINOR("minor growth detected (below 'maximum allowed growth percentage')"),
    GROW_SAFE("growth detected in 'safe list' of known growing classes"),
    GROW_HICK_UPS("growth with hick-ups (less than 'minimum growth points percentage')"),
    SHRINK_AND_GROW("both shrink and growth detected"),
    STABLE("all histograms show same number of objects"),
    SHRINK("opposite of growth: only shrinks detected"),
    SINGLE("present in one histogram only (probably a temporary class such as a lambda)"),
    UNKNOWN("no matching analysis result");

}
