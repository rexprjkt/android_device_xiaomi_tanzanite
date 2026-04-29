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

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xiaomi.settings.R
import java.util.HashSet

class GameBarAppRemoverFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private var adapter: GameBarAutoAppsAdapter? = null
    private lateinit var packageManager: PackageManager
    private var autoAppsList: MutableList<ApplicationInfo>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.game_bar_app_selector, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.app_list)
        packageManager = requireContext().packageManager
        recyclerView.layoutManager = LinearLayoutManager(context)
        loadAutoApps()
    }

    private fun loadAutoApps() {
        val autoAppsSet = savedAutoApps
        autoAppsList = ArrayList()
        for (pkg in autoAppsSet) {
            try {
                val info = packageManager.getApplicationInfo(pkg, 0)
                autoAppsList!!.add(info)
            } catch (e: PackageManager.NameNotFoundException) {
            }
        }
        val listener = object : GameBarAutoAppsAdapter.OnAppRemoveListener {
            override fun onAppRemove(appInfo: ApplicationInfo) {
                removeAppFromAutoList(appInfo.packageName)
                Toast.makeText(context, appInfo.loadLabel(packageManager).toString() + " removed.", Toast.LENGTH_SHORT).show()
                autoAppsList!!.remove(appInfo)
                adapter!!.notifyDataSetChanged()
            }
        }
        adapter = GameBarAutoAppsAdapter(packageManager, autoAppsList!!, listener)
        recyclerView.adapter = adapter
    }

    private val savedAutoApps: Set<String>
        get() = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getStringSet(GameBarAppSelectorFragment.PREF_AUTO_APPS, HashSet())!!

    private fun removeAppFromAutoList(packageName: String) {
        val autoApps = HashSet(savedAutoApps)
        autoApps.remove(packageName)
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .edit().putStringSet(GameBarAppSelectorFragment.PREF_AUTO_APPS, autoApps).apply()
    }
}
