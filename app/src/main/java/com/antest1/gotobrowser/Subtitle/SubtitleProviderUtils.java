package com.antest1.gotobrowser.Subtitle;

public class SubtitleProviderUtils {
    private final static Kc3SubtitleProvider kc3SubtitleProvider = new Kc3SubtitleProvider();

    private final static KcwikiSubtitleProvider kcwikiSubtitleProvider = new KcwikiSubtitleProvider();

    private static SubtitleProvider currentProvider = kc3SubtitleProvider;

    public static SubtitleProvider getCurrentSubtitleProvider() {
        return currentProvider;
    }

    public static SubtitleProvider getSubtitleProvider(String subtitleLocale) {
        return switch (subtitleLocale) {
            case "zh-tw", "zh-cn" -> {
                currentProvider = kcwikiSubtitleProvider;
                yield kcwikiSubtitleProvider;
            }
            default -> {
                currentProvider = kc3SubtitleProvider;
                yield kc3SubtitleProvider;
            }
        };
    }
}
