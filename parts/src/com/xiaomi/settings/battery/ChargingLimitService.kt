/*
 * Copyright (C) 2023-2026 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.battery

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.IBinder
import androidx.preference.PreferenceManager

class ChargingLimitService : Service() {
    private var mReceiverRegistered = false

    private val mBatteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Intent.ACTION_BATTERY_CHANGED == intent.action) {
                updateChargingState(context, intent)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val appContext = applicationContext
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = appContext.registerReceiver(null, filter)
        updateChargingState(appContext, batteryStatus)

        if (!mReceiverRegistered) {
            appContext.registerReceiver(mBatteryReceiver, filter)
            mReceiverRegistered = true
        }
        return START_STICKY
    }

    private fun updateChargingState(context: Context, intent: Intent?) {
        intent ?: return
        val storageContext = context.createDeviceProtectedStorageContext()
        val prefs = PreferenceManager.getDefaultSharedPreferences(storageContext)

        if (!prefs.getBoolean(BatteryUtils.PREF_CHARGING_CTRL, false)) {
            BatteryUtils.setChargingSuspendAsync(false)
            return
        }

        val limit = prefs.getInt(BatteryUtils.PREF_CHARGING_LIMIT, 80)
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        if (level != -1) {
            BatteryUtils.setChargingSuspendAsync(level >= limit)
        }
    }

    override fun onDestroy() {
        if (mReceiverRegistered) {
            applicationContext.unregisterReceiver(mBatteryReceiver)
            mReceiverRegistered = false
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
