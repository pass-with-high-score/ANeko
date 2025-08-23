package org.nqmgaming.aneko.core.motion;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import timber.log.Timber;

public class MotionConfigParser extends MotionParams {


    private final String resourceBaseDir;

    private final HashMap<String, Motion> motions = new HashMap<>();

    public MotionConfigParser(Resources res, File baseDir, String xmlFile) {

        this.resourceBaseDir = baseDir.getAbsolutePath();

        try {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            File file = new File(baseDir, xmlFile);
            parser.setInput(new BufferedInputStream(new FileInputStream(file)), "utf8");
            AttributeSet attrs = Xml.asAttributeSet(parser);
            parseXml(res, parser, attrs);

        } catch (Exception e) {
            Timber.e(e);
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public float getAcceleration() {
        return super.getAcceleration();
    }

    @Override
    public float getMaxVelocity() {
        return super.getMaxVelocity();
    }

    @Override
    public float getDecelerationDistance() {
        return super.getDecelerationDistance();
    }

    @Override
    public float getProximityDistance() {
        return super.getProximityDistance();
    }

    public boolean hasState(String state) {
        return motions.containsKey(state);
    }

    @Override
    public String getInitialState() {
        return super.getInitialState();
    }

    @Override
    public String getAwakeState() {
        return super.getAwakeState();
    }

    public String getMoveState(MoveDirection dir) {
        switch (dir) {
            case UP:
                return move_state_prefix + "Up";
            case DOWN:
                return move_state_prefix + "Down";
            case LEFT:
                return move_state_prefix + "Left";
            case RIGHT:
                return move_state_prefix + "Right";
            default:
                return null;
        }
    }

    public String getWallState(WallDirection dir) {
        switch (dir) {
            case UP:
                return wall_state_prefix + "Up";
            case DOWN:
                return wall_state_prefix + "Down";
            case LEFT:
                return wall_state_prefix + "Left";
            case RIGHT:
                return wall_state_prefix + "Right";
            default:
                return null;
        }
    }

    public String getNextState(String state) {
        Motion motion = motions.get(state);
        return (motion != null ? motion.next_state : null);
    }

    public boolean needCheckMove(String state) {
        Motion motion = motions.get(state);
        return (motion != null && motion.check_move);
    }

    public boolean needCheckWall(String state) {
        Motion motion = motions.get(state);
        return (motion != null && motion.check_wall);
    }

    public MotionDrawable getDrawable(String state) {
        Motion motion = motions.get(state);
        return (motion != null ? motion.items : null);
    }

    public void parseXml(Resources res, XmlPullParser xml, AttributeSet attrs)
            throws XmlPullParserException, IOException {
        int depth = xml.getDepth();
        while (true) {
            int type = xml.next();
            if (type == XmlPullParser.END_DOCUMENT ||
                    (type == XmlPullParser.END_TAG && depth >= xml.getDepth())) {
                break;
            }
            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            String name = xml.getName();
            if (TAG_MOTION_PARAMS.equals(name)) {
                parseMotionParams(res, xml, attrs);
            } else {
                throw new IllegalArgumentException("unknown tag: " + name);
            }
        }
    }


    public void parseMotionParams(
            Resources res,
            XmlPullParser xml, AttributeSet attrs)
            throws XmlPullParserException, IOException {
        float density = res.getDisplayMetrics().density;
        acceleration = density * attrs.getAttributeIntValue(
                null, ATTR_ACCELERATION, DEF_ACCELERATION);
        deceleration_distance = density * attrs.getAttributeIntValue(
                null, ATTR_ACCELERATION, DEF_DECELERATE_DISTANCE);
        max_velocity = density * attrs.getAttributeIntValue(
                null, ATTR_MAX_VELOCITY, DEF_MAX_VELOCITY);
        proximity_distance = density * attrs.getAttributeIntValue(
                null, ATTR_PROXIMITY, DEF_PROXIMITY_DISTANCE);

        initial_state = attrs.getAttributeValue(null, ATTR_INITIAL_STATE);
        initial_state = (initial_state != null ? initial_state :
                DEF_INITIAL_STATE);

        awake_state = attrs.getAttributeValue(null, ATTR_AWAKE_STATE);
        awake_state = (awake_state != null ? awake_state : DEF_AWAKE_STATE);

        move_state_prefix =
                attrs.getAttributeValue(null, ATTR_MOVE_STATE_PREFIX);
        move_state_prefix = (move_state_prefix != null ? move_state_prefix :
                DEF_MOVE_STATE_PREFIX);

        wall_state_prefix =
                attrs.getAttributeValue(null, ATTR_WALL_STATE_PREFIX);
        wall_state_prefix = (wall_state_prefix != null ? wall_state_prefix :
                DEF_WALL_STATE_PREFIX);

        int depth = xml.getDepth();
        while (true) {
            int type = xml.next();
            if (type == XmlPullParser.END_DOCUMENT ||
                    (type == XmlPullParser.END_TAG && depth >= xml.getDepth())) {
                break;
            }
            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            String name = xml.getName();
            if (TAG_MOTION.equals(name)) {
                parseMotion(res, xml, attrs);
            } else {
                throw new IllegalArgumentException("unknown tag: " + name);
            }
        }
    }

    public void parseMotion(Resources res,
                            XmlPullParser xml, AttributeSet attrs)
            throws XmlPullParserException, IOException {
        Motion motion = new Motion();

        motion.name = attrs.getAttributeValue(null, ATTR_STATE);
        if (motion.name == null) {
            throw new IllegalArgumentException(
                    "state is not specified: " + attrs.getPositionDescription());
        }

        int duration = attrs.getAttributeIntValue(null, ATTR_DURATION, -1);
        motion.next_state = attrs.getAttributeValue(
                null, ATTR_NEXT_STATE);
        motion.check_move = attrs.getAttributeBooleanValue(
                null, ATTR_CHECK_MOVE, false);
        motion.check_wall = attrs.getAttributeBooleanValue(
                null, ATTR_CHECK_WALL, false);

        motion.items = new MotionDrawable();

        int depth = xml.getDepth();
        while (true) {
            int type = xml.next();
            if (type == XmlPullParser.END_DOCUMENT ||
                    (type == XmlPullParser.END_TAG && depth >= xml.getDepth())) {
                break;
            }
            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            String name = xml.getName();
            if (TAG_ITEM.equals(name)) {
                parseItem(res, motion.items, xml, attrs);
            } else if (TAG_REPEAT_ITEM.equals(name)) {
                parseRepeatItem(res, motion.items, xml, attrs);
            } else {
                throw new IllegalArgumentException("unknown tag: " + name);
            }
        }

        motion.items.setTotalDuration(duration);
        motion.items.setRepeatCount(1);

        motions.put(motion.name, motion);
    }

    public void parseItem(Resources res, MotionDrawable items,
                          XmlPullParser xml, AttributeSet attrs) {
        int duration = attrs.getAttributeIntValue(
                null, ATTR_ITEM_DURATION, -1);
        String filename = attrs.getAttributeValue(null, ATTR_ITEM_DRAWABLE);

        int start = attrs.getAttributeIntValue(null, "start", 0);
        int end = attrs.getAttributeIntValue(null, "end", -1);
        if (end > start) {
            String fmt = resourceBaseDir + "/" + filename.replaceFirst("\\d+$", "") + "%03d.png";
            Drawable d;
            for (int i = start; i <= end; i++) {
                String p = String.format(Locale.KOREAN, fmt, i);
                d = Drawable.createFromPath(p);
                items.addFrame(d, duration);
            }
        } else {
            Drawable d = Drawable.createFromPath(resourceBaseDir + "/" + filename + ".png");
            assert d != null;
            items.addFrame(d, duration);
        }
    }

    public void parseRepeatItem(Resources res, MotionDrawable items,
                                XmlPullParser xml, AttributeSet attrs)
            throws XmlPullParserException, IOException {
        int duration = attrs.getAttributeIntValue(
                null, ATTR_ITEM_DURATION, -1);
        int repeat = attrs.getAttributeIntValue(
                null, ATTR_ITEM_REPEAT_COUNT, -1);
        MotionDrawable dr = new MotionDrawable();

        int depth = xml.getDepth();
        while (true) {
            int type = xml.next();
            if (type == XmlPullParser.END_DOCUMENT ||
                    (type == XmlPullParser.END_TAG && depth >= xml.getDepth())) {
                break;
            }
            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            String name = xml.getName();
            if (TAG_ITEM.equals(name)) {
                parseItem(res, dr, xml, attrs);
            } else if (TAG_REPEAT_ITEM.equals(name)) {
                parseRepeatItem(res, dr, xml, attrs);
            } else {
                throw new IllegalArgumentException("unknown tag: " + name);
            }
        }

        dr.setTotalDuration(duration);
        dr.setRepeatCount(repeat);
        items.addFrame(dr, -1);
    }
}
