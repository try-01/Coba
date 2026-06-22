package com.tvhanan.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

object HapticUtil {

    private var vibrator: Vibrator? = null
    private var enabled: Boolean = true

    fun init(context: Context) {
        @Suppress("DEPRECATION")
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun setEnabled(value: Boolean) {
        enabled = value
    }

    fun tick() {
        if (!enabled) return
        val v = vibrator ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(30)
        }
    }
}
