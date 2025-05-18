package org.nqmgaming.aneko.core.service;

import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
import static android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.nqmgaming.aneko.core.motion.MotionDrawable;
import org.nqmgaming.aneko.core.motion.MotionParams;
import org.nqmgaming.aneko.core.motion.MotionConfigParser;
import org.nqmgaming.aneko.R;
import org.tamanegi.aneko.ANekoActivity;

import java.io.File;
import java.util.Random;

import timber.log.Timber;

public class AnimationService extends Service {
    public static final String ACTION_START = "org.nqmgaming.aneko.action.START";
    public static final String ACTION_STOP = "org.nqmgaming.aneko.action.STOP";
    public static final String ACTION_TOGGLE = "org.nqmgaming.aneko.action.TOGGLE";
    public static final String ACTION_GET_SKIN = "org.tamanegi.aneko.action.GET_SKIN";
    public static final String META_KEY_SKIN = "org.tamanegi.aneko.skin";
    public static final String PREF_KEY_ENABLE = "motion.enable";
    public static final String PREF_KEY_VISIBLE = "motion.visible";
    public static final String PREF_KEY_TRANSPARENCY = "motion.transparency";
    public static final String PREF_KEY_SIZE = "motion.size";
    public static final String PREF_KEY_SPEED = "motion.speed";
    public static final String PREF_KEY_BEHAVIOUR = "motion.behaviour";
    public static final String PREF_KEY_SKIN_COMPONENT = "motion.skin";
    public static final String PREF_KEY_KEEP_ALIVE = "motion.keep_alive";
    public static final String PREF_KEY_NOTIFICATION_ENABLE = "notification.enable";
    private static final int MSG_ANIMATE = 1;
    private static final long ANIMATION_INTERVAL = 125; // msec
    private static final long BEHAVIOUR_CHANGE_DURATION = 4000; // msec
    public static final String ANeko_SKINS = "/ANeko/skins";
    private int image_width = 80;
    private int image_height = 80;

    private enum Behaviour {
        closer, further, whimsical
    }

    private static final Behaviour[] BEHAVIOURS = {
            Behaviour.closer, Behaviour.further, Behaviour.whimsical};
    private static final boolean ICS_OR_LATER = true;
    private boolean is_started;
    private SharedPreferences prefs;
    private PreferenceChangeListener pref_listener;
    private Handler handler;
    private MotionState motion_state = null;
    private Random random;
    private View touch_view = null;
    private ImageView image_view = null;
    private LayoutParams image_params = null;
    private BroadcastReceiver receiver = null;


    @Override
    public void onCreate() {
        is_started = false;
        handler = new Handler(this::onHandleMessage);
        random = new Random();
        prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!Settings.canDrawOverlays(this)) {
            Intent overlayIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            overlayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(overlayIntent);
            return START_NOT_STICKY;
        }
        if (!is_started &&
                (intent == null || ACTION_START.equals(intent.getAction()))) {
            if (is_started) {
                stopAnimation();
            }
            startAnimation();
            setForegroundNotification(true);
            is_started = true;
        } else if (ACTION_TOGGLE.equals(intent.getAction())) {
            toggleAnimation();
        } else if (is_started &&
                ACTION_STOP.equals(intent.getAction())) {
            stopAnimation();
            stopSelfResult(startId);
            setForegroundNotification(false);
            is_started = false;
        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onConfigurationChanged(Configuration conf) {
        if (!is_started || motion_state == null) return;
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        assert wm != null;
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        motion_state.setDisplaySize(width, height);
    }

    private void startAnimation() {
        this.pref_listener = new PreferenceChangeListener();
        this.prefs.registerOnSharedPreferenceChangeListener(this.pref_listener);
        if (checkPrefEnable() && loadMotionState()) {
            refreshMotionSpeed();
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            this.touch_view = new View(this);
            this.touch_view.setOnTouchListener(new TouchListener());
            LayoutParams touch_params = new LayoutParams(1, 1, TYPE_APPLICATION_OVERLAY,
                    FLAG_NOT_FOCUSABLE | FLAG_WATCH_OUTSIDE_TOUCH | FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT);

            touch_params.gravity = Gravity.CENTER;
            assert wm != null;
            wm.addView(this.touch_view, touch_params);
            this.image_view = new ImageView(this);
            this.image_params = new WindowManager.LayoutParams(
                    this.image_width,
                    this.image_height,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT
            );
            this.image_params.gravity = Gravity.TOP | Gravity.START;
            wm.addView(this.image_view, this.image_params);
            requestAnimate();
        }

    }

    private void stopAnimation() {
        prefs.unregisterOnSharedPreferenceChangeListener(pref_listener);
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        if (touch_view != null) {
            assert wm != null;
            wm.removeView(touch_view);
        }
        if (image_view != null) {
            assert wm != null;
            wm.removeView(image_view);
        }
        if (receiver != null) {
            unregisterReceiver(receiver);
        }

        motion_state = null;
        touch_view = null;
        image_view = null;
        receiver = null;

        handler.removeMessages(MSG_ANIMATE);
    }

    private void toggleAnimation() {
        boolean visible = prefs.getBoolean(PREF_KEY_VISIBLE, true);

        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(PREF_KEY_VISIBLE, !visible);
        edit.apply();

        startService(new Intent(this, AnimationService.class)
                .setAction(ACTION_START));
    }

    private void setForegroundNotification(boolean start) {
        PendingIntent intent = PendingIntent.getService(
                this, 0,
                new Intent(this, AnimationService.class).setAction(ACTION_TOGGLE),
                PendingIntent.FLAG_IMMUTABLE);

        {
            NotificationChannel channel = new NotificationChannel(
                    getString(R.string.app_name),
                    getString(R.string.aneko_notification_channel_name),
                    NotificationManager.IMPORTANCE_MIN
            );
            channel.setDescription(getString(R.string.notification_chanel_description));
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }

        Notification.Builder builder;
        builder = new Notification.Builder(this, getString(R.string.app_name));

        builder
                .setContentIntent(intent)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(start ? R.string.notification_enabled : R.string.notification_disabled))
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setAutoCancel(false);

        Notification notify = builder.build();

        stopForeground(true);
        if (start) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notify, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(1, notify);
            }
            return;
        }

        if (this.prefs.getBoolean(PREF_KEY_ENABLE, true)) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(1, notify);
            }
        }
    }


    private boolean loadMotionState() {
        String skin = prefs.getString(PREF_KEY_SKIN_COMPONENT, "");
        if (skin.contains(".xml") || skin.isEmpty()) return loadMotionDir();
        else return loadMotionApk();
    }

    private boolean loadMotionApk() {
        String skin_pkg = prefs.getString(PREF_KEY_SKIN_COMPONENT, null);
        ComponentName skin_comp = (skin_pkg == null ? null : ComponentName.unflattenFromString(skin_pkg));
        if (skin_comp != null && loadMotionState(skin_comp)) {
            return true;
        }
        skin_comp = new ComponentName(this, ANekoActivity.class);
        return loadMotionState(skin_comp);
    }

    private boolean loadMotionDir() {
        boolean loaded = false;
        String skinPath = prefs.getString(PREF_KEY_SKIN_COMPONENT, "");
        try {
            MotionParams params2 = getMotionParams(skinPath);
            motion_state = new MotionState();
            motion_state.setParams(params2);

            loaded = true;
        } catch (Exception e) {
            Timber.e(e);
        }
        if (loaded) {
            afterMotionLoaded();
            return true;
        }

        ComponentName skin_comp;
        skin_comp = new ComponentName(this, ANekoActivity.class);
        return loadMotionState(skin_comp);
    }

    @NonNull
    private MotionParams getMotionParams(String skinPath) throws PackageManager.NameNotFoundException {
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        File skinsDir = new File(externalStorageDirectory, ANeko_SKINS);

        if (!skinsDir.exists() && !skinsDir.mkdirs()) {
            Timber.w("Failed to create skins directory: %s", skinsDir.getAbsolutePath());
        }

        String[] ts = skinPath.split("/");

        String folder = ts[0];
        String xmlFile = ts[1];

        File dir = new File(skinsDir, folder);

        PackageManager pm = getPackageManager();
        ComponentName skin_comp = new ComponentName(this, ANekoActivity.class);
        Resources res = pm.getResourcesForActivity(skin_comp);

        return new MotionConfigParser(res, dir, xmlFile);
    }


    private boolean loadMotionState(ComponentName skin_comp) {
        motion_state = new MotionState();
        try {
            PackageManager pm = getPackageManager();
            ActivityInfo ai = pm.getActivityInfo(
                    skin_comp, PackageManager.GET_META_DATA);
            Resources res = pm.getResourcesForActivity(skin_comp);

            int rid = ai.metaData.getInt(META_KEY_SKIN, 0);

            MotionParams params = new MotionParams(res, rid);
            motion_state.setParams(params);
        } catch (Exception e) {
            Timber.e(e);
            Toast.makeText(this, R.string.msg_skin_load_failed,
                            Toast.LENGTH_LONG)
                    .show();

            startService(new Intent(this, AnimationService.class)
                    .setAction(ACTION_TOGGLE));
            return false;
        }
        afterMotionLoaded();
        return true;
    }

    private void afterMotionLoaded() {
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        assert wm != null;
        Display d = wm.getDefaultDisplay();
        Point size = new Point();
        d.getSize(size);
        int dw = size.x;
        int dh = size.y;

        int cx, cy;
        switch (random.nextInt(4)) {
            case 0:
                cx = 0;
                cy = random.nextInt(dh);
                break;
            case 1:
                cx = dw;
                cy = random.nextInt(dh);
                break;
            case 2:
                cx = random.nextInt(dw);
                cy = 0;
                break;
            default:
                cx = random.nextInt(dw);
                cy = dh;
                break;
        }

        String alpha_str = prefs.getString(PREF_KEY_TRANSPARENCY, "0.0");
        float opacity = 1 - Float.parseFloat(alpha_str);
        motion_state.alpha = (int) (opacity * 0xff);

        motion_state.setBehaviour(
                Behaviour.valueOf(
                        prefs.getString(PREF_KEY_BEHAVIOUR,
                                motion_state.behaviour.toString())));

        motion_state.setDisplaySize(dw, dh);
        motion_state.setCurrentPosition(cx, cy);
        motion_state.setTargetPositionDirect(dw >> 1, dh >> 1);
        refreshMotionSize();
    }


    private void refreshMotionSize() {
        int v = 80;
        try {
            String value = prefs.getString(PREF_KEY_SIZE, "80");
            v = (int) Float.parseFloat(value);
        } catch (NumberFormatException e) {
            Timber.e(e);
        }
        this.image_width = this.image_height = v;

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (image_params != null && image_view != null) {
            image_params.width = v;
            image_params.height = v;
            assert wm != null;
            wm.updateViewLayout(image_view, image_params);
        }
    }


    // Motion Speed
    private void refreshMotionSpeed() {
        if (motion_state == null) {
            return;
        }
        String speedStr = prefs.getString(PREF_KEY_SPEED, "1.0");
        float speedFactor = 1.0f;
        try {
            speedFactor = Float.parseFloat(speedStr);
        } catch (NumberFormatException e) {
            Timber.e(e, "Invalid speed factor in preferences: %s. Using default 1.0f.", speedStr);
        }

        motion_state.setSpeedFactor(speedFactor);
    }


    private void requestAnimate() {
        if (!handler.hasMessages(MSG_ANIMATE)) {
            handler.sendEmptyMessage(MSG_ANIMATE);
        }
    }

    private void updateDrawable() {
        if (motion_state == null || image_view == null) {
            return;
        }

        MotionDrawable drawable = motion_state.getCurrentDrawable();
        if (drawable == null) {
            return;
        }

        drawable.setAlpha(motion_state.alpha);
        image_view.setImageDrawable(drawable);
        drawable.stop();
        drawable.start();
    }

    private void updatePosition() {
        Point pt = motion_state.getPosition();
        image_params.x = pt.x;
        image_params.y = pt.y;

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        assert wm != null;
        wm.updateViewLayout(image_view, image_params);
    }

    private void updateToNext() {
        if (motion_state.checkWall() ||
                motion_state.updateMovingState() ||
                motion_state.changeToNextState()) {
            updateDrawable();
            updatePosition();
            requestAnimate();
        }
    }

    private boolean onHandleMessage(Message msg) {
        if (msg.what == MSG_ANIMATE) {
            handler.removeMessages(MSG_ANIMATE);

            motion_state.updateState();
            if (motion_state.isStateChanged() ||
                    motion_state.isPositionMoved()) {
                if (motion_state.isStateChanged()) {
                    updateDrawable();
                }

                updatePosition();

                handler.sendEmptyMessageDelayed(
                        MSG_ANIMATE, ANIMATION_INTERVAL);
            }
        } else {
            return false;
        }

        return true;
    }

    private boolean checkPrefEnable() {
        boolean enable = prefs.getBoolean(PREF_KEY_ENABLE, false);
        boolean visible = prefs.getBoolean(PREF_KEY_VISIBLE, false);

        if (!enable || !visible) {
            startService(new Intent(this, AnimationService.class)
                    .setAction(ACTION_STOP));
            return false;
        } else {
            return true;
        }
    }

    private class PreferenceChangeListener
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (PREF_KEY_ENABLE.equals(key) || PREF_KEY_VISIBLE.equals(key)) {
                checkPrefEnable();
            } else if (PREF_KEY_SIZE.equals(key)) {
                refreshMotionSize();
            } else if (PREF_KEY_SPEED.equals(key)) {
                refreshMotionSpeed();
            } else if (PREF_KEY_KEEP_ALIVE.equals(key)) {
                // Do nothing
                Timber.d("Keep alive preference changed, but no action taken.");
            } else if (loadMotionState()) {
                requestAnimate();
            }
        }
    }

    private class TouchListener implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        public boolean onTouch(View v, MotionEvent ev) {
            if (motion_state == null) {
                return false;
            }

            if (ev.getAction() == MotionEvent.ACTION_OUTSIDE) {
                WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
                assert wm != null;
                Point pnt = new Point();
                wm.getDefaultDisplay().getSize(pnt);
                int dw = pnt.x;
                int dh = pnt.y;

                float x, y;

                if (random.nextFloat() < 0.4f) {
                    switch (random.nextInt(4)) {
                        case 0:
                            x = 0;
                            y = random.nextInt(dh);
                            break;
                        case 1:
                            x = dw;
                            y = random.nextInt(dh);
                            break;
                        case 2:
                            x = random.nextInt(dw);
                            y = 0;
                            break;
                        default:
                            x = random.nextInt(dw);
                            y = dh;
                            break;
                    }
                } else {
                    x = random.nextInt(dw);
                    y = random.nextInt(dh);
                }

                motion_state.setTargetPosition(x, y);
                requestAnimate();

            } else if (ev.getAction() == MotionEvent.ACTION_CANCEL) {
                motion_state.forceStop();
                requestAnimate();
            }

            return false;
        }
    }


    private class MotionState {
        private float cur_x = 0;
        private float cur_y = 0;
        private float target_x = 0;
        private float target_y = 0;
        private float vx = 0;                   // pixels per sec
        private float vy = 0;                   // pixels per sec

        private int display_width = 1;
        private int display_height = 1;

        private MotionParams params;
        private int alpha = 0xff;

        //private Behaviour behaviour = Behaviour.closer;
        private Behaviour behaviour = Behaviour.whimsical;
        private int cur_behaviour_idx = 0;
        private long last_behaviour_changed = 0;

        private String cur_state = null;

        private boolean moving_state = false;
        private boolean state_changed = false;
        private boolean position_moved = false;
        private float speedFactor = 1.0f;

        private final MotionEndListener on_motion_end = new MotionEndListener();

        public void setSpeedFactor(float factor) {
            if (factor <= 0) {
                Timber.w("Attempted to set non-positive speed factor: %f. Using 1.0f.", factor);
                this.speedFactor = 1.0f;
            } else {
                this.speedFactor = factor;
            }
        }

        private void updateState() {
            state_changed = false;
            position_moved = false;

            float dx = target_x - cur_x;
            float dy = target_y - cur_y;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len <= params.getProximityDistance()) {
                if (moving_state) {
                    vx = 0;
                    vy = 0;
                    changeState(params.getInitialState());
                }
                return;
            }

            if (!moving_state) {
                String nstate = params.getAwakeState();
                if (params.hasState(nstate)) {
                    changeState(nstate);
                }
                return;
            }

            float interval = ANIMATION_INTERVAL / 1000f; // Time step in seconds

            float baseAcceleration = params.getAcceleration();
            float baseMaxVelocity = params.getMaxVelocity();
            float decelerate_distance = params.getDecelerationDistance();

            float effectiveAcceleration = baseAcceleration * this.speedFactor;
            float effectiveMaxVelocity = baseMaxVelocity * this.speedFactor;

            if (len > 0) {
                vx += (effectiveAcceleration * interval * dx / len);
                vy += (effectiveAcceleration * interval * dy / len);
            }

            float currentSpeedMag = (float) Math.sqrt(vx * vx + vy * vy);

            float dynamicMaxSpeed = effectiveMaxVelocity * Math.min((len + 1.0f) / (decelerate_distance + 1.0f), 1.0f);

            if (currentSpeedMag > dynamicMaxSpeed) {
                if (dynamicMaxSpeed <= 0 && currentSpeedMag > 0) {
                    vx = 0;
                    vy = 0;
                } else if (currentSpeedMag > 0) {
                    float ratio = dynamicMaxSpeed / currentSpeedMag;
                    vx *= ratio;
                    vy *= ratio;
                }
            }

            cur_x += vx * interval;
            cur_y += vy * interval;
            position_moved = true;

            changeToMovingState();
        }

        private boolean checkWall() {
            if (!params.needCheckWall(cur_state)) {
                return false;
            }

            MotionDrawable drawable = getCurrentDrawable();
            float dw2 = drawable.getIntrinsicWidth() / 2f;
            float dh2 = drawable.getIntrinsicHeight() / 2f;

            MotionParams.WallDirection dir;
            float nx = cur_x;
            float ny = cur_y;
            if (cur_x >= 0 && cur_x < dw2) {
                nx = dw2;
                dir = MotionParams.WallDirection.LEFT;
            } else if (cur_x <= display_width && cur_x > display_width - dw2) {
                nx = display_width - dw2;
                dir = MotionParams.WallDirection.RIGHT;
            } else if (cur_y >= 0 && cur_y < dh2) {
                ny = dh2;
                dir = MotionParams.WallDirection.UP;
            } else if (cur_y <= display_height && cur_y > display_height - dh2) {
                ny = display_height - dh2;
                dir = MotionParams.WallDirection.DOWN;
            } else {
                return false;
            }

            String nstate = params.getWallState(dir);
            if (!params.hasState(nstate)) {
                return false;
            }

            cur_x = target_x = nx;
            cur_y = target_y = ny;
            changeState(nstate);

            return true;
        }

        private boolean updateMovingState() {
            if (!params.needCheckMove(cur_state)) {
                return false;
            }

            float dx = target_x - cur_x;
            float dy = target_y - cur_y;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len <= params.getProximityDistance()) {
                return false;
            }

            changeToMovingState();
            return true;
        }

        private void setParams(MotionParams _params) {
            String nstate = _params.getInitialState();
            if (!_params.hasState(nstate)) {
                throw new IllegalArgumentException(
                        "Initial State does not exist");
            }

            params = _params;

            changeState(nstate);
            moving_state = false;
        }

        private void changeState(String state) {
            if (state.equals(cur_state)) {
                return;
            }

            cur_state = state;
            state_changed = true;
            moving_state = false;
            getCurrentDrawable().setOnMotionEndListener(on_motion_end);
        }

        private boolean changeToNextState() {
            String next_state = params.getNextState(motion_state.cur_state);
            if (next_state == null) {
                return false;
            }

            changeState(next_state);
            return true;
        }

        private void changeToMovingState() {
            int dir = (int) (Math.atan2(vy, vx) * 4 / Math.PI + 8.5) % 8;
            MotionParams.MoveDirection[] dirs = {
                    MotionParams.MoveDirection.RIGHT,
                    MotionParams.MoveDirection.DOWN_RIGHT,
                    MotionParams.MoveDirection.DOWN,
                    MotionParams.MoveDirection.DOWN_LEFT,
                    MotionParams.MoveDirection.LEFT,
                    MotionParams.MoveDirection.UP_LEFT,
                    MotionParams.MoveDirection.UP,
                    MotionParams.MoveDirection.UP_RIGHT
            };

            String nstate = params.getMoveState(dirs[dir]);
            if (!params.hasState(nstate)) {
                return;
            }

            changeState(nstate);
            moving_state = true;
        }

        private void setDisplaySize(int w, int h) {
            display_width = w;
            display_height = h;
        }

        private void setBehaviour(Behaviour b) {
            behaviour = b;
            last_behaviour_changed = 0;

            for (int i = 0; i < BEHAVIOURS.length; i++) {
                if (BEHAVIOURS[i] == behaviour) {
                    cur_behaviour_idx = i;
                    break;
                }
            }
        }

        private void setCurrentPosition(float x, float y) {
            cur_x = x;
            cur_y = y;
        }

        private void setTargetPosition(float x, float y) {
            if (!ICS_OR_LATER) {
                long cur_time = System.currentTimeMillis();
                double r = (double) (cur_time - last_behaviour_changed) / BEHAVIOUR_CHANGE_DURATION;
                if (behaviour == Behaviour.whimsical && (r < 0 || random.nextDouble() * r > 1)) {
                    int next_idx = random.nextInt(BEHAVIOURS.length);
                    if (next_idx != cur_behaviour_idx) {
                        last_behaviour_changed = cur_time;
                    }
                    cur_behaviour_idx = next_idx;
                }
            } else {
                cur_behaviour_idx = BEHAVIOURS.length - 1;
            }

            Behaviour current = BEHAVIOURS[cur_behaviour_idx];
            if (current == Behaviour.closer) {
                setTargetPositionDirect(x, y);

            } else if (current == Behaviour.further) {
                float dx = display_width / 2f - x;
                float dy = display_height / 2f - y;
                if (dx == 0 && dy == 0) {
                    float ang = random.nextFloat() * (float) Math.PI * 2;
                    dx = (float) Math.cos(ang);
                    dy = (float) Math.sin(ang);
                }
                if (dx < 0) {
                    dx = -dx;
                    dy = -dy;
                }

                PointF e1, e2;
                if (dy > dx * display_height / display_width || dy < -dx * display_height / display_width) {
                    float dxdy = dx / dy;
                    e1 = new PointF((display_width - display_height * dxdy) / 2f, 0);
                    e2 = new PointF((display_width + display_height * dxdy) / 2f, display_height);
                } else {
                    float dydx = dy / dx;
                    e1 = new PointF(0, (display_height - display_width * dydx) / 2f);
                    e2 = new PointF(display_width, (display_height + display_width * dydx) / 2f);
                }

                double d1 = Math.hypot(e1.x - x, e1.y - y);
                double d2 = Math.hypot(e2.x - x, e2.y - y);
                PointF e = (d1 > d2 ? e1 : e2);

                float r = 0.9f + random.nextFloat() * 0.1f;
                setTargetPositionDirect(e.x * r + x * (1 - r), e.y * r + y * (1 - r));

            } else {
                float min_wh2 = Math.min(display_width, display_height) / 2f;
                float r = random.nextFloat() * min_wh2 + min_wh2;
                float a = random.nextFloat() * 360f;
                float nx = cur_x + r * (float) Math.cos(Math.toRadians(a));
                float ny = cur_y + r * (float) Math.sin(Math.toRadians(a));

                if (random.nextFloat() < 0.15f) {
                    nx = (random.nextBoolean()) ? 0 : display_width;
                }
                if (random.nextFloat() < 0.15f) {
                    ny = (random.nextBoolean()) ? 0 : display_height;
                }

                if (random.nextFloat() < 0.15f) {
                    nx = Math.max(0, Math.min(nx, display_width));
                    ny = Math.max(0, Math.min(ny, display_height));
                } else {
                    nx = (nx < 0 ? -nx : (nx >= display_width ? display_width * 2 - nx - 1 : nx));
                    ny = (ny < 0 ? -ny : (ny >= display_height ? display_height * 2 - ny - 1 : ny));
                }

                setTargetPositionDirect(nx, ny);
            }
        }


        private void setTargetPositionDirect(float x, float y) {
            target_x = x;
            target_y = y;
        }

        private void forceStop() {
            setTargetPosition(cur_x, cur_y);
            vx = 0;
            vy = 0;
        }

        private boolean isStateChanged() {
            return state_changed;
        }

        private boolean isPositionMoved() {
            return position_moved;
        }

        private MotionDrawable getCurrentDrawable() {
            return params.getDrawable(cur_state);
        }

        private Point getPosition() {
            MotionDrawable drawable = getCurrentDrawable();
            return new Point((int) (cur_x - drawable.getIntrinsicWidth() / 2f),
                    (int) (cur_y - drawable.getIntrinsicHeight() / 2f));
        }
    }

    private class MotionEndListener
            implements MotionDrawable.OnMotionEndListener {
        @Override
        public void onMotionEnd(MotionDrawable drawable) {
            if (is_started && motion_state != null &&
                    drawable == motion_state.getCurrentDrawable()) {
                updateToNext();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
    }
}
