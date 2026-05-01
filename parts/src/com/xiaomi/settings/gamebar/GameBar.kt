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


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.xiaomi.settings.R
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale

class GameBar private constructor(context: Context) {

    private val mContext: Context = context
    private val mWindowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mHandler: Handler = Handler(Looper.getMainLooper())

    private var mOverlayView: View? = null
    private var mRootLayout: LinearLayout? = null
    private var mLayoutParams: WindowManager.LayoutParams? = null
    private var mIsShowing = false

    private var mTextSizeSp = 16
    private var mBackgroundAlpha = 128
    private var mCornerRadius = 16
    private var mPaddingDp = 12
    private var mTitleColorHex = "#FFFFFF"
    private var mValueColorHex = "#FFFFFF"
    private var mOverlayFormat = "full"
    private var mPosition = "top_left"
    private var mSplitMode = "stacked"
    private var mUpdateIntervalMs = 1000
    private var mDraggable = false

    var isShowBatteryTemp: Boolean = false
        private set
    var isShowCpuUsage: Boolean = false
        private set
    var isShowCpuClock: Boolean = false
        private set
    var isShowCpuTemp: Boolean = false
        private set
    var isShowRam: Boolean = false
        private set
    var isShowFps: Boolean = false
        private set

    private var mLongPressEnabled = false
    private var mLongPressThresholdMs: Long = 1000
    private var mPressActive = false
    private var mDownX: Float = 0f
    private var mDownY: Float = 0f

    private var mGestureDetector: GestureDetector? = null
    private var mDoubleTapCaptureEnabled = false
    private var mSingleTapToggleEnabled = false
    private var mBgDrawable: GradientDrawable? = null

    private var mItemSpacingDp = 8

    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f

    private var mBatteryTemp: Float = 0f

    private val mLongPressRunnable = Runnable {
        if (mPressActive) {
            openOverlaySettings()
            mPressActive = false
        }
    }

    private val mUpdateRunnable = object : Runnable {
        override fun run() {
            if (mIsShowing) {
                updateStats()
                mHandler.postDelayed(this, mUpdateIntervalMs.toLong())
            }
        }
    }

    private val mBatteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Intent.ACTION_BATTERY_CHANGED == intent.action) {
                val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                mBatteryTemp = tempTenths / 10f
            }
        }
    }

    init {
        mBgDrawable = GradientDrawable()
        applyBackgroundStyle()

        mGestureDetector = GestureDetector(mContext, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (mDoubleTapCaptureEnabled) {
                    if (GameDataExport.getInstance().isCapturing) {
                        GameDataExport.getInstance().stopCapture()
                        Toast.makeText(mContext, "Capture Stopped", Toast.LENGTH_SHORT).show()
                    } else {
                        GameDataExport.getInstance().startCapture()
                        Toast.makeText(mContext, "Capture Started", Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
                return super.onDoubleTap(e)
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (mSingleTapToggleEnabled) {
                    mOverlayFormat = if ("full" == mOverlayFormat) "minimal" else "full"
                    PreferenceManager.getDefaultSharedPreferences(mContext)
                        .edit()
                        .putString("game_bar_format", mOverlayFormat)
                        .apply()
                    Toast.makeText(mContext, "Overlay Format: $mOverlayFormat", Toast.LENGTH_SHORT).show()
                    updateStats()
                    return true
                }
                return super.onSingleTapConfirmed(e)
            }
        })
    }

    fun applyPreferences() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(mContext)

        isShowFps = prefs.getBoolean("game_bar_fps_enable", false)
        isShowBatteryTemp = prefs.getBoolean("game_bar_temp_enable", false)
        isShowCpuUsage = prefs.getBoolean("game_bar_cpu_usage_enable", false)
        isShowCpuClock = prefs.getBoolean("game_bar_cpu_clock_enable", false)
        isShowCpuTemp = prefs.getBoolean("game_bar_cpu_temp_enable", false)
        isShowRam = prefs.getBoolean("game_bar_ram_enable", false)

        mDoubleTapCaptureEnabled = prefs.getBoolean("game_bar_doubletap_capture", false)
        mSingleTapToggleEnabled = prefs.getBoolean("game_bar_single_tap_toggle", false)

        updateSplitMode(prefs.getString("game_bar_split_mode", "stacked")!!)
        updateTextSize(prefs.getInt("game_bar_text_size", 16))
        updateBackgroundAlpha(prefs.getInt("game_bar_background_alpha", 128))
        updateCornerRadius(prefs.getInt("game_bar_corner_radius", 16))
        updatePadding(prefs.getInt("game_bar_padding", 12))
        updateTitleColor(prefs.getString("game_bar_title_color", "#FFFFFF")!!)
        updateValueColor(prefs.getString("game_bar_value_color", "#4CAF50")!!)
        updateOverlayFormat(prefs.getString("game_bar_format", "full")!!)
        updateUpdateInterval(prefs.getString("game_bar_update_interval", "1000")!!)
        updatePosition(prefs.getString("game_bar_position", "top_left")!!)

        val spacing = prefs.getInt("game_bar_item_spacing", 8)
        updateItemSpacing(spacing)

        mLongPressEnabled = prefs.getBoolean("game_bar_longpress_enable", false)
        val lpTimeoutStr = prefs.getString("game_bar_longpress_timeout", "1000")
        try {
            val lpt = java.lang.Long.parseLong(lpTimeoutStr!!)
            setLongPressThresholdMs(lpt)
        } catch (ignored: NumberFormatException) {
        }
    }

    fun show() {
        if (mIsShowing) return

        applyPreferences()

        mLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        if ("draggable" == mPosition) {
            mDraggable = true
            loadSavedPosition(mLayoutParams!!)
            if (mLayoutParams!!.x == 0 && mLayoutParams!!.y == 0) {
                mLayoutParams!!.gravity = Gravity.TOP or Gravity.START
                mLayoutParams!!.x = 0
                mLayoutParams!!.y = 100
            }
        } else {
            mDraggable = false
            applyPosition(mLayoutParams!!, mPosition)
        }

        mOverlayView = LinearLayout(mContext)
        mOverlayView!!.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        mRootLayout = mOverlayView as LinearLayout?
        applySplitMode()
        applyBackgroundStyle()
        applyPadding()

        mOverlayView!!.setOnTouchListener { v, event ->
            if (mGestureDetector != null && mGestureDetector!!.onTouchEvent(event)) {
                return@setOnTouchListener true
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (mDraggable) {
                        initialX = mLayoutParams!!.x
                        initialY = mLayoutParams!!.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                    }
                    if (mLongPressEnabled) {
                        mPressActive = true
                        mDownX = event.rawX
                        mDownY = event.rawY
                        mHandler.postDelayed(mLongPressRunnable, mLongPressThresholdMs)
                    }
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (mLongPressEnabled && mPressActive) {
                        val dx = Math.abs(event.rawX - mDownX)
                        val dy = Math.abs(event.rawY - mDownY)
                        if (dx > TOUCH_SLOP || dy > TOUCH_SLOP) {
                            mPressActive = false
                            mHandler.removeCallbacks(mLongPressRunnable)
                        }
                    }
                    if (mDraggable) {
                        val deltaX = (event.rawX - initialTouchX).toInt()
                        val deltaY = (event.rawY - initialTouchY).toInt()
                        mLayoutParams!!.x = initialX + deltaX
                        mLayoutParams!!.y = initialY + deltaY
                        mWindowManager.updateViewLayout(mOverlayView, mLayoutParams)
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (mLongPressEnabled && mPressActive) {
                        mPressActive = false
                        mHandler.removeCallbacks(mLongPressRunnable)
                    }
                    if (mDraggable) {
                        val prefs = PreferenceManager.getDefaultSharedPreferences(mContext)
                        prefs.edit()
                            .putInt(PREF_KEY_X, mLayoutParams!!.x)
                            .putInt(PREF_KEY_Y, mLayoutParams!!.y)
                            .apply()
                    }
                    true
                }

                else -> false
            }
        }

        mWindowManager.addView(mOverlayView, mLayoutParams)
        mIsShowing = true
        
        // Register Battery Receiver
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = mContext.registerReceiver(mBatteryReceiver, filter)
        // Get initial value immediately
        if (intent != null) {
            val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            mBatteryTemp = tempTenths / 10f
        }

        startUpdates()

        // Start the FPS meter if using the new API method.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            GameBarFpsMeter.getInstance(mContext).start()
        }
    }

    fun hide() {
        if (!mIsShowing) return
        mHandler.removeCallbacksAndMessages(null)
        if (mOverlayView != null) {
            mWindowManager.removeView(mOverlayView)
            mOverlayView = null
        }
        
        // Unregister Battery Receiver
        try {
            mContext.unregisterReceiver(mBatteryReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }

        mIsShowing = false
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            GameBarFpsMeter.getInstance(mContext).stop()
        }
    }

    private fun updateStats() {
        if (!mIsShowing || mRootLayout == null) return

        mRootLayout!!.removeAllViews()

        val statViews = ArrayList<View>()

        // 1) FPS
        val fpsVal = GameBarFpsMeter.getInstance(mContext).fps
        val fpsStr = if (fpsVal >= 0) String.format(Locale.getDefault(), "%.0f", fpsVal) else "N/A"
        if (isShowFps) {
            statViews.add(createStatLine("FPS", fpsStr))
        }

        // 2) Battery temp
        var batteryTempStr = "N/A"
        if (isShowBatteryTemp) {
            batteryTempStr = String.format(Locale.getDefault(), "%.1f", mBatteryTemp)
            statViews.add(createStatLine("Temp", "$batteryTempStr°C"))
        }

        // 3) CPU usage
        var cpuUsageStr = "N/A"
        if (isShowCpuUsage) {
            cpuUsageStr = GameBarCpuInfo.cpuUsage
            val display = if ("N/A" == cpuUsageStr) "N/A" else "$cpuUsageStr%"
            statViews.add(createStatLine("CPU", display))
        }

        // 4) CPU freq
        if (isShowCpuClock) {
            val freqs = GameBarCpuInfo.cpuFrequencies
            if (!freqs.isEmpty()) {
                statViews.add(buildCpuFreqView(freqs))
            }
        }

        // 5) CPU temp
        var cpuTempStr = "N/A"
        if (isShowCpuTemp) {
            cpuTempStr = GameBarCpuInfo.cpuTemp
            statViews.add(createStatLine("CPU Temp", if ("N/A" == cpuTempStr) "N/A" else "$cpuTempStr°C"))
        }

        // 6) RAM usage
        var ramStr = "N/A"
        if (isShowRam) {
            ramStr = GameBarMemInfo.ramUsage
            statViews.add(createStatLine("RAM", if ("N/A" == ramStr) "N/A" else "$ramStr MB"))
        }

        if ("side_by_side" == mSplitMode) {
            mRootLayout!!.orientation = LinearLayout.HORIZONTAL
            if ("minimal" == mOverlayFormat) {
                for (i in statViews.indices) {
                    mRootLayout!!.addView(statViews[i])
                    if (i < statViews.size - 1) {
                        mRootLayout!!.addView(createDotView())
                    }
                }
            } else {
                for (view in statViews) {
                    mRootLayout!!.addView(view)
                }
            }
        } else {
            mRootLayout!!.orientation = LinearLayout.VERTICAL
            for (view in statViews) {
                mRootLayout!!.addView(view)
            }
        }

        if (GameDataExport.getInstance().isCapturing) {
            val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val pkgName = ForegroundAppDetector.getForegroundPackageName(mContext)

            GameDataExport.getInstance().addOverlayData(
                dateTime,
                pkgName,
                fpsStr,
                batteryTempStr,
                cpuUsageStr,
                cpuTempStr
            )
        }

        if (mLayoutParams != null) {
            mWindowManager.updateViewLayout(mOverlayView, mLayoutParams)
        }
    }

    private fun buildCpuFreqView(freqs: List<String>): View {
        val freqContainer = LinearLayout(mContext)
        freqContainer.orientation = LinearLayout.HORIZONTAL

        val spacingPx = dpToPx(mContext, mItemSpacingDp)
        val outerLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        outerLp.setMargins(spacingPx, spacingPx / 2, spacingPx, spacingPx / 2)
        freqContainer.layoutParams = outerLp

        if ("full" == mOverlayFormat) {
            val labelTv = TextView(mContext)
            labelTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextSizeSp.toFloat())
            try {
                labelTv.setTextColor(Color.parseColor(mTitleColorHex))
            } catch (e: Exception) {
                labelTv.setTextColor(Color.WHITE)
            }
            labelTv.text = "CPU Freq "
            freqContainer.addView(labelTv)
        }

        val verticalFreqs = LinearLayout(mContext)
        verticalFreqs.orientation = LinearLayout.VERTICAL

        for (freqLine in freqs) {
            val lineLayout = LinearLayout(mContext)
            lineLayout.orientation = LinearLayout.HORIZONTAL

            val freqTv = TextView(mContext)
            freqTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextSizeSp.toFloat())
            try {
                freqTv.setTextColor(Color.parseColor(mValueColorHex))
            } catch (e: Exception) {
                freqTv.setTextColor(Color.WHITE)
            }
            freqTv.text = freqLine

            lineLayout.addView(freqTv)

            val lineLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lineLp.setMargins(spacingPx, spacingPx / 4, spacingPx, spacingPx / 4)
            lineLayout.layoutParams = lineLp

            verticalFreqs.addView(lineLayout)
        }

        freqContainer.addView(verticalFreqs)
        return freqContainer
    }

    private fun createStatLine(title: String, rawValue: String): LinearLayout {
        val lineLayout = LinearLayout(mContext)
        lineLayout.orientation = LinearLayout.HORIZONTAL

        if ("full" == mOverlayFormat) {
            val tvTitle = TextView(mContext)
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextSizeSp.toFloat())
            try {
                tvTitle.setTextColor(Color.parseColor(mTitleColorHex))
            } catch (e: Exception) {
                tvTitle.setTextColor(Color.WHITE)
            }
            tvTitle.text = if (title.isEmpty()) "" else "$title "

            val tvValue = TextView(mContext)
            tvValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextSizeSp.toFloat())
            try {
                tvValue.setTextColor(Color.parseColor(mValueColorHex))
            } catch (e: Exception) {
                tvValue.setTextColor(Color.WHITE)
            }
            tvValue.text = rawValue

            lineLayout.addView(tvTitle)
            lineLayout.addView(tvValue)
        } else {
            val tvMinimal = TextView(mContext)
            tvMinimal.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextSizeSp.toFloat())
            try {
                tvMinimal.setTextColor(Color.parseColor(mValueColorHex))
            } catch (e: Exception) {
                tvMinimal.setTextColor(Color.WHITE)
            }
            tvMinimal.text = rawValue
            lineLayout.addView(tvMinimal)
        }

        val spacingPx = dpToPx(mContext, mItemSpacingDp)
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(spacingPx, spacingPx / 2, spacingPx, spacingPx / 2)
        lineLayout.layoutParams = lp

        return lineLayout
    }

    private fun createDotView(): View {
        val dotView = TextView(mContext)
        dotView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextSizeSp.toFloat())
        try {
            dotView.setTextColor(Color.parseColor(mValueColorHex))
        } catch (e: Exception) {
            dotView.setTextColor(Color.WHITE)
        }
        dotView.text = " . "
        return dotView
    }

    fun setShowBatteryTemp(show: Boolean) {
        isShowBatteryTemp = show
    }

    fun setShowCpuUsage(show: Boolean) {
        isShowCpuUsage = show
    }

    fun setShowCpuClock(show: Boolean) {
        isShowCpuClock = show
    }

    fun setShowCpuTemp(show: Boolean) {
        isShowCpuTemp = show
    }

    fun setShowRam(show: Boolean) {
        isShowRam = show
    }

    fun setShowFps(show: Boolean) {
        isShowFps = show
    }

    fun updateTextSize(sp: Int) {
        mTextSizeSp = sp
    }

    fun updateCornerRadius(radius: Int) {
        mCornerRadius = radius
        applyBackgroundStyle()
    }

    fun updateBackgroundAlpha(alpha: Int) {
        mBackgroundAlpha = alpha
        applyBackgroundStyle()
    }

    fun updatePadding(dp: Int) {
        mPaddingDp = dp
        applyPadding()
    }

    fun updateTitleColor(hex: String) {
        mTitleColorHex = hex
    }

    fun updateValueColor(hex: String) {
        mValueColorHex = hex
    }

    fun updateOverlayFormat(format: String) {
        mOverlayFormat = format
        if (mIsShowing) {
            updateStats()
        }
    }

    fun updateItemSpacing(dp: Int) {
        mItemSpacingDp = dp
        if (mIsShowing) {
            updateStats()
        }
    }

    private fun applyBackgroundStyle() {
        val color = Color.argb(mBackgroundAlpha, 0, 0, 0)
        mBgDrawable!!.setColor(color)
        mBgDrawable!!.cornerRadius = mCornerRadius.toFloat()

        if (mOverlayView != null) {
            mOverlayView!!.background = mBgDrawable
        }
    }

    private fun applyPadding() {
        if (mRootLayout != null) {
            val px = dpToPx(mContext, mPaddingDp)
            mRootLayout!!.setPadding(px, px, px, px)
        }
    }

    fun updatePosition(pos: String) {
        mPosition = pos
        if (mIsShowing && mOverlayView != null && mLayoutParams != null) {
            if ("draggable" == mPosition) {
                mDraggable = true
                loadSavedPosition(mLayoutParams!!)
                if (mLayoutParams!!.x == 0 && mLayoutParams!!.y == 0) {
                    mLayoutParams!!.gravity = Gravity.TOP or Gravity.START
                    mLayoutParams!!.x = 0
                    mLayoutParams!!.y = 100
                }
            } else {
                mDraggable = false
                applyPosition(mLayoutParams!!, mPosition)
            }
            mWindowManager.updateViewLayout(mOverlayView, mLayoutParams)
        }
    }

    fun updateSplitMode(mode: String) {
        mSplitMode = mode
        if (mIsShowing && mOverlayView != null) {
            applySplitMode()
            updateStats()
        }
    }

    fun updateUpdateInterval(intervalStr: String) {
        try {
            mUpdateIntervalMs = Integer.parseInt(intervalStr)
        } catch (e: NumberFormatException) {
            mUpdateIntervalMs = 1000
        }
        if (mIsShowing) {
            startUpdates()
        }
    }

    fun setLongPressEnabled(enabled: Boolean) {
        mLongPressEnabled = enabled
    }

    fun setLongPressThresholdMs(ms: Long) {
        mLongPressThresholdMs = ms
    }

    fun setDoubleTapCaptureEnabled(enabled: Boolean) {
        mDoubleTapCaptureEnabled = enabled
    }

    fun setSingleTapToggleEnabled(enabled: Boolean) {
        mSingleTapToggleEnabled = enabled
    }

    private fun startUpdates() {
        mHandler.removeCallbacksAndMessages(null)
        mHandler.post(mUpdateRunnable)
    }

    private fun applySplitMode() {
        if (mRootLayout == null) return
        if ("side_by_side" == mSplitMode) {
            mRootLayout!!.orientation = LinearLayout.HORIZONTAL
        } else {
            mRootLayout!!.orientation = LinearLayout.VERTICAL
        }
    }

    private fun loadSavedPosition(lp: WindowManager.LayoutParams) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(mContext)
        val savedX = prefs.getInt(PREF_KEY_X, Int.MIN_VALUE)
        val savedY = prefs.getInt(PREF_KEY_Y, Int.MIN_VALUE)
        if (savedX != Int.MIN_VALUE && savedY != Int.MIN_VALUE) {
            lp.gravity = Gravity.TOP or Gravity.START
            lp.x = savedX
            lp.y = savedY
        }
    }

    private fun applyPosition(lp: WindowManager.LayoutParams, pos: String) {
        when (pos) {
            "top_left" -> {
                lp.gravity = Gravity.TOP or Gravity.START
                lp.x = 0
                lp.y = 100
            }

            "top_center" -> {
                lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                lp.y = 100
            }

            "top_right" -> {
                lp.gravity = Gravity.TOP or Gravity.END
                lp.x = 0
                lp.y = 100
            }

            "bottom_left" -> {
                lp.gravity = Gravity.BOTTOM or Gravity.START
                lp.x = 0
                lp.y = 100
            }

            "bottom_center" -> {
                lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                lp.y = 100
            }

            "bottom_right" -> {
                lp.gravity = Gravity.BOTTOM or Gravity.END
                lp.x = 0
                lp.y = 100
            }

            else -> {
                lp.gravity = Gravity.TOP or Gravity.START
                lp.x = 0
                lp.y = 100
            }
        }
    }

    private fun openOverlaySettings() {
        try {
            val intent = Intent(mContext, GameBarSettingsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mContext.startActivity(intent)
        } catch (e: Exception) {
            // Exception ignored
        }
    }

    companion object {

        private var sInstance: GameBar? = null
        @Synchronized
        fun getInstance(context: Context): GameBar {
            return sInstance ?: GameBar(context.applicationContext).also { sInstance = it }
        }

        private const val FPS_PATH = "/sys/class/drm/sde-crtc-0/measured_fps"
        private const val PREF_KEY_X = "game_bar_x"
        private const val PREF_KEY_Y = "game_bar_y"

        private const val TOUCH_SLOP = 20f

        private fun dpToPx(context: Context, dp: Int): Int {
            val scale = context.resources.displayMetrics.density
            return Math.round(dp * scale)
        }
    }
}
