/*
 * Copyright (C) 2023-2026 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.battery

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

object BatteryUtils {
    private const val TAG = "XiaomiBatteryUtils"
    const val PREF_CHARGING_CTRL = "charging_control"
    const val PREF_CHARGING_LIMIT = "charging_limit"
    private const val NODE_SUSPEND = "/sys/class/power_supply/battery/input_suspend"

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun setChargingSuspend(suspend: Boolean) {
        val file = File(NODE_SUSPEND)
        if (!file.exists()) return
        val value = if (suspend) "1" else "0"
        try {
            FileOutputStream(file).use { fos ->
                fos.write(value.toByteArray(StandardCharsets.UTF_8))
                fos.flush()
                try {
                    fos.fd.sync()
                } catch (ignored: IOException) {}
            }
            Log.d(TAG, "Successfully wrote $value to $NODE_SUSPEND")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write to $NODE_SUSPEND", e)
        }
    }

    fun setChargingSuspendAsync(suspend: Boolean) {
        ioScope.launch {
            setChargingSuspend(suspend)
        }
    }
}
