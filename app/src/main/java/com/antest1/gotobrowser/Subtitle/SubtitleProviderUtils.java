package com.antest1.gotobrowser.Subtitle;

public class SubtitleProviderUtils {
    private final static Kc3SubtitleProvider kc3SubtitleProvider = new Kc3SubtitleProvider();

    public static SubtitleProvider getCurrentSubtitleProvider() {
        return kc3SubtitleProvider;
    }

    public static SubtitleProvider getSubtitleProvider(String subtitleLocale) {
        return kc3SubtitleProvider;
    }


    public static Kc3SubtitleProvider getKc3SubtitleProvider() {
        return kc3SubtitleProvider;
    }
}
