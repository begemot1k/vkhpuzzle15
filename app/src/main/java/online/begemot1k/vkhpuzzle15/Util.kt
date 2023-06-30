package online.begemot1k.vkhpuzzle15

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.annotation.RequiresApi

class Util {
    companion object {
        fun time2string(milliseconds: Long): String {
            val hours = milliseconds / 1000 / 3600
            val minutes = milliseconds / 1000 / 3600
            val seconds = milliseconds / 1000
            return "%02d:%02d:%02d".format(hours, minutes, seconds)
        }
        @RequiresApi(Build.VERSION_CODES.S)
        fun vibrate(context: Context, heavy: Boolean = false) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            if (heavy) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            }
        }
    }
}
