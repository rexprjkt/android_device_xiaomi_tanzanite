/*
 * Copyright (C) 2023-2026 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.battery

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import com.android.settingslib.widget.MainSwitchPreference
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import com.xiaomi.settings.R

class ChargingControlFragment : SettingsBasePreferenceFragment(),
    Preference.OnPreferenceChangeListener {

    private var mLimitPref: SeekBarPreference? = null
    private var mMasterSwitch: MainSwitchPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.charging_control_settings)

        mLimitPref = findPreference<SeekBarPreference>(BatteryUtils.PREF_CHARGING_LIMIT)?.apply {
            onPreferenceChangeListener = this@ChargingControlFragment
            updateSummary(value)
        }

        mMasterSwitch = findPreference<MainSwitchPreference>(BatteryUtils.PREF_CHARGING_CTRL)?.apply {
            onPreferenceChangeListener = this@ChargingControlFragment
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        var isEnabled = mMasterSwitch?.isChecked ?: false
        var limit = mLimitPref?.value ?: 80

        if (preference == mMasterSwitch) {
            isEnabled = newValue as Boolean
        } else if (preference == mLimitPref) {
            limit = newValue as Int
            updateSummary(limit)
        }

        val currentLevel = getBatteryLevel()
        BatteryUtils.setChargingSuspendAsync(isEnabled && currentLevel >= limit)

        context?.let { ctx ->
            ctx.startService(Intent(ctx, ChargingLimitService::class.java))
        }
        return true
    }

    private fun getBatteryLevel(): Int {
        val bm = context?.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        return bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
    }

    private fun updateSummary(value: Int) {
        mLimitPref?.summary = getString(R.string.charging_limit_summary, value)
    }

    override fun onDestroyView() {
        mLimitPref?.onPreferenceChangeListener = null
        mMasterSwitch?.onPreferenceChangeListener = null
        mLimitPref = null
        mMasterSwitch = null
        super.onDestroyView()
    }
}
