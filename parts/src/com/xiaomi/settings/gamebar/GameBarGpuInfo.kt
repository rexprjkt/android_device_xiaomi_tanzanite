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

object GameBarGpuInfo {

    private val GPU_TEMP_PATHS = arrayOf(
        "/sys/class/thermal/thermal_zone35/temp",
        "/sys/class/thermal/thermal_zone36/temp",
        "/sys/class/thermal/thermal_zone37/temp"
    )

    val gpuTemp: String
        get() {
            var total = 0f
            var count = 0

            for (path in GPU_TEMP_PATHS) {
                var line = readLine(path)
                if (line == null) continue
                line = line.trim()
                try {
                    val raw = line.toFloat()
                    total += raw / 1000f 
                    count++
                } catch (ignored: NumberFormatException) {
                }
            }

            if (count == 0) return "N/A"

            val avg = total / count
            return String.format("%.1f", avg)
        }

    private fun readLine(path: String): String? {
        try {
            BufferedReader(FileReader(path)).use { br -> return br.readLine() }
        } catch (e: IOException) {
            return null
        }
    }
}
