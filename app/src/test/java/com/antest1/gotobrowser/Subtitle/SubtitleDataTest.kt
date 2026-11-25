package com.antest1.gotobrowser.Subtitle

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleDataTest {
    @Test
    fun everyLineBreakTagFormBecomesANewLine() {
        listOf("<br>", "<br/>", "<br />", "</br>", "< br />", "<BR>").forEach { tag ->
            assertEquals(tag, "first\nsecond", SubtitleData.withLineBreaks("first${tag}second"))
        }
    }

    @Test
    fun spacesAroundATagAreDropped() {
        assertEquals("first\n(second)", SubtitleData.withLineBreaks("first <br> (second)"))
    }

    @Test
    fun tagsAtTheEndsLeaveNoEmptyLines() {
        assertEquals("first", SubtitleData.withLineBreaks("<br/>first<br/>"))
    }

    @Test
    fun textWithoutTagsIsUnchanged() {
        assertEquals("a < b > c", SubtitleData.withLineBreaks("a < b > c"))
    }
}
