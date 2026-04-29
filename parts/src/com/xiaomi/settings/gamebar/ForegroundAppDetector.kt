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

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.lang.reflect.Method
import java.util.List

object ForegroundAppDetector {

    private const val TAG = "ForegroundAppDetector"

    fun getForegroundPackageName(context: Context): String {
        var pkg = tryGetRunningTasks(context)
        if (pkg != null) {
            return pkg
        }
        pkg = tryReflectActivityTaskManager()
        if (pkg != null) {
            return pkg
        }
        return "Unknown"
    }

    private fun tryGetRunningTasks(context: Context): String? {
        try {
            if (context.checkSelfPermission("android.permission.GET_TASKS")
                == PackageManager.PERMISSION_GRANTED
            ) {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val tasks = am.getRunningTasks(1)
                if (tasks != null && tasks.isNotEmpty()) {
                    val top = tasks[0]
                    if (top.topActivity != null) {
                        return top.topActivity!!.packageName
                    }
                }
            } else {
                Log.w(TAG, "GET_TASKS permission not granted to this system app?")
            }
        } catch (e: Exception) {
            Log.e(TAG, "tryGetRunningTasks error: ", e)
        }
        return null
    }

    private fun tryReflectActivityTaskManager(): String? {
        try {
            val atmClass = Class.forName("android.app.ActivityTaskManager")
            val getServiceMethod = atmClass.getDeclaredMethod("getService")
            getServiceMethod.isAccessible = true
            val atmService = getServiceMethod.invoke(null)
            val getTasksMethod = atmService.javaClass.getMethod("getTasks", Int::class.javaPrimitiveType)
            val taskList = getTasksMethod.invoke(atmService, 1) as List<*>
            if (taskList != null && !taskList.isEmpty()) {
                val firstTask = taskList[0]!!
                val rtiClass = firstTask.javaClass
                val getTopActivityMethod = rtiClass.getDeclaredMethod("getTopActivity")
                val compName = getTopActivityMethod.invoke(firstTask)
                if (compName != null) {
                    val getPackageNameMethod = compName.javaClass.getMethod("getPackageName")
                    return getPackageNameMethod.invoke(compName) as String
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "tryReflectActivityTaskManager error: ", e)
        }
        return null
    }
}
