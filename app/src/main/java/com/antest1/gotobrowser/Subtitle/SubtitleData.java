package com.antest1.gotobrowser.Subtitle;

public class SubtitleData {
    public SubtitleData(String text, int delay) {
        this.text = text;
        this.delay = delay;
    }

    public SubtitleData(String text, int delay, Long extraDelay) {
        this.text = text;
        this.delay = delay;
        this.extraDelay = extraDelay;
    }

    private String text;

    private int delay;

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
}
