package org.nqmgaming.aneko.motion;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class MotionDrawable extends Drawable implements Animatable {
    public interface OnMotionEndListener {
        void onMotionEnd(MotionDrawable drawable);
    }

    private final MotionConstantState state;

    private int cur_frame = -1;
    private int cur_repeat = 0;
    private int cur_duration = -1;
    private OnMotionEndListener on_end = null;

    private int alpha = 0xff;
    private ColorFilter color_filter;

    private final Runnable frame_updater = this::updateFrame;
    private final Drawable.Callback child_callback = new ChildCallback();
    private final OnMotionEndListener child_end = new ChildOnMotionEnd();

    MotionDrawable() {
        state = new MotionConstantState();
    }

    private MotionDrawable(AnimationDrawable anim) {
        this();

        state.repeat_count = (anim.isOneShot() ? 1 : -1);

        int nf = anim.getNumberOfFrames();
        for (int i = 0; i < nf; i++) {
            addFrame(anim.getFrame(i), anim.getDuration(i));
        }
    }

    void setTotalDuration(int duration) {
        state.total_duration = duration;
    }

    void setRepeatCount(int count) {
        state.repeat_count = count;
    }

    void addFrame(Drawable drawable, int duration) {
        if (drawable instanceof AnimationDrawable) {
            MotionDrawable md = new MotionDrawable((AnimationDrawable) drawable);
            md.setTotalDuration(duration);
            drawable = md;
        }

        if (drawable instanceof MotionDrawable) {
            MotionDrawable md = (MotionDrawable) drawable;
            md.setOnMotionEndListener(child_end);
        }
        drawable.setCallback(child_callback);
        state.addFrame(drawable, duration);
    }

    private Drawable getCurrentFrame() {
        return state.getFrame(cur_frame);
    }

    public void setOnMotionEndListener(OnMotionEndListener listener) {
        on_end = listener;
    }

    private void invokeOnMotionEndListener() {
        if (on_end != null) {
            on_end.onMotionEnd(this);
        }
    }

    @Override
    public int getIntrinsicWidth() {
        return getCurrentFrame().getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return getCurrentFrame().getIntrinsicHeight();
    }

    @Override
    public Drawable.ConstantState getConstantState() {
        return state;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Drawable current = getCurrentFrame();
        if (current != null) {
            current.draw(canvas);
        }
    }

    @Override
    public int getOpacity() {
        Drawable current = getCurrentFrame();
        return ((current == null || !current.isVisible()) ?
                PixelFormat.TRANSPARENT : state.getOpacity());
    }

    @Override
    public void setAlpha(int _alpha) {
        if (alpha != _alpha) {
            alpha = _alpha;
            Drawable current = getCurrentFrame();
            if (current != null) {
                current.setAlpha(alpha);
            }
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        if (color_filter != cf) {
            color_filter = cf;
            Drawable current = getCurrentFrame();
            if (current != null) {
                current.setColorFilter(color_filter);
            }
        }
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        Drawable current = getCurrentFrame();
        if (current != null) {
            current.setBounds(bounds);
        }
    }

    @Override
    protected boolean onLevelChange(int level) {
        Drawable current = getCurrentFrame();
        if (current != null) {
            return current.setLevel(level);
        }
        return false;
    }

    @Override
    protected boolean onStateChange(@NonNull int[] state) {
        Drawable current = getCurrentFrame();
        if (current != null) {
            return current.setState(state);
        }
        return false;
    }

    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);
        Drawable current = getCurrentFrame();
        if (current != null) {
            current.setVisible(visible, restart);
        }

        if (visible) {
            if (changed || restart) {
                stop();
                start();
            }
        } else {
            stop();
        }

        return changed;
    }

    @Override
    public boolean isRunning() {
        return (cur_duration >= 0);
    }

    @Override
    public void start() {
        if (!isRunning()) {
            cur_frame = -1;
            cur_repeat = 0;
            cur_duration = 0;
            updateFrame();
        }
    }

    @Override
    public void stop() {
        if (isRunning()) {
            unscheduleSelf(frame_updater);
            cur_duration = -1;
        }
    }

    private void updateFrame() {
        int nf = state.getFrameCount();
        int next = cur_frame + 1;
        int next_repeat = cur_repeat;
        if (next >= nf) {
            next = 0;
            next_repeat = cur_repeat + 1;

            if (state.repeat_count >= 0 && next_repeat >= state.repeat_count) {
                cur_duration = -1;
                invokeOnMotionEndListener();
                return;
            }
        }

        if (state.total_duration >= 0 && cur_duration >= state.total_duration) {
            cur_duration = -1;
            invokeOnMotionEndListener();
            return;
        }

        {
            Drawable current = getCurrentFrame();
            if (current != null) {
                current.setVisible(false, false);
            }
        }

        cur_frame = next;
        cur_repeat = next_repeat;

        Drawable next_drawable = state.getFrame(next);
        assert next_drawable != null;
        next_drawable.setVisible(isVisible(), true);
        next_drawable.setAlpha(alpha);
        next_drawable.setColorFilter(color_filter);
        next_drawable.setState(getState());
        next_drawable.setLevel(getLevel());
        next_drawable.setBounds(getBounds());

        int duration = state.getFrameDuration(next);
        int next_duration =
                (duration < 0 && state.total_duration < 0 ? -1 :
                        duration < 0 ? state.total_duration - cur_duration :
                                state.total_duration < 0 ? cur_duration + duration :
                                        Math.min(cur_duration + duration,
                                                state.total_duration));
        if (next_duration >= 0) {
            duration = next_duration - cur_duration;
            scheduleSelf(frame_updater, SystemClock.uptimeMillis() + duration);
        }

        cur_duration = (next_duration >= 0 ? next_duration : cur_duration);
        invalidateSelf();
    }

    private static class MotionConstantState extends ConstantState {
        private final ArrayList<ItemInfo> drawables;
        private int changing_configurations = 0;
        private int opacity;
        private int total_duration = 0;
        private int repeat_count = 1;

        private MotionConstantState() {
            drawables = new ArrayList<>();
        }

        private void addFrame(Drawable drawable, int duration) {
            drawables.add(new ItemInfo(drawable, duration));
            if (duration >= 0 && total_duration >= 0) {
                total_duration += duration;
            } else {
                total_duration = -1;
            }

            changing_configurations |= drawable.getChangingConfigurations();
            opacity = (drawables.size() > 1 ?
                    Drawable.resolveOpacity(opacity, drawable.getOpacity()) :
                    drawable.getOpacity());
        }

        private Drawable getFrame(int idx) {
            idx = (Math.max(idx, 0));
            if (drawables.size() <= idx) {
                return null;
            }

            return drawables.get(idx).drawable;
        }

        private int getFrameDuration(int idx) {
            idx = (Math.max(idx, 0));
            if (drawables.size() <= idx) {
                return 0;
            }

            return drawables.get(idx).duration;
        }

        private int getFrameCount() {
            return drawables.size();
        }

        @Override
        public int getChangingConfigurations() {
            return changing_configurations;
        }

        @NonNull
        @Override
        public Drawable newDrawable() {
            throw new UnsupportedOperationException(
                    "newDrawable is not supported");
        }

        @NonNull
        @Override
        public Drawable newDrawable(Resources res) {
            throw new UnsupportedOperationException(
                    "newDrawable is not supported");
        }

        private int getOpacity() {
            return (drawables.size() > 1 ? opacity : PixelFormat.TRANSPARENT);
        }
    }

    private static class ItemInfo {
        private final Drawable drawable;
        private final int duration;

        private ItemInfo(Drawable drawable, int duration) {
            this.drawable = drawable;
            this.duration = duration;
        }
    }

    private class ChildCallback implements Drawable.Callback {
        @Override
        public void invalidateDrawable(@NonNull Drawable who) {
            if (who == getCurrentFrame()) {
                invalidateSelf();
            }
        }

        @Override
        public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
            if (who == getCurrentFrame()) {
                scheduleSelf(what, when);
            }
        }

        @Override
        public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
            if (who == getCurrentFrame()) {
                unscheduleSelf(what);
            }
        }
    }

    private class ChildOnMotionEnd implements OnMotionEndListener {
        @Override
        public void onMotionEnd(MotionDrawable drawable) {
            if (drawable == getCurrentFrame()) {
                updateFrame();
            }
        }
    }
}
