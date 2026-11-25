package com.antest1.gotobrowser.Subtitle

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleDataTest {
    @Test
    fun everyLineBreakTagFormBecomesANewLine() {
        listOf("<br>", "<br/>", "<br />", "</br>", "< br />", "<BR>").forEach { tag ->
            assertEquals(tag, "first\nsecond", SubtitleData.toDisplayText("first${tag}second"))
        }
    }

    @Test
    fun spacesAroundATagAreDropped() {
        assertEquals("first\n(second)", SubtitleData.toDisplayText("first <br> (second)"))
    }

    @Test
    fun tagsAtTheEndsLeaveNoEmptyLines() {
        assertEquals("first", SubtitleData.toDisplayText("<br/>first<br/>"))
    }

    @Test
    fun italicTagsAreRemoved() {
        assertEquals("So, what do you think?", SubtitleData.toDisplayText("So, what do <i>you</i> think?"))
        assertEquals("Then as you wish. Just do what you want.",
            SubtitleData.toDisplayText("<i>Then as you wish.</i> Just do what you want."))
    }

    @Test
    fun textWithoutTagsIsUnchanged() {
        assertEquals("a < b > c, (・∀・)<", SubtitleData.toDisplayText("a < b > c, (・∀・)<"))
    }
}
