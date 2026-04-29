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

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import androidx.preference.PreferenceManager
import java.util.HashSet

class GameBarMonitorService : Service() {

    private var mHandler: Handler? = null
    private var mMonitorRunnable: Runnable? = null
    
    companion object {
        private const val MONITOR_INTERVAL = 2000L // 2 seconds
    }

    override fun onCreate() {
        super.onCreate()
        mHandler = Handler()
        mMonitorRunnable = object : Runnable {
            override fun run() {
                monitorForegroundApp()
                mHandler?.postDelayed(this, MONITOR_INTERVAL)
            }
        }
        mHandler?.post(mMonitorRunnable!!)
    }

    private fun monitorForegroundApp() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val masterEnabled = prefs.getBoolean("game_bar_enable", false)
        if (masterEnabled) {
            GameBar.getInstance(this).applyPreferences()
            GameBar.getInstance(this).show()
            return
        }

        val autoEnabled = prefs.getBoolean("game_bar_auto_enable", false)
        if (!autoEnabled) {
            GameBar.getInstance(this).hide()
            return
        }

        val foreground = ForegroundAppDetector.getForegroundPackageName(this)
        val autoApps = prefs.getStringSet(GameBarAppSelectorFragment.PREF_AUTO_APPS, HashSet())!!
        if (autoApps.contains(foreground)) {
            GameBar.getInstance(this).applyPreferences()
            GameBar.getInstance(this).show()
        } else {
            GameBar.getInstance(this).hide()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler?.removeCallbacks(mMonitorRunnable!!)
    }
}
