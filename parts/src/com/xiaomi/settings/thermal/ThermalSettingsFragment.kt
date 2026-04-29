/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.thermal

import android.app.Activity
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.os.UserHandle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xiaomi.settings.R
import com.xiaomi.settings.thermal.ThermalUtils.ThermalState
import com.xiaomi.settings.utils.dlog
import com.android.settingslib.widget.MainSwitchBar

class ThermalSettingsFragment : SettingsBasePreferenceFragment() {

    private lateinit var appsAdapter: AppsAdapter
    private lateinit var launcherApps: LauncherApps
    private lateinit var thermalUtils: ThermalUtils
    private lateinit var appsRecyclerView: RecyclerView
    private lateinit var loadingView: View
    private var isLoaded = false
    private val handlerThread = HandlerThread(TAG).apply { start() }
    private val bgHandler = Handler(handlerThread.looper)

    private val launcherAppsCallback =
        object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) {
                if (user != Process.myUserHandle()) return
                dlog(TAG, "onPackageRemoved: $packageName")
                loadApps()
            }

            override fun onPackageAdded(packageName: String, user: UserHandle) {
                if (user != Process.myUserHandle()) return
                dlog(TAG, "onPackageAdded: $packageName")
                loadApps()
            }

            override fun onPackageChanged(packageName: String, user: UserHandle) {}
            override fun onPackagesAvailable(p: Array<String>, u: UserHandle, r: Boolean) {}
            override fun onPackagesUnavailable(p: Array<String>, u: UserHandle, r: Boolean) {}
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        thermalUtils = ThermalUtils.getInstance(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcherApps = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        appsAdapter = AppsAdapter(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?, 
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.thermal_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appsRecyclerView = view.findViewById(R.id.thermal_rv_view)
        appsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        appsRecyclerView.adapter = appsAdapter
        
        loadingView = view.findViewById(R.id.thermal_loading)
        loadApps()
        launcherApps.registerCallback(launcherAppsCallback, bgHandler)
    }

    override fun onDestroy() {
        super.onDestroy()
        dlog(TAG, "onDestroy")
        handlerThread.quitSafely()
        launcherApps.unregisterCallback(launcherAppsCallback)
    }

    private fun loadApps() {
        bgHandler.post {
            val appEntries = launcherApps
                .getActivityList(null, Process.myUserHandle())
                .distinctBy { it.componentName.packageName }
                .map { info ->
                    AppEntry(
                        packageName = info.componentName.packageName,
                        label = info.label.toString(),
                        icon = info.getIcon(0),
                        state = thermalUtils.getStateForPackage(info.componentName.packageName)
                    )
                }
                .sortedBy { it.label.lowercase() }
            
            dlog(TAG, "loaded ${appEntries.size} apps")
            
            requireActivity().runOnUiThread {
                appsAdapter.entries.clear()
                appsAdapter.entries.addAll(appEntries)
                appsAdapter.notifyDataSetChanged()
                isLoaded = true
                updateVisibility()
            }
        }
    }

    private fun updateVisibility() {
        appsRecyclerView.visibility = if (isLoaded) View.VISIBLE else View.GONE
        loadingView.visibility = if (isLoaded) View.GONE else View.VISIBLE
    }

    private data class AppEntry(
        val packageName: String,
        val label: String,
        val icon: Drawable,
        val state: ThermalState
    )

    private inner class AppsAdapter(private val activity: Activity) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>(), AdapterView.OnItemSelectedListener {

        var entries = mutableListOf<AppEntry>()
        private val modeAdapter = ModeAdapter(activity)

        override fun getItemViewType(position: Int): Int = if (position == 0) 0 else 1

        override fun getItemCount() = entries.size + 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == 0) {
                val switchBar = MainSwitchBar(parent.context)
                
                val params = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                val horizontalMargin = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 16f, parent.context.resources.displayMetrics
                ).toInt()
                val verticalMargin = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 12f, parent.context.resources.displayMetrics
                ).toInt()

                params.setMargins(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin)
                
                switchBar.layoutParams = params
                HeaderViewHolder(switchBar)
            } else {
                ItemViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.thermal_list_item, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is HeaderViewHolder) {
                holder.switchBar.apply {
                    setTitle(activity.getString(R.string.thermal_enable))
                    setChecked(thermalUtils.enabled)
                    addOnSwitchChangeListener { _, isChecked ->
                        dlog(TAG, "Switch changed: $isChecked")
                        thermalUtils.enabled = isChecked
                        notifyDataSetChanged()
                    }
                    show()
                }
            } else if (holder is ItemViewHolder) {
                val entry = entries[position - 1]
                val state = thermalUtils.getStateForPackage(entry.packageName)
                
                holder.itemView.alpha = if (thermalUtils.enabled) 1.0f else 0.4f
                holder.mode.isEnabled = thermalUtils.enabled
                
                holder.mode.apply {
                    adapter = modeAdapter
                    setSelection(state.id, false)
                    onItemSelectedListener = this@AppsAdapter
                    tag = entry
                }
                holder.title.text = entry.label
                holder.icon.setImageDrawable(entry.icon)
            }
        }

        override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
            val entry = p?.tag as? AppEntry ?: return
            dlog(TAG, "onItemSelected: ${entry.packageName} -> $pos")
            thermalUtils.writePackage(entry.packageName, pos)
        }

        override fun onNothingSelected(p: AdapterView<*>?) {}
    }

    private inner class HeaderViewHolder(val switchBar: MainSwitchBar) : RecyclerView.ViewHolder(switchBar)

    private inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.app_name)
        val mode: Spinner = view.findViewById(R.id.app_mode)
        val icon: ImageView = view.findViewById(R.id.app_icon)
    }

    private inner class ModeAdapter(context: Context) : BaseAdapter() {
        private val inflater = LayoutInflater.from(context)
        private val items = ThermalState.values().map { it.label }
        override fun getCount() = items.size
        override fun getItem(p: Int) = items[p]
        override fun getItemId(p: Int) = 0L
        override fun getView(p: Int, cv: View?, parent: ViewGroup): View {
            val tv = (cv as? TextView ?: inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false) as TextView)
            tv.text = context?.getString(items[p]) ?: ""
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            return tv
        }
    }

    companion object {
        private const val TAG = "ThermalSettingsFragment"
    }
}
