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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xiaomi.settings.R

class GameBarAppsAdapter(
    private val packageManager: PackageManager,
    private val apps: List<ApplicationInfo>,
    private val listener: OnAppClickListener?
) : RecyclerView.Adapter<GameBarAppsAdapter.ViewHolder>() {

    interface OnAppClickListener {
        fun onAppClick(appInfo: ApplicationInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.game_bar_app_selector_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appInfo = apps[position]
        holder.appName.text = appInfo.loadLabel(packageManager)
        holder.appPackage.text = appInfo.packageName
        holder.appIcon.setImageDrawable(appInfo.loadIcon(packageManager))
        holder.itemView.setOnClickListener {
            listener?.onAppClick(appInfo)
        }
    }

    override fun getItemCount(): Int {
        return apps.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appName: TextView = itemView.findViewById(R.id.app_name)
        val appPackage: TextView = itemView.findViewById(R.id.app_package)
        val appIcon: ImageView = itemView.findViewById(R.id.app_icon)
    }
}
