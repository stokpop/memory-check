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

import kotlinx.serialization.Serializable

@Serializable
data class ClassInfo(val name: String, val isOnSafeList: Boolean = false, val isOnWatchList: Boolean = false) {
    companion object {
        fun prefixWatchListAndSafeList(classInfo: ClassInfo): String {
            val prefixString = java.lang.StringBuilder()
            if (classInfo.isOnWatchList) prefixString.append("(WL)")
            if (classInfo.isOnSafeList) prefixString.append("(SL)")
            return if (prefixString.isEmpty()) "" else prefixString.toString()
        }

    }
}
