package com.antest1.gotobrowser;

public class Constants {
    public static final String PREF_LANDSCAPE = "pref_landscape";
    public static final String PREF_ADJUSTMENT = "pref_adjustment";
    public static final String PREF_CONNECTOR = "pref_connector";

    public static final String CONN_OOI = "ooi.moe";
    public static final String CONN_NITRABBIT = "nitrabbit";

    public static final String URL_OOI = "http://ooi.moe/";
    public static final String URL_NITRABBIT = "http://connector.usagi.space/kancolle/";

    public static final String[] URL_LIST = {URL_OOI, URL_NITRABBIT};

    public static final String URL_OSAPI = "osapi.dmm.com/gadgets/";
    public static final String RESIZE_OSAPI = "function resize(){$(\"#spacing_top\").remove(),$(\"#sectionWrap\").hide(),$(\"body\")[0].style.backgroundColor=\"black\",$(\"body\")[0].style.overflow=\"hidden\",$(\"body\")[0].style.margin=\"0px\",$(\"body\")[0].style.padding=\"0px\";var a=1200,b=720,c=window.innerWidth,d=window.innerHeight;a/b<c/d&&$(\"#flashWrap iframe\").css(\"padding\",\"0 48px\"),console.log(a+\" \"+b+\" / \"+c+\" \"+d)}setTimeout(resize,1500);";

    public static final String CONNECT_NITRABBIT = "$(\"#viewform\").unbind(\"submit\");function connect(){var a=$(\"#viewform input[name=\\\"game_url\\\"]\").val();return location.href=a,!1}$(\"#viewform\").submit(connect);";
}
