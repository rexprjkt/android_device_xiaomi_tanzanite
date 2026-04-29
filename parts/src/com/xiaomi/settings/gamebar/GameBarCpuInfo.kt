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
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.Collections

object GameBarCpuInfo {

    private var sPrevIdle: Long = -1
    private var sPrevTotal: Long = -1

    private val CPU_TEMP_PATHS = Array(16) { i ->
        "/sys/class/thermal/thermal_zone" + (10 + i) + "/temp"
    }

    val cpuUsage: String
        get() {
            val line = readLine("/proc/stat")
            if (line == null || !line.startsWith("cpu ")) return "N/A"
            val parts = line.split("\\s+".toRegex()).toTypedArray()
            if (parts.size < 8) return "N/A"

            try {
                val user = parts[1].toLong()
                val nice = parts[2].toLong()
                val system = parts[3].toLong()
                val idle = parts[4].toLong()
                val iowait = parts[5].toLong()
                val irq = parts[6].toLong()
                val softirq = parts[7].toLong()
                val steal = if (parts.size > 8) parts[8].toLong() else 0

                val total = user + nice + system + idle + iowait + irq + softirq + steal

                if (sPrevTotal != -1L && total != sPrevTotal) {
                    val diffTotal = total - sPrevTotal
                    val diffIdle = idle - sPrevIdle
                    val usage = 100 * (diffTotal - diffIdle) / diffTotal
                    sPrevTotal = total
                    sPrevIdle = idle
                    return usage.toString()
                } else {
                    sPrevTotal = total
                    sPrevIdle = idle
                    return "N/A"
                }
            } catch (e: NumberFormatException) {
                return "N/A"
            }
        }

    val cpuFrequencies: List<String>
        get() {
            val result = ArrayList<String>()
            val cpuDirPath = "/sys/devices/system/cpu/"
            val cpuDir = File(cpuDirPath)
            val files = cpuDir.listFiles { _, name -> name.matches("cpu\\d+".toRegex()) }
            if (files == null || files.isEmpty()) {
                return result
            }

            val cpuFolders = ArrayList<File>()
            Collections.addAll(cpuFolders, *files)
            cpuFolders.sortWith(Comparator.comparingInt { extractCpuNumber(it) })

            for (cpu in cpuFolders) {
                val freqPath = cpu.absolutePath + "/cpufreq/scaling_cur_freq"
                val freqStr = readLine(freqPath)
                if (freqStr != null && freqStr.isNotEmpty()) {
                    try {
                        val khz = freqStr.trim().toInt()
                        val mhz = khz / 1000
                        result.add(cpu.name + ": " + mhz + " MHz")
                    } catch (e: NumberFormatException) {
                        result.add(cpu.name + ": N/A")
                    }
                } else {
                    result.add(cpu.name + ": offline or frequency not available")
                }
            }
            return result
        }

    val cpuTemp: String
        get() {
            var total = 0f
            var count = 0

            for (path in CPU_TEMP_PATHS) {
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

    private fun extractCpuNumber(cpuFolder: File): Int {
        val name = cpuFolder.name.replace("cpu", "")
        return try {
            name.toInt()
        } catch (e: NumberFormatException) {
            -1
        }
    }

    private fun readLine(path: String): String? {
        try {
            BufferedReader(FileReader(path)).use { br -> return br.readLine() }
        } catch (e: IOException) {
            return null
        }
    }
}
