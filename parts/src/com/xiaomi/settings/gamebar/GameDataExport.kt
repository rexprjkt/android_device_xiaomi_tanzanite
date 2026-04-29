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

import android.os.Environment
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale

class GameDataExport private constructor() {

    var isCapturing: Boolean = false
        private set

    private val mStatsRows = ArrayList<Array<String>>()

    fun startCapture() {
        isCapturing = true
        mStatsRows.clear()
        mStatsRows.add(CSV_HEADER)
    }

    fun stopCapture() {
        isCapturing = false
    }

    fun addOverlayData(
        dateTime: String,
        packageName: String,
        fps: String,
        batteryTemp: String,
        cpuUsage: String,
        cpuTemp: String,
        gpuTemp: String
    ) {
        if (!isCapturing) return

        val row = arrayOf(
            dateTime,
            packageName,
            fps,
            batteryTemp,
            cpuUsage,
            cpuTemp,
            gpuTemp
        )
        mStatsRows.add(row)
    }

    fun exportDataToCsv() {
        if (mStatsRows.size <= 1) {
            return
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outFile = File(Environment.getExternalStorageDirectory(), "GameBar_log_$timeStamp.csv")

        var bw: BufferedWriter? = null
        try {
            bw = BufferedWriter(FileWriter(outFile, true))
            for (row in mStatsRows) {
                bw.write(toCsvLine(row))
                bw.newLine()
            }
            bw.flush()
        } catch (ignored: IOException) {
        } finally {
            if (bw != null) {
                try {
                    bw.close()
                } catch (ignored: IOException) {
                }
            }
        }
    }

    private fun toCsvLine(columns: Array<String>): String {
        val sb = StringBuilder()
        for (i in columns.indices) {
            sb.append(columns[i])
            if (i < columns.size - 1) {
                sb.append(",")
            }
        }
        return sb.toString()
    }

    companion object {
        private val CSV_HEADER = arrayOf(
            "DateTime",
            "PackageName",
            "FPS",
            "Battery_Temp",
            "CPU_Usage",
            "CPU_Temp",
            "GPU_Temp"
        )

        @Volatile
        private var sInstance: GameDataExport? = null

        fun getInstance(): GameDataExport {
            return sInstance ?: synchronized(this) {
                sInstance ?: GameDataExport().also { sInstance = it }
            }
        }
    }
}
