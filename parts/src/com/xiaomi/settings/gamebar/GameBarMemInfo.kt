/*
 * Copyright (C) 2025 kenway214
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaomi.settings.gamebar

import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

object GameBarMemInfo {

    val ramUsage: String
        get() {
            var memTotal: Long = 0
            var memAvailable: Long = 0

            try {
                BufferedReader(FileReader("/proc/meminfo")).use { br ->
                    var line: String?
                    while (br.readLine().also { line = it } != null) {
                        if (line!!.startsWith("MemTotal:")) {
                            memTotal = parseMemValue(line!!)
                        } else if (line!!.startsWith("MemAvailable:")) {
                            memAvailable = parseMemValue(line!!)
                        }
                        if (memTotal > 0 && memAvailable > 0) {
                            break
                        }
                    }
                }
            } catch (e: IOException) {
                return "N/A"
            }

            if (memTotal == 0L) {
                return "N/A"
            }

            val usedKb = memTotal - memAvailable
            val usedMb = usedKb / 1024
            return usedMb.toString()
        }

    private fun parseMemValue(line: String): Long {
        val parts = line.split("\\s+".toRegex()).toTypedArray()
        if (parts.size < 3) {
            return 0
        }
        return try {
            parts[1].toLong()
        } catch (e: NumberFormatException) {
            0
        }
    }
}
