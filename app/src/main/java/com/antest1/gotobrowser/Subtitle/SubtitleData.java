package com.antest1.gotobrowser.Subtitle;

public class SubtitleData {
    public SubtitleData(String text, int delay) {
        this.text = text;
        this.delay = delay;
    }

    private String text;

    private int delay;

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
}
