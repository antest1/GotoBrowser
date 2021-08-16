package com.antest1.gotobrowser.Subtitle;

public class SubtitleProviderUtils {
    private final static Kc3SubtitleProvider kc3SubtitleProvider = new Kc3SubtitleProvider();

    private final static KcwikiSubtitleProvider kcwikiSubtitleProvider = new KcwikiSubtitleProvider();

    private static SubtitleProvider currentProvider = kc3SubtitleProvider;

    public static SubtitleProvider getCurrentSubtitleProvider() {
        return currentProvider;
    }

    public static SubtitleProvider getSubtitleProvider(String subtitleLocale) {
        switch (subtitleLocale) {
            default:
            case "en":
            case "kr":
            case "jp":
                currentProvider = kc3SubtitleProvider;
                return kc3SubtitleProvider;
            case "zh-tw":
            case "zh-cn":
                currentProvider = kcwikiSubtitleProvider;
                return kcwikiSubtitleProvider;
        }
    }
}
