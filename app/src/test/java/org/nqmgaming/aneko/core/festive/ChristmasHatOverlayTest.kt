package org.nqmgaming.aneko.core.festive

import org.junit.Assert.assertEquals
import org.junit.Test

class ChristmasHatOverlayTest {

    @Test
    fun getHatPose_mapsFrontPosesCorrectly() {
        assertEquals(ChristmasHatOverlay.HatPose.FRONT, ChristmasHatOverlay.getHatPose("mati1"))
        assertEquals(ChristmasHatOverlay.HatPose.FRONT, ChristmasHatOverlay.getHatPose("mati2"))
        assertEquals(ChristmasHatOverlay.HatPose.FRONT, ChristmasHatOverlay.getHatPose("awake"))
        assertEquals(ChristmasHatOverlay.HatPose.FRONT, ChristmasHatOverlay.getHatPose("akubi1"))
        assertEquals(ChristmasHatOverlay.HatPose.FRONT, ChristmasHatOverlay.getHatPose("akubi2"))
        assertEquals(ChristmasHatOverlay.HatPose.FRONT, ChristmasHatOverlay.getHatPose("akubi_l"))
        assertEquals(ChristmasHatOverlay.HatPose.FRONT, ChristmasHatOverlay.getHatPose("akubi_r"))
        assertEquals(ChristmasHatOverlay.HatPose.FRONT, ChristmasHatOverlay.getHatPose("icon"))
        assertEquals(ChristmasHatOverlay.HatPose.FRONT, ChristmasHatOverlay.getHatPose(null))
        assertEquals(ChristmasHatOverlay.HatPose.FRONT, ChristmasHatOverlay.getHatPose("unknown"))
    }

    @Test
    fun getHatPose_mapsKakiPoseCorrectly() {
        assertEquals(ChristmasHatOverlay.HatPose.KAKI, ChristmasHatOverlay.getHatPose("kaki1"))
        assertEquals(ChristmasHatOverlay.HatPose.KAKI, ChristmasHatOverlay.getHatPose("kaki2"))
    }

    @Test
    fun getHatPose_mapsSleepPoseCorrectly() {
        assertEquals(ChristmasHatOverlay.HatPose.SLEEP, ChristmasHatOverlay.getHatPose("sleep1"))
        assertEquals(ChristmasHatOverlay.HatPose.SLEEP, ChristmasHatOverlay.getHatPose("sleep2"))
    }

    @Test
    fun getHatPose_mapsRightPosesCorrectly() {
        assertEquals(ChristmasHatOverlay.HatPose.RIGHT, ChristmasHatOverlay.getHatPose("right1"))
        assertEquals(ChristmasHatOverlay.HatPose.RIGHT, ChristmasHatOverlay.getHatPose("right2"))
        assertEquals(ChristmasHatOverlay.HatPose.RIGHT, ChristmasHatOverlay.getHatPose("dwright1"))
        assertEquals(ChristmasHatOverlay.HatPose.RIGHT, ChristmasHatOverlay.getHatPose("dwright2"))
        assertEquals(ChristmasHatOverlay.HatPose.RIGHT, ChristmasHatOverlay.getHatPose("rtogi1"))
        assertEquals(ChristmasHatOverlay.HatPose.RIGHT, ChristmasHatOverlay.getHatPose("rtogi2"))
    }

    @Test
    fun getHatPose_mapsLeftPosesCorrectly() {
        assertEquals(ChristmasHatOverlay.HatPose.LEFT, ChristmasHatOverlay.getHatPose("left1"))
        assertEquals(ChristmasHatOverlay.HatPose.LEFT, ChristmasHatOverlay.getHatPose("left2"))
        assertEquals(ChristmasHatOverlay.HatPose.LEFT, ChristmasHatOverlay.getHatPose("dwleft1"))
        assertEquals(ChristmasHatOverlay.HatPose.LEFT, ChristmasHatOverlay.getHatPose("dwleft2"))
        assertEquals(ChristmasHatOverlay.HatPose.LEFT, ChristmasHatOverlay.getHatPose("ltogi1"))
        assertEquals(ChristmasHatOverlay.HatPose.LEFT, ChristmasHatOverlay.getHatPose("ltogi2"))
    }

    @Test
    fun getHatPose_mapsDiagonalPosesCorrectly() {
        assertEquals(ChristmasHatOverlay.HatPose.UPRIGHT, ChristmasHatOverlay.getHatPose("upright1"))
        assertEquals(ChristmasHatOverlay.HatPose.UPRIGHT, ChristmasHatOverlay.getHatPose("upright2"))
        assertEquals(ChristmasHatOverlay.HatPose.UPLEFT, ChristmasHatOverlay.getHatPose("upleft1"))
        assertEquals(ChristmasHatOverlay.HatPose.UPLEFT, ChristmasHatOverlay.getHatPose("upleft2"))
    }

    @Test
    fun getHatPose_mapsUpAndDownPosesCorrectly() {
        assertEquals(ChristmasHatOverlay.HatPose.UP, ChristmasHatOverlay.getHatPose("up1"))
        assertEquals(ChristmasHatOverlay.HatPose.UP, ChristmasHatOverlay.getHatPose("up2"))
        assertEquals(ChristmasHatOverlay.HatPose.UP, ChristmasHatOverlay.getHatPose("utogi1"))
        assertEquals(ChristmasHatOverlay.HatPose.UP, ChristmasHatOverlay.getHatPose("utogi2"))

        assertEquals(ChristmasHatOverlay.HatPose.DOWN, ChristmasHatOverlay.getHatPose("down1"))
        assertEquals(ChristmasHatOverlay.HatPose.DOWN, ChristmasHatOverlay.getHatPose("down2"))
        assertEquals(ChristmasHatOverlay.HatPose.DOWN, ChristmasHatOverlay.getHatPose("dtogi1"))
        assertEquals(ChristmasHatOverlay.HatPose.DOWN, ChristmasHatOverlay.getHatPose("dtogi2"))
    }
}
