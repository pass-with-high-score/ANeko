package org.nqmgaming.aneko.core.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.PointF
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.provider.Settings
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.annotation.NonNull
import androidx.core.net.toUri
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.motion.MotionConfigParser
import org.nqmgaming.aneko.core.motion.MotionDrawable
import org.nqmgaming.aneko.core.motion.MotionParams
import timber.log.Timber
import java.io.File
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class Behaviour {
    closer, further, whimsical
}

class AnimationService : Service() {

    companion object {
        const val ACTION_START = "org.nqmgaming.aneko.action.START"
        const val ACTION_STOP = "org.nqmgaming.aneko.action.STOP"
        const val ACTION_TOGGLE = "org.nqmgaming.aneko.action.TOGGLE"
        const val ACTION_GET_SKIN = "org.tamanegi.aneko.action.GET_SKIN"

        const val META_KEY_SKIN = "org.tamanegi.aneko.skin"
        const val PREF_KEY_ENABLE = "motion.enable"
        const val PREF_KEY_VISIBLE = "motion.visible"
        const val PREF_KEY_TRANSPARENCY = "motion.transparency"
        const val PREF_KEY_SIZE = "motion.size"
        const val PREF_KEY_SPEED = "motion.speed"
        const val PREF_KEY_BEHAVIOUR = "motion.behaviour"
        const val PREF_KEY_SKIN_COMPONENT = "motion.skin"
        const val PREF_KEY_NOTIFICATION_ENABLE = "notification.enable"

        private const val MSG_ANIMATE = 1
        private const val ANIMATION_INTERVAL = 125L // msec
        private const val BEHAVIOUR_CHANGE_DURATION = 4000L // msec

        // Thư mục skins trong internal storage
        private const val SKINS_DIR_NAME = "skins"
    }

    private enum class Behaviour { closer, further, whimsical }

    private var imageWidth = 80
    private var imageHeight = 80

    private var isStarted = false
    private lateinit var prefs: SharedPreferences
    private var prefListener: PreferenceChangeListener? = null
    private val handler = Handler(Looper.getMainLooper(), ::onHandleMessage)
    private var motionState: MotionState? = null
    private val random = java.util.Random()

    private var touchView: View? = null
    private var imageView: ImageView? = null
    private var imageParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        isStarted = false
        prefs = getSharedPreferences(packageName + "_preferences", MODE_PRIVATE)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            val overlayIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(overlayIntent)
            return START_NOT_STICKY
        }

        when {
            !isStarted && (intent == null || ACTION_START == intent.action) -> {
                if (isStarted) stopAnimation()
                startAnimation()
                setForegroundNotification(true)
                isStarted = true
            }

            ACTION_TOGGLE == intent?.action -> toggleAnimation()
            isStarted && ACTION_STOP == intent?.action -> {
                stopAnimation()
                stopSelfResult(startId)
                setForegroundNotification(false)
                isStarted = false
            }
        }
        return START_REDELIVER_INTENT
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val display: Display = wm.defaultDisplay
        val size = Point().also { display.getSize(it) }
        motionState?.setDisplaySize(size.x, size.y)
    }

    private fun startAnimation() {
        prefListener = PreferenceChangeListener().also {
            prefs.registerOnSharedPreferenceChangeListener(it)
        }

        if (checkPrefEnable() && loadMotionState()) {
            refreshMotionSpeed()

            val wm = getSystemService(WINDOW_SERVICE) as WindowManager

            touchView = View(this).apply {
                setOnTouchListener(TouchListener())
            }
            val touchParams = WindowManager.LayoutParams(
                1, 1,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.CENTER }
            wm.addView(touchView, touchParams)

            imageView = ImageView(this)
            imageParams = WindowManager.LayoutParams(
                imageWidth,
                imageHeight,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.TOP or Gravity.START }
            wm.addView(imageView, imageParams)

            requestAnimate()
        }
    }

    private fun stopAnimation() {
        prefListener?.let { prefs.unregisterOnSharedPreferenceChangeListener(it) }
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager

        touchView?.let { wm.removeView(it) }
        imageView?.let { wm.removeView(it) }

        motionState = null
        touchView = null
        imageView = null

        handler.removeMessages(MSG_ANIMATE)
    }

    private fun toggleAnimation() {
        val visible = prefs.getBoolean(PREF_KEY_VISIBLE, true)
        prefs.edit().putBoolean(PREF_KEY_VISIBLE, !visible).apply()
        startService(Intent(this, AnimationService::class.java).setAction(ACTION_START))
    }

    private fun setForegroundNotification(start: Boolean) {
        val intent = PendingIntent.getService(
            this, 0,
            Intent(this, AnimationService::class.java).setAction(ACTION_TOGGLE),
            PendingIntent.FLAG_IMMUTABLE
        )

        val channel = NotificationChannel(
            getString(R.string.app_name),
            getString(R.string.aneko_notification_channel_name),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = getString(R.string.notification_chanel_description)
        }
        (getSystemService(NOTIFICATION_SERVICE) as? NotificationManager)
            ?.createNotificationChannel(channel)

        val builder = Notification.Builder(this, getString(R.string.app_name))
            .setContentIntent(intent)
            .setSmallIcon(if (start) R.drawable.right2 else R.drawable.sleep2)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(if (start) R.string.notification_enabled else R.string.notification_disabled))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setAutoCancel(false)

        val notify = builder.build()
        stopForeground(true)
        if (start) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1, notify, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(1, notify)
            }
            return
        }

        if (prefs.getBoolean(PREF_KEY_ENABLE, true)) {
            (getSystemService(NOTIFICATION_SERVICE) as? NotificationManager)?.notify(1, notify)
        }
    }

    // ================== Load Motion ==================

    private fun loadMotionState(): Boolean {
        prefs.getString(PREF_KEY_SKIN_COMPONENT, "") ?: ""
        return loadMotionDir()
    }

    /**
     * Đọc từ INTERNAL: files/skins/<folder>/skin.xml (+ ảnh)
     * Giá trị prefs "motion.skin" có thể là "<folder>/skin.xml" hoặc "<folder>"
     */
    private fun loadMotionDir(): Boolean {
        val skinPath = "com.nqmgaming.caza"
        val loaded = try {
            val params = getMotionParamsInternal(skinPath)
            motionState = MotionState().apply { setParams(params) }
            true
        } catch (e: Exception) {
            Timber.e(e)
            false
        }

        if (loaded) {
            afterMotionLoaded()
            return true
        }
        return false
    }

    /**
     * Build path trong internal storage rồi parse bằng MotionConfigParser.parseFromFile(...)
     */
    @NonNull
    @Throws(PackageManager.NameNotFoundException::class)
    private fun getMotionParamsInternal(skinPath: String): MotionParams {
        // Root: {filesDir}/skins
        val skinsRoot = File(filesDir, SKINS_DIR_NAME).apply { if (!exists()) mkdirs() }

        var folder = ""
        var xmlFile = "skin.xml"

        if (skinPath.isNotBlank()) {
            val parts = skinPath.split('/')
            if (parts.size >= 2) {
                folder = parts.dropLast(1).joinToString(File.separator)
                xmlFile = parts.last()
            } else {
                if (skinPath.lowercase().endsWith(".xml")) {
                    xmlFile = parts[0]
                    folder = ""
                } else {
                    folder = parts[0]
                    xmlFile = "skin.xml"
                }
            }
        }

        val dir = if (folder.isBlank()) skinsRoot else File(skinsRoot, folder)
        val skinXml = File(dir, xmlFile).let { f ->
            if (!f.exists()) {
                // fallback: lấy *.xml đầu tiên
                dir.listFiles { _, name -> name.lowercase().endsWith(".xml") }?.firstOrNull()
                    ?: throw IllegalArgumentException("skin.xml not found in ${dir.absolutePath}")
            } else f
        }

        // Không còn dùng Resources cho frame; Resources chỉ dùng cho metric trong parser.
        return MotionConfigParser.parseFromFile(this, skinXml, dir)
    }

    private fun afterMotionLoaded() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val d: Display = wm.defaultDisplay
        val size = Point().also { d.getSize(it) }
        val dw = size.x
        val dh = size.y

        val (cx, cy) = when (random.nextInt(4)) {
            0 -> 0 to random.nextInt(dh)
            1 -> dw to random.nextInt(dh)
            2 -> random.nextInt(dw) to 0
            else -> random.nextInt(dw) to dh
        }

        val alphaStr = prefs.getString(PREF_KEY_TRANSPARENCY, "0.0") ?: "0.0"
        val opacity = 1f - alphaStr.toFloatOrNull().let { it ?: 0f }
        motionState!!.alpha = (opacity * 0xff).roundToInt()

        motionState!!.setBehaviour(
            Behaviour.valueOf(
                prefs.getString(PREF_KEY_BEHAVIOUR, motionState!!.behaviour.toString())!!
            )
        )

        motionState!!.setDisplaySize(dw, dh)
        motionState!!.setCurrentPosition(cx.toFloat(), cy.toFloat())
        motionState!!.setTargetPositionDirect((dw shr 1).toFloat(), (dh shr 1).toFloat())
        refreshMotionSize()
    }

    // ================== UI refresh ==================

    private fun refreshMotionSize() {
        val v = prefs.getString(PREF_KEY_SIZE, "80")?.toFloatOrNull()?.toInt() ?: 80
        imageWidth = v
        imageHeight = v

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        if (imageParams != null && imageView != null) {
            imageParams!!.width = v
            imageParams!!.height = v
            wm.updateViewLayout(imageView, imageParams)
        }
    }

    private fun refreshMotionSpeed() {
        val ms = motionState ?: return
        val speedFactor = prefs.getString(PREF_KEY_SPEED, "1.0")?.toFloatOrNull() ?: 1.0f
        ms.setSpeedFactor(if (speedFactor > 0f) speedFactor else 1.0f)
    }

    private fun requestAnimate() {
        if (!handler.hasMessages(MSG_ANIMATE)) {
            handler.sendEmptyMessage(MSG_ANIMATE)
        }
    }

    private fun updateDrawable() {
        val ms = motionState ?: return
        val iv = imageView ?: return
        val drawable = ms.currentDrawable()

        drawable.alpha = ms.alpha
        iv.setImageDrawable(drawable)
        drawable.stop()
        drawable.start()
    }

    private fun updatePosition() {
        val pos = motionState?.position() ?: return
        imageParams?.x = pos.x
        imageParams?.y = pos.y
        (getSystemService(WINDOW_SERVICE) as WindowManager).updateViewLayout(imageView, imageParams)
    }

    private fun updateToNext() {
        val ms = motionState ?: return
        if (ms.checkWall() || ms.updateMovingState() || ms.changeToNextState()) {
            updateDrawable()
            updatePosition()
            requestAnimate()
        }
    }

    private fun onHandleMessage(msg: Message): Boolean {
        if (msg.what == MSG_ANIMATE) {
            handler.removeMessages(MSG_ANIMATE)
            motionState?.updateState()
            motionState?.let { ms ->
                if (ms.isStateChanged() || ms.isPositionMoved()) {
                    if (ms.isStateChanged()) updateDrawable()
                    updatePosition()
                    handler.sendEmptyMessageDelayed(MSG_ANIMATE, ANIMATION_INTERVAL)
                }
            }
            return true
        }
        return false
    }

    private fun checkPrefEnable(): Boolean {
        val enable = prefs.getBoolean(PREF_KEY_ENABLE, false)
        val visible = prefs.getBoolean(PREF_KEY_VISIBLE, false)
        if (!enable || !visible) {
            startService(Intent(this, AnimationService::class.java).setAction(ACTION_STOP))
            return false
        }
        return true
    }

    // ================== Listeners ==================

    private inner class PreferenceChangeListener :
        SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
            when (key) {
                PREF_KEY_ENABLE, PREF_KEY_VISIBLE -> checkPrefEnable()
                PREF_KEY_SIZE -> refreshMotionSize()
                PREF_KEY_SPEED -> refreshMotionSpeed()
                PREF_KEY_TRANSPARENCY -> {
                    motionState?.let {
                        val s = prefs.getString(PREF_KEY_TRANSPARENCY, "0.0") ?: "0.0"
                        val opacity = 1f - (s.toFloatOrNull() ?: 0f)
                        it.alpha = (opacity * 0xff).roundToInt()
                    }
                }

                else -> if (loadMotionState()) requestAnimate()
            }
        }
    }

    private inner class TouchListener : View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View?, ev: MotionEvent): Boolean {
            val ms = motionState ?: return false

            if (ev.action == MotionEvent.ACTION_OUTSIDE) {
                val wm = getSystemService(WINDOW_SERVICE) as WindowManager
                val pnt = Point().also { wm.defaultDisplay.getSize(it) }
                val dw = pnt.x
                val dh = pnt.y

                val (x, y) = if (random.nextFloat() < 0.4f) {
                    when (random.nextInt(4)) {
                        0 -> 0f to random.nextInt(dh).toFloat()
                        1 -> dw.toFloat() to random.nextInt(dh).toFloat()
                        2 -> random.nextInt(dw).toFloat() to 0f
                        else -> random.nextInt(dw).toFloat() to dh.toFloat()
                    }
                } else {
                    random.nextInt(dw).toFloat() to random.nextInt(dh).toFloat()
                }

                ms.setTargetPosition(x, y)
                requestAnimate()
            } else if (ev.action == MotionEvent.ACTION_CANCEL) {
                ms.forceStop()
                requestAnimate()
            }
            return false
        }
    }

    private inner class MotionEndListener : MotionDrawable.OnMotionEndListener {
        override fun onMotionEnd(drawable: MotionDrawable) {
            if (isStarted && motionState?.currentDrawable() === drawable) {
                updateToNext()
            }
        }
    }

    // ================== MotionState ==================

    private inner class MotionState {
        private var curX = 0f
        private var curY = 0f
        private var targetX = 0f
        private var targetY = 0f
        private var vx = 0f // px/s
        private var vy = 0f // px/s

        private var displayWidth = 1
        private var displayHeight = 1

        private lateinit var params: MotionParams
        var alpha: Int = 0xff

        var behaviour: Behaviour = Behaviour.whimsical
            private set
        private var curBehaviourIdx = 0
        private var lastBehaviourChanged = 0L

        private var curState: String? = null
        private var movingState = false
        private var stateChanged = false
        private var positionMoved = false
        private var speedFactor = 1.0f

        private val onMotionEnd = MotionEndListener()

        fun setSpeedFactor(factor: Float) {
            speedFactor = if (factor <= 0f) {
                Timber.w("Attempt to set non-positive speed: %f, fallback 1.0", factor)
                1.0f
            } else factor
        }

        fun updateState() {
            stateChanged = false
            positionMoved = false

            val dx = targetX - curX
            val dy = targetY - curY
            val len = hypot(dx, dy)

            if (len <= params.proximityDistance) {
                if (movingState) {
                    vx = 0f; vy = 0f
                    changeState(params.initialState)
                }
                return
            }

            if (!movingState) {
                val nstate = params.awakeState
                if (params.hasState(nstate)) changeState(nstate)
                return
            }

            val interval = ANIMATION_INTERVAL / 1000f
            val baseAcc = params.acceleration
            val baseMaxV = params.maxVelocity
            val decelDist = params.decelerationDistance

            val acc = baseAcc * speedFactor
            val maxV = baseMaxV * speedFactor

            if (len > 0) {
                vx += (acc * interval * dx / len)
                vy += (acc * interval * dy / len)
            }

            val curSpeed = hypot(vx, vy)
            val dynMax = maxV * min((len + 1.0f) / (decelDist + 1.0f), 1.0f)

            if (curSpeed > dynMax) {
                if (dynMax <= 0 && curSpeed > 0) {
                    vx = 0f; vy = 0f
                } else if (curSpeed > 0) {
                    val ratio = dynMax / curSpeed
                    vx *= ratio; vy *= ratio
                }
            }

            curX += vx * interval
            curY += vy * interval
            positionMoved = true

            changeToMovingState()
        }

        fun checkWall(): Boolean {
            if (!params.needCheckWall(curState!!)) return false

            val drawable = currentDrawable()
            val dw2 = drawable.intrinsicWidth / 2f
            val dh2 = drawable.intrinsicHeight / 2f

            val dir = when {
                curX >= 0 && curX < dw2 -> MotionParams.WallDirection.LEFT
                curX <= displayWidth && curX > displayWidth - dw2 -> MotionParams.WallDirection.RIGHT
                curY >= 0 && curY < dh2 -> MotionParams.WallDirection.UP
                curY <= displayHeight && curY > displayHeight - dh2 -> MotionParams.WallDirection.DOWN
                else -> return false
            }

            val nstate = params.getWallState(dir)
            if (!params.hasState(nstate)) return false
            changeState(nstate)
            return true
        }

        fun updateMovingState(): Boolean {
            if (!params.needCheckMove(curState!!)) return false
            val len = hypot(targetX - curX, targetY - curY)
            if (len <= params.proximityDistance) return false
            changeToMovingState()
            return true
        }

        fun setParams(p: MotionParams) {
            val nstate = p.initialState
            require(p.hasState(nstate)) { "Initial State does not exist" }
            params = p
            changeState(nstate)
            movingState = false
        }

        private fun changeState(state: String) {
            if (state == curState) return
            curState = state
            stateChanged = true
            movingState = false
            currentDrawable().setOnMotionEndListener(onMotionEnd)
        }

        fun changeToNextState(): Boolean {
            val next = params.getNextState(curState ?: return false) ?: return false
            changeState(next)
            return true
        }

        private fun changeToMovingState() {
            val dirIdx = ((atan2(vy, vx) * 4 / Math.PI) + 8.5).toInt() % 8
            val dirs = arrayOf(
                MotionParams.MoveDirection.RIGHT,
                MotionParams.MoveDirection.DOWN_RIGHT,
                MotionParams.MoveDirection.DOWN,
                MotionParams.MoveDirection.DOWN_LEFT,
                MotionParams.MoveDirection.LEFT,
                MotionParams.MoveDirection.UP_LEFT,
                MotionParams.MoveDirection.UP,
                MotionParams.MoveDirection.UP_RIGHT
            )
            val nstate = params.getMoveState(dirs[dirIdx])
            if (!params.hasState(nstate)) return
            changeState(nstate)
            movingState = true
        }

        fun setDisplaySize(w: Int, h: Int) {
            displayWidth = w; displayHeight = h
        }

        fun setBehaviour(b: Behaviour) {
            behaviour = b
            lastBehaviourChanged = 0
            curBehaviourIdx = Behaviour.valueOf(b.toString()).ordinal
        }

        fun setCurrentPosition(x: Float, y: Float) {
            curX = x; curY = y
        }

        fun setTargetPosition(x: Float, y: Float) {
            // Giữ nguyên hành vi cũ (ICS_OR_LATER = true -> whimsical)
            curBehaviourIdx = Behaviour.entries.size - 1
            when (Behaviour.entries[curBehaviourIdx]) {
                Behaviour.closer -> setTargetPositionDirect(x, y)
                Behaviour.further -> {
                    var dx = displayWidth / 2f - x
                    var dy = displayHeight / 2f - y
                    if (dx == 0f && dy == 0f) {
                        val ang = random.nextFloat() * (PI.toFloat()) * 2
                        dx = cos(ang); dy = sin(ang)
                    }
                    if (dx < 0) {
                        dx = -dx; dy = -dy
                    }
                    val e: PointF = run {
                        val e1: PointF
                        val e2: PointF
                        if (dy > dx * displayHeight / displayWidth || dy < -dx * displayHeight / displayWidth) {
                            val dxdy = dx / dy
                            e1 = PointF((displayWidth - displayHeight * dxdy) / 2f, 0f)
                            e2 = PointF(
                                (displayWidth + displayHeight * dxdy) / 2f,
                                displayHeight.toFloat()
                            )
                        } else {
                            val dydx = dy / dx
                            e1 = PointF(0f, (displayHeight - displayWidth * dydx) / 2f)
                            e2 = PointF(
                                displayWidth.toFloat(),
                                (displayHeight + displayWidth * dydx) / 2f
                            )
                        }
                        val d1 = hypot(e1.x - x, e1.y - y).toDouble()
                        val d2 = hypot(e2.x - x, e2.y - y).toDouble()
                        if (d1 > d2) e1 else e2
                    }
                    val r = 0.9f + random.nextFloat() * 0.1f
                    setTargetPositionDirect(e.x * r + x * (1 - r), e.y * r + y * (1 - r))
                }

                Behaviour.whimsical -> {
                    val minWH2 = min(displayWidth, displayHeight) / 2f
                    val r = random.nextFloat() * minWH2 + minWH2
                    val a = random.nextFloat() * 360f
                    var nx = curX + r * cos(Math.toRadians(a.toDouble())).toFloat()
                    var ny = curY + r * sin(Math.toRadians(a.toDouble())).toFloat()

                    if (random.nextFloat() < 0.15f) nx =
                        if (random.nextBoolean()) 0f else displayWidth.toFloat()
                    if (random.nextFloat() < 0.15f) ny =
                        if (random.nextBoolean()) 0f else displayHeight.toFloat()

                    if (random.nextFloat() < 0.15f) {
                        nx = nx.coerceIn(0f, displayWidth.toFloat())
                        ny = ny.coerceIn(0f, displayHeight.toFloat())
                    } else {
                        nx =
                            if (nx < 0) -nx else if (nx >= displayWidth) displayWidth * 2 - nx - 1 else nx
                        ny =
                            if (ny < 0) -ny else if (ny >= displayHeight) displayHeight * 2 - ny - 1 else ny
                    }
                    setTargetPositionDirect(nx, ny)
                }
            }
        }

        fun setTargetPositionDirect(x: Float, y: Float) {
            targetX = x; targetY = y
        }

        fun forceStop() {
            setTargetPosition(curX, curY)
            vx = 0f; vy = 0f
        }

        fun isStateChanged(): Boolean = stateChanged
        fun isPositionMoved(): Boolean = positionMoved

        fun currentDrawable(): MotionDrawable {
            return params.getDrawable(curState!!)!!
        }

        fun position(): Point {
            val d = currentDrawable()
            return Point(
                (curX - d.intrinsicWidth / 2f).toInt(),
                (curY - d.intrinsicHeight / 2f).toInt()
            )
        }
    }

}
