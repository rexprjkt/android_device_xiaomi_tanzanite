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

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import java.util.HashSet

class GameBarAppRemoverFragment : SettingsBasePreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val screen = preferenceManager.createPreferenceScreen(requireContext())
        preferenceScreen = screen
        loadAutoApps(screen)
    }

    private fun loadAutoApps(screen: PreferenceScreen) {
        val packageManager = requireContext().packageManager
        val autoAppsSet = savedAutoApps

        for (pkg in autoAppsSet) {
            try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                val pref = Preference(requireContext()).apply {
                    title = appInfo.loadLabel(packageManager)
                    summary = appInfo.packageName
                    icon = appInfo.loadIcon(packageManager)
                    isPersistent = false
                    setOnPreferenceClickListener {
                        removeAppFromAutoList(appInfo.packageName)
                        Toast.makeText(context, "$title removed.", Toast.LENGTH_SHORT).show()
                        screen.removePreference(this)
                        true
                    }
                }
                screen.addPreference(pref)
            } catch (e: PackageManager.NameNotFoundException) {
                removeAppFromAutoList(pkg)
            }
        }
    }

    private val savedAutoApps: Set<String>
        get() = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getStringSet(GameBarAppSelectorFragment.PREF_AUTO_APPS, HashSet()) ?: HashSet()

    private fun removeAppFromAutoList(packageName: String) {
        val autoApps = HashSet(savedAutoApps)
        autoApps.remove(packageName)
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .edit().putStringSet(GameBarAppSelectorFragment.PREF_AUTO_APPS, autoApps).apply()
    }
}
