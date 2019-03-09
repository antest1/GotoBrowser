package com.antest1.gotobrowser;

public class Constants {
    public static final int VERSION_TABLE_VERSION = 1;

    public static final String PREF_LANDSCAPE = "pref_landscape";
    public static final String PREF_ADJUSTMENT = "pref_adjustment";
    public static final String PREF_CONNECTOR = "pref_connector";
    public static final String PREF_SILENT = "pref_silent";
    public static final String PREF_PADDING = "pref_padding";
    public static final String PREF_LATEST_URL = "pref_latest_url";
    public static final String PREF_DMM_ID = "pref_autocomplete_id";
    public static final String PREF_DMM_PASS = "pref_autocomplete_pass";

    public static final String ACTION_WITHLC = "with_layout_control";

    public static final String CONN_DMM = "DMM direct";
    public static final String CONN_KANSU = "kancolle.su";
    public static final String CONN_OOI = "ooi.moe";
    public static final String CONN_NITRABBIT = "nitrabbit";

    public static final String URL_DMM = "http://www.dmm.com/netgame/social/-/gadgets/=/app_id=854854/";
    public static final String URL_OOI = "http://ooi.moe/";
    public static final String URL_KANSU = "http://kancolle.su/";
    public static final String URL_NITRABBIT = "http://connector.usagi.space/kancolle/";

    public static final String[] URL_LIST = {URL_DMM, URL_KANSU, URL_OOI, URL_NITRABBIT};

    public static final String URL_DMM_LOGIN = "www.dmm.com/my/-/login/";
    public static final String URL_DMM_FOREIGN = "www.dmm.com/netgame/foreign";
    public static final String URL_DMM_POINT = "point.dmm.com/choice";
    public static final String URL_OSAPI = "osapi.dmm.com/gadgets/";
    public static final String RESIZE_OSAPI = "var width=1200,height=720,game_ratio=1200/height;function resize(a){resize_triggered=!0;var b=window.innerWidth,c=window.innerHeight;game_ratio<b/c?$(\"#flashWrap iframe\").css(\"padding\",\"0 \"+a+\"px\"):$(\"#flashWrap iframe\").css(\"padding\",\"0px\")}$(\"body\")[0].style.backgroundColor=\"black\",$(\"body\")[0].style.overflow=\"hidden\",$(\"body\")[0].style.margin=\"0px\",$(\"body\")[0].style.padding=\"0px\",resize_triggered=!1,setTimeout(function(){$(\"#spacing_top\").remove(),$(\"#sectionWrap\").hide()},1500),setTimeout(resize,1500,%d);";
    public static final String RESIZE_DMM = "var width=1200,height=720,game_ratio=width/height;function resize(e){resize_triggered=!0;var i=window.innerWidth,a=window.innerHeight;game_ratio<i/a?$(\"#game_frame\").css(\"padding\",\"0 \"+e+\"px\"):$(\"#game_frame\").css(\"padding\",\"0px\")}$(\"#foot, #ntg-recommend\").remove(),$(\"img, .dmm-ntgnavi, .area-naviapp, .mg-b10\").remove(),$(\"#page, body\").css(\"background-color\",\"black\"),$(\"#main-ntg\").css(\"padding\",\"0px\"),$(\"#game_frame\").height(736),$(\"#game_frame\").css(\"margin-top\",\"-16px\"),resize_triggered=!1,setTimeout(resize,300,%d);";
    public static final String RESIZE_CALL = "resize_triggered&&resize(%d);";
    public static final String REFRESH_CALL = "(function(){return document.getElementById(\"flashWrap\")==null})();";

    public static final String DMM_COOKIE = "document.cookie='ckcy=1;expires=Thu, 16-Jan-2023 00:00:00 GMT;path=/netgame;domain=.dmm.com';";

    public static final String CONNECT_NITRABBIT = "$(\"#viewform\").unbind(\"submit\");function connect(){var a=$(\"#viewform input[name=\\\"game_url\\\"]\").val();return location.href=a,!1}$(\"#viewform\").submit(connect);";
    public static final String AUTOCOMPLETE_OOI = "$(\"#login_id\").val(\"%s\");$(\"#password\").val(\"%s\");";
    public static final String AUTOCOMPLETE_NIT = "$(\"input[name=id]\").val(\"%s\");$(\"input[name=pw]\").val(\"%s\");";

    public static final String[] REQUEST_BLOCK_RULES = {
        "twitter.com/i/jot",
        "dmm.com/latest/js/dmm.tracking",
        "doubleclick.net",
        "googletagmanager.com/",
        "facebook.com",
        "pics.dmm.com/"
    };

    public static final String[] SERVER_LIST = {
            "203.104.209.71",
            "203.104.209.87",
            "125.6.184.215",
            "203.104.209.183",
            "203.104.209.150",
            "203.104.209.150",
            "203.104.209.167",
            "203.104.209.199",
            "125.6.189.7",
            "125.6.189.39",
            "125.6.189.71",
            "125.6.189.103",
            "125.6.189.135",
            "125.6.189.167",
            "125.6.189.215",
            "125.6.189.247",
            "203.104.209.23",
            "203.104.209.39",
            "203.104.209.55",
            "203.104.209.102"
    };
}
