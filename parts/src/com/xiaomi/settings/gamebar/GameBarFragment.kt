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
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.widget.MainSwitchPreference
import com.xiaomi.settings.R

class GameBarFragment : SettingsBasePreferenceFragment() {

    private lateinit var mGameBar: GameBar
    private var mMasterSwitch: MainSwitchPreference? = null
    private var mAutoEnableSwitch: SwitchPreferenceCompat? = null
    private var mFpsSwitch: SwitchPreferenceCompat? = null
    private var mBatteryTempSwitch: SwitchPreferenceCompat? = null
    private var mCpuUsageSwitch: SwitchPreferenceCompat? = null
    private var mCpuClockSwitch: SwitchPreferenceCompat? = null
    private var mCpuTempSwitch: SwitchPreferenceCompat? = null
    private var mRamSwitch: SwitchPreferenceCompat? = null
    private var mCaptureStartPref: Preference? = null
    private var mCaptureStopPref: Preference? = null
    private var mCaptureExportPref: Preference? = null
    private var mDoubleTapCapturePref: SwitchPreferenceCompat? = null
    private var mSingleTapTogglePref: SwitchPreferenceCompat? = null
    private var mLongPressEnablePref: SwitchPreferenceCompat? = null
    private var mLongPressTimeoutPref: ListPreference? = null
    private var mTextSizePref: SeekBarPreference? = null
    private var mBgAlphaPref: SeekBarPreference? = null
    private var mCornerRadiusPref: SeekBarPreference? = null
    private var mPaddingPref: SeekBarPreference? = null
    private var mItemSpacingPref: SeekBarPreference? = null
    private var mUpdateIntervalPref: ListPreference? = null
    private var mTextColorPref: ListPreference? = null
    private var mTitleColorPref: ListPreference? = null
    private var mValueColorPref: ListPreference? = null
    private var mPositionPref: ListPreference? = null
    private var mSplitModePref: ListPreference? = null
    private var mOverlayFormatPref: ListPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.game_bar_preferences, rootKey)

        mGameBar = GameBar.getInstance(requireContext())

        // Initialize all preferences.
        mMasterSwitch = findPreference("game_bar_enable")
        mAutoEnableSwitch = findPreference("game_bar_auto_enable")
        mFpsSwitch = findPreference("game_bar_fps_enable")
        mBatteryTempSwitch = findPreference("game_bar_temp_enable")
        mCpuUsageSwitch = findPreference("game_bar_cpu_usage_enable")
        mCpuClockSwitch = findPreference("game_bar_cpu_clock_enable")
        mCpuTempSwitch = findPreference("game_bar_cpu_temp_enable")
        mRamSwitch = findPreference("game_bar_ram_enable")

        mCaptureStartPref = findPreference("game_bar_capture_start")
        mCaptureStopPref = findPreference("game_bar_capture_stop")
        mCaptureExportPref = findPreference("game_bar_capture_export")

        mDoubleTapCapturePref = findPreference("game_bar_doubletap_capture")
        mSingleTapTogglePref = findPreference("game_bar_single_tap_toggle")
        mLongPressEnablePref = findPreference("game_bar_longpress_enable")
        mLongPressTimeoutPref = findPreference("game_bar_longpress_timeout")

        mTextSizePref = findPreference("game_bar_text_size")
        mBgAlphaPref = findPreference("game_bar_background_alpha")
        mCornerRadiusPref = findPreference("game_bar_corner_radius")
        mPaddingPref = findPreference("game_bar_padding")
        mItemSpacingPref = findPreference("game_bar_item_spacing")

        mUpdateIntervalPref = findPreference("game_bar_update_interval")
        mTextColorPref = findPreference("game_bar_text_color")
        mTitleColorPref = findPreference("game_bar_title_color")
        mValueColorPref = findPreference("game_bar_value_color")
        mPositionPref = findPreference("game_bar_position")
        mSplitModePref = findPreference("game_bar_split_mode")
        mOverlayFormatPref = findPreference("game_bar_format")

        val appSelectorPref = findPreference<Preference>("game_bar_app_selector")
        appSelectorPref?.setOnPreferenceClickListener {
            val intent = Intent(context, GameBarAppSelectorActivity::class.java)
            startActivity(intent)
            true
        }
        val appRemoverPref = findPreference<Preference>("game_bar_app_remover")
        appRemoverPref?.setOnPreferenceClickListener {
            val intent = Intent(context, GameBarAppRemoverActivity::class.java)
            startActivity(intent)
            true
        }

        mMasterSwitch?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            if (enabled) {
                if (Settings.canDrawOverlays(context)) {
                    mGameBar.applyPreferences()
                    mGameBar.show()
                    context?.startService(Intent(context, GameBarMonitorService::class.java))
                } else {
                    Toast.makeText(context, R.string.overlay_permission_required, Toast.LENGTH_SHORT).show()
                    return@setOnPreferenceChangeListener false
                }
            } else {
                mGameBar.hide()
                if (mAutoEnableSwitch == null || !mAutoEnableSwitch!!.isChecked) {
                    context?.stopService(Intent(context, GameBarMonitorService::class.java))
                }
            }
            true
        }

        mAutoEnableSwitch?.setOnPreferenceChangeListener { _, newValue ->
            val autoEnabled = newValue as Boolean
            if (autoEnabled) {
                context?.startService(Intent(context, GameBarMonitorService::class.java))
            } else {
                if (mMasterSwitch == null || !mMasterSwitch!!.isChecked) {
                    context?.stopService(Intent(context, GameBarMonitorService::class.java))
                }
            }
            true
        }

        mFpsSwitch?.setOnPreferenceChangeListener { _, newValue ->
            mGameBar.setShowFps((newValue as Boolean))
            true
        }
        mBatteryTempSwitch?.setOnPreferenceChangeListener { _, newValue ->
            mGameBar.setShowBatteryTemp((newValue as Boolean))
            true
        }
        mCpuUsageSwitch?.setOnPreferenceChangeListener { _, newValue ->
            mGameBar.setShowCpuUsage((newValue as Boolean))
            true
        }
        mCpuClockSwitch?.setOnPreferenceChangeListener { _, newValue ->
            mGameBar.setShowCpuClock((newValue as Boolean))
            true
        }
        mCpuTempSwitch?.setOnPreferenceChangeListener { _, newValue ->
            mGameBar.setShowCpuTemp((newValue as Boolean))
            true
        }
        mRamSwitch?.setOnPreferenceChangeListener { _, newValue ->
            mGameBar.setShowRam((newValue as Boolean))
            true
        }
        mCaptureStartPref?.setOnPreferenceClickListener {
            GameDataExport.getInstance().startCapture()
            Toast.makeText(context, "Started logging Data", Toast.LENGTH_SHORT).show()
            true
        }
        mCaptureStopPref?.setOnPreferenceClickListener {
            GameDataExport.getInstance().stopCapture()
            Toast.makeText(context, "Stopped logging Data", Toast.LENGTH_SHORT).show()
            true
        }
        mCaptureExportPref?.setOnPreferenceClickListener {
            GameDataExport.getInstance().exportDataToCsv()
            Toast.makeText(context, "Exported log data to file", Toast.LENGTH_SHORT).show()
            true
        }
        mDoubleTapCapturePref?.setOnPreferenceChangeListener { _, newValue ->
            mGameBar.setDoubleTapCaptureEnabled((newValue as Boolean))
            true
        }
        mSingleTapTogglePref?.setOnPreferenceChangeListener { _, newValue ->
            mGameBar.setSingleTapToggleEnabled((newValue as Boolean))
            true
        }
        mLongPressEnablePref?.setOnPreferenceChangeListener { _, newValue ->
            mGameBar.setLongPressEnabled((newValue as Boolean))
            true
        }
        mLongPressTimeoutPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is String) {
                val ms = java.lang.Long.parseLong(newValue)
                mGameBar.setLongPressThresholdMs(ms)
            }
            true
        }
        mTextSizePref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is Int) {
                mGameBar.updateTextSize(newValue)
            }
            true
        }
        mBgAlphaPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is Int) {
                mGameBar.updateBackgroundAlpha(newValue)
            }
            true
        }
        mCornerRadiusPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is Int) {
                mGameBar.updateCornerRadius(newValue)
            }
            true
        }
        mPaddingPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is Int) {
                mGameBar.updatePadding(newValue)
            }
            true
        }
        mItemSpacingPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is Int) {
                mGameBar.updateItemSpacing(newValue)
            }
            true
        }
        mUpdateIntervalPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is String) {
                mGameBar.updateUpdateInterval(newValue)
            }
            true
        }
        mTextColorPref?.setOnPreferenceChangeListener { _, _ -> true }
        mTitleColorPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is String) {
                mGameBar.updateTitleColor(newValue)
            }
            true
        }
        mValueColorPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is String) {
                mGameBar.updateValueColor(newValue)
            }
            true
        }
        mPositionPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is String) {
                mGameBar.updatePosition(newValue)
            }
            true
        }
        mSplitModePref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is String) {
                mGameBar.updateSplitMode(newValue)
            }
            true
        }
        mOverlayFormatPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is String) {
                mGameBar.updateOverlayFormat(newValue)
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        if (!hasUsageStatsPermission(requireContext())) {
            requestUsageStatsPermission()
        }
        val context = context
        if (context != null) {
            if ((mMasterSwitch != null && mMasterSwitch!!.isChecked) ||
                (mAutoEnableSwitch != null && mAutoEnableSwitch!!.isChecked)
            ) {
                context.startService(Intent(context, GameBarMonitorService::class.java))
            } else {
                context.stopService(Intent(context, GameBarMonitorService::class.java))
            }
        }
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager?
            ?: return false
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }
}
