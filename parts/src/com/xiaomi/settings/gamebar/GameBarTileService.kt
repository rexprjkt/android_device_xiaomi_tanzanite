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

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.preference.PreferenceManager
import com.xiaomi.settings.R

class GameBarTileService : TileService() {
    private var mGameBar: GameBar? = null

    override fun onCreate() {
        super.onCreate()
        mGameBar = GameBar.getInstance(this)
    }

    override fun onStartListening() {
        val enabled = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean("game_bar_enable", false)
        updateTileState(enabled)
    }

    override fun onClick() {
        val currentlyEnabled = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean("game_bar_enable", false)
        val newState = !currentlyEnabled

        PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
            .putBoolean("game_bar_enable", newState)
            .commit()

        updateTileState(newState)

        if (newState) {
            mGameBar?.applyPreferences()
            mGameBar?.show()
        } else {
            mGameBar?.hide()
        }
    }

    private fun updateTileState(enabled: Boolean) {
        val tile = qsTile ?: return
        
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.game_bar_tile_label)
        tile.contentDescription = getString(R.string.game_bar_tile_description)
        tile.updateTile()
    }
}
