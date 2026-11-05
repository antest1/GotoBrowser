package com.antest1.gotobrowser.Subtitle;

import com.google.gson.JsonObject;

public class SubtitleProviderUtils {
    private final static Kc3SubtitleProvider kc3SubtitleProvider = new Kc3SubtitleProvider();

    private final static KcwikiSubtitleProvider kcwikiSubtitleProvider = new KcwikiSubtitleProvider();

    private static SubtitleProvider currentProvider = kc3SubtitleProvider;

    public static SubtitleProvider getCurrentSubtitleProvider() {
        return currentProvider;
    }

    // Feed every provider, so switching the subtitle locale mid-session keeps the ship mapping.
    public static void loadKcApiData(JsonObject api_data) {
        kc3SubtitleProvider.loadKcApiData(api_data);
        kcwikiSubtitleProvider.loadKcApiData(api_data);
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
