package com.antest1.gotobrowser.Subtitle;

import java.util.regex.Pattern;

public class SubtitleData {
    // Every line-break tag form found in the subtitle data: <br>, <br/>, <br />, </br>, < br />.
    private static final Pattern LINE_BREAK_TAG =
            Pattern.compile("[ \\t]*<\\s*/?\\s*br\\s*/?\\s*>[ \\t]*", Pattern.CASE_INSENSITIVE);
    // Emphasis tags (<i>, </i>); the subtitle is drawn as plain text, so they are dropped.
    private static final Pattern ITALIC_TAG =
            Pattern.compile("<\\s*/?\\s*i\\s*>", Pattern.CASE_INSENSITIVE);

    /**
     * Prepares subtitle text for display: line-break tags become new lines (without the spaces
     * around them) and emphasis tags are removed.
     */
    public static String toDisplayText(String text) {
        String plain = ITALIC_TAG.matcher(text).replaceAll("");
        return LINE_BREAK_TAG.matcher(plain).replaceAll("\n").trim();
    }

    public SubtitleData(String text, int delay, int duration) {
        this.text = text;
        this.delay = delay;
        this.duration = duration;
    }

    private String text;

    private int delay;

    private int duration;

    private Long extraDelay = null;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public Long getExtraDelay() {
        return extraDelay;
    }

    public void setExtraDelay(Long extraDelay) {
        this.extraDelay = extraDelay;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
