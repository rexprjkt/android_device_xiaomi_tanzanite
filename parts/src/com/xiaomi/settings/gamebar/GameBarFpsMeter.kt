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

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.view.WindowManager
import android.window.TaskFpsCallback
import androidx.preference.PreferenceManager
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

class GameBarFpsMeter private constructor(context: Context) {

    private val mContext: Context = context
    private val mWindowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
    private var mCurrentFps = 0f
    private var mTaskFpsCallback: TaskFpsCallback? = null
    private var mCallbackRegistered = false
    private var mCurrentTaskId = -1
    private var mLastFpsUpdateTime = System.currentTimeMillis()
    private val mHandler = Handler()

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mTaskFpsCallback = object : TaskFpsCallback() {
                override fun onFpsReported(fps: Float) {
                    if (fps > 0) {
                        mCurrentFps = fps
                        mLastFpsUpdateTime = System.currentTimeMillis()
                    }
                }
            }
        }
    }

    fun start() {
        val method = mPrefs.getString("game_bar_fps_method", "new")
        if ("new" != method) return

        stop()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val taskId = focusedTaskId
            if (taskId <= 0) {
                return
            }
            mCurrentTaskId = taskId
            try {
                mWindowManager.registerTaskFpsCallback(mCurrentTaskId, { it.run() }, mTaskFpsCallback!!)
                mCallbackRegistered = true
            } catch (e: Exception) {
            }
            mLastFpsUpdateTime = System.currentTimeMillis()
            mHandler.postDelayed(mTaskCheckRunnable, TASK_CHECK_INTERVAL_MS)
        }
    }

    fun stop() {
        val method = mPrefs.getString("game_bar_fps_method", "new")
        if ("new" == method && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (mCallbackRegistered) {
                try {
                    mWindowManager.unregisterTaskFpsCallback(mTaskFpsCallback!!)
                } catch (e: Exception) {
                }
                mCallbackRegistered = false
            }
            mHandler.removeCallbacks(mTaskCheckRunnable)
        }
    }

    val fps: Float
        get() {
            val method = mPrefs.getString("game_bar_fps_method", "new")
            return if ("legacy" == method) {
                readLegacyFps()
            } else {
                mCurrentFps
            }
        }

    private fun readLegacyFps(): Float {
        try {
            BufferedReader(FileReader("/sys/class/drm/sde-crtc-0/measured_fps")).use { br ->
                val line = br.readLine()
                if (line != null && line.startsWith("fps:")) {
                    val parts = line.split("\\s+".toRegex()).toTypedArray()
                    if (parts.size >= 2) {
                        return parts[1].trim().toFloat()
                    }
                }
            }
        } catch (e: IOException) {
        } catch (e: NumberFormatException) {
        }
        return -1f
    }

    private val mTaskCheckRunnable = object : Runnable {
        override fun run() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val newTaskId = focusedTaskId
                if (newTaskId > 0 && newTaskId != mCurrentTaskId) {
                    reinitCallback()
                } else {
                    val now = System.currentTimeMillis()
                    if (now - mLastFpsUpdateTime > STALENESS_THRESHOLD_MS) {
                        reinitCallback()
                    }
                }
                mHandler.postDelayed(this, TASK_CHECK_INTERVAL_MS)
            }
        }
    }

    private val focusedTaskId: Int
        get() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return -1
            }
            try {
                val atmClass = Class.forName("android.app.ActivityTaskManager")
                val getServiceMethod = atmClass.getDeclaredMethod("getService")
                val atmService = getServiceMethod.invoke(null)
                val getFocusedRootTaskInfoMethod = atmService.javaClass.getMethod("getFocusedRootTaskInfo")
                val taskInfo = getFocusedRootTaskInfoMethod.invoke(atmService)
                if (taskInfo != null) {
                    try {
                        val taskIdField = taskInfo.javaClass.getField("taskId")
                        return taskIdField.getInt(taskInfo)
                    } catch (nsfe: NoSuchFieldException) {
                        try {
                            val taskIdField = taskInfo.javaClass.getField("mTaskId")
                            return taskIdField.getInt(taskInfo)
                        } catch (nsfe2: NoSuchFieldException) {
                        }
                    }
                }
            } catch (e: Exception) {
            }
            return -1
        }

    private fun reinitCallback() {
        stop()
        mHandler.postDelayed({ start() }, 500)
    }

    companion object {
        private const val TOLERANCE = 0.1f
        private const val STALENESS_THRESHOLD_MS: Long = 2000
        private const val TASK_CHECK_INTERVAL_MS: Long = 1000

        @Volatile
        private var sInstance: GameBarFpsMeter? = null

        fun getInstance(context: Context): GameBarFpsMeter {
            return sInstance ?: synchronized(this) {
                sInstance ?: GameBarFpsMeter(context.applicationContext).also { sInstance = it }
            }
        }
    }
}
