package com.antest1.gotobrowser.Subtitle;

public class SubtitleData {
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
