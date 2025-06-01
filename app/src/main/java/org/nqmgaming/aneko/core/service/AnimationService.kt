package org.nqmgaming.aneko.core.service

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.nqmgaming.aneko.data.skin.MotionState
import java.util.prefs.PreferenceChangeListener
import kotlin.random.Random
import androidx.core.net.toUri

class AnimationService : Service() {
    companion object {
        const val ACTION_START: String = "org.nqmgaming.aneko.action.START"
        const val ACTION_STOP: String = "org.nqmgaming.aneko.action.STOP"
        const val ACTION_TOGGLE: String = "org.nqmgaming.aneko.action.TOGGLE"
        const val ACTION_GET_SKIN: String = "org.tamanegi.aneko.action.GET_SKIN"
        const val META_KEY_SKIN: String = "org.tamanegi.aneko.skin"
        const val PREF_KEY_ENABLE: String = "motion.enable"
        const val PREF_KEY_VISIBLE: String = "motion.visible"
        const val PREF_KEY_TRANSPARENCY: String = "motion.transparency"
        const val PREF_KEY_SIZE: String = "motion.size"
        const val PREF_KEY_SPEED: String = "motion.speed"
        const val PREF_KEY_BEHAVIOUR: String = "motion.behaviour"
        const val PREF_KEY_SKIN_COMPONENT: String = "motion.skin"
        const val PREF_KEY_KEEP_ALIVE: String = "motion.keep_alive"
        const val PREF_KEY_NOTIFICATION_ENABLE: String = "notification.enable"
        const val PREF_KEY_ENABLED_APPS: String = "enabled_apps"
        private const val MSG_ANIMATE = 1
        private const val ANIMATION_INTERVAL = 125L // msec
        private const val BEHAVIOUR_CHANGE_DURATION = 4000L // msec
        const val ANEKO_SKINS = "/ANeko/skins"

        private val BEHAVIOURS = arrayOf(
            Behaviour.CLOSER, Behaviour.FURTHER, Behaviour.WHIMSICAL
        )

        // This was hardcoded to true in Java. If it's meant to check OS version,
        // it should be Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH
        // For a direct conversion of the hardcoded value:
        private const val ICS_OR_LATER = true
    }

    private enum class Behaviour {
        CLOSER, FURTHER, WHIMSICAL
    }

    private var imageWidth = 80
    private var imageHeight = 80

    private var isStarted = false
    private lateinit var prefs: SharedPreferences
    private lateinit var prefListener: PreferenceChangeListener
    private lateinit var job: Job
    private var motionState: MotionState? = null
    private lateinit var random: Random // Using Kotlin's Random

    private var touchView: View? = null
    private var imageView: ImageView? = null
    private var imageParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        isStarted = true
        job = CoroutineScope(Dispatchers.Main).launch {

        }
        random = Random.Default
        prefs = getSharedPreferences("${packageName}_preferences", MODE_PRIVATE)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            val overlayIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(overlayIntent)
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_START, null -> {
                if (!isStarted) {
                    startAnimation()
                    setForegroundNotification(true)
                    isStarted = true
                } else {
                    stopAnimation()
                    startAnimation()
                    setForegroundNotification(true)
                }
            }
            ACTION_TOGGLE -> {
                toggleAnimation()
            }
            ACTION_STOP -> {
                if (isStarted) {
                    stopAnimation()
                    stopSelfResult(startId)
                    setForegroundNotification(false)
                    isStarted = false
                }
            }
        }
        return START_REDELIVER_INTENT
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!isStarted || motionState == null) return

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()
        wm.currentWindowMetrics?.let {
            outMetrics.setTo(it.bounds)
        } ?: run {
            wm.defaultDisplay.getMetrics(outMetrics)
        }
    }

}