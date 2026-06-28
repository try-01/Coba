package com.tvhanan.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

object HapticUtil {

    private var vibrator: Vibrator? = null
    var isEnabled: Boolean = true // Flag kontrol dinamis dari Settings

    fun init(context: Context) {
        @Suppress("DEPRECATION")
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun tick() {
        val v = vibrator ?: return
        if (!isEnabled) return // Jangan bergetar jika dimatikan di menu pengaturan

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // EFFECT_CLICK memberikan respons ketukan fisik yang jauh lebih mantap & instan dibanding TICK
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(25) // Getaran singkat yang pas untuk Android versi lama
        }
    }
}
