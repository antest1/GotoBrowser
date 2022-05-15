package com.antest1.gotobrowser;

public class Constants {
    public static final int VERSION_TABLE_VERSION = 1;
    public static final int CACHE_SIZE_BYTES = 1024 * 1024 * 2;

    public static final int REQUEST_EXTERNAL_PERMISSION = 100;

    public static final String PREF_LANDSCAPE = "pref_landscape";
    public static final String PREF_ADJUSTMENT = "pref_adjustment";
    public static final String PREF_CONNECTOR = "pref_connector";
    public static final String PREF_SILENT = "pref_silent";
    public static final String PREF_BROADCAST = "pref_broadcast";
    public static final String PREF_PANELSTART = "pref_panelstart";
    public static final String PREF_KEYBOARD = "pref_keyboard";
    public static final String PREF_MUTEMODE = "pref_mutemode";
    public static final String PREF_CAPTURE = "pref_camera";
    public static final String PREF_LOCKMODE = "pref_lockmode";
    public static final String PREF_KEEPMODE = "pref_keepmode";
    public static final String PREF_SHOWCC = "pref_showcc";
    public static final String PREF_LATEST_URL = "pref_latest_url";
    public static final String PREF_DMM_ID = "pref_autocomplete_id";
    public static final String PREF_DMM_PASS = "pref_autocomplete_pass";
    public static final String PREF_SUBTITLE_LOCALE = "pref_subtitle_locale";
    public static final String PREF_SUBTITLE_UPDATE = "pref_subtitle_update";
    public static final String PREF_FONT_PREFETCH = "pref_font_prefetch";
    public static final String PREF_PIP_MODE = "pref_pip_mode";
    public static final String PREF_ALTER_GADGET = "pref_alter_gadget";
    public static final String PREF_NC_SCREENSHOT_SET = "perf_nc_screenshot_set";
    public static final String PREF_APP_VERSION = "pref_app_version";
    public static final String PREF_CHECK_UPDATE = "pref_check_update";
    public static final String PREF_PANEL_METHOD = "pref_panel_method";
    public static final String PREF_MULTIWIN_MARGIN = "pref_multiwin_margin";
    public static final String PREF_DEVTOOLS_DEBUG = "pref_devtools_debug";
    public static final String PREF_ALTER_METHOD = "pref_alter_method";
    public static final String PREF_ALTER_ENDPOINT = "pref_alter_endpoint";
    public static final String PREF_TP_DISCLAIMED = "pref_tp_disclaimed";
    public static final String PREF_LEGACY_RENDERER = "pref_legacy_renderer";
    public static final String PREF_MOD_KANTAI3D = "pref_mod_kantai3d";
    public static final String PREF_MOD_FPS = "pref_mod_fps";
    public static final String PREF_MOD_CRIT = "pref_mod_crit";
    public static final String PREF_USE_EXTCACHE = "pref_use_extcache";
    public static final String PREF_UI_HELP_CHECKED = "pref_ui_help_checked";
    public static final String PREF_DOWNLOAD_RETRY = "pref_retry";

    public static final String[] PREF_SETTINGS = {
            PREF_FONT_PREFETCH,
            PREF_PIP_MODE,
            PREF_ALTER_GADGET,
            PREF_SUBTITLE_LOCALE,
            PREF_MULTIWIN_MARGIN,
            PREF_PANEL_METHOD,
            PREF_DEVTOOLS_DEBUG,
            PREF_ALTER_METHOD,
            PREF_ALTER_ENDPOINT,
            PREF_DOWNLOAD_RETRY,
            PREF_TP_DISCLAIMED,
            PREF_LEGACY_RENDERER,
            PREF_MOD_KANTAI3D,
            PREF_MOD_FPS,
            PREF_MOD_CRIT,
            PREF_USE_EXTCACHE
    };

    public static final String PANEL_METHOD_SWIPE = "0";
    public static final String PANEL_METHOD_BUTTON = "1";

    public static final String PREF_ALTER_METHOD_URL = "1";
    public static final String PREF_ALTER_METHOD_PROXY = "2";

    public static final String ACTION_SHOWPANEL = "with_layout_control_";
    public static final String ACTION_SHOWKEYBOARD = "with_keyboard_";

    public static final String CONN_DMM = "DMM direct";
    public static final String CONN_KANSU = "kancolle.su";
    public static final String CONN_OOI = "ooi.moe";
    public static final String CONN_NITRABBIT = "nitrabbit";

    public static final String URL_DMM = "http://www.dmm.com/netgame/social/-/gadgets/=/app_id=854854/";
    public static final String URL_OOI = "http://ooi.moe/";
    public static final String URL_KANSU = "http://kancolle.su/";
    public static final String URL_NITRABBIT = "http://connector.usagi.space/kancolle/";

    public static final String[] URL_LIST = {URL_DMM, URL_KANSU, URL_OOI};

    public static final String URL_DMM_LOGIN = "www.dmm.com/my/-/login/";
    public static final String URL_DMM_LOGIN_2 = "accounts.dmm.com/service/login/password/";
    public static final String URL_DMM_FOREIGN = "www.dmm.com/netgame/foreign";
    public static final String URL_DMM_POINT = "point.dmm.com/choice";
    public static final String URL_OSAPI = "osapi.dmm.com/gadgets/";
    public static final String URL_OOI_1 = "ooi.moe/kancolle";
    public static final String URL_KANSU_1 = "kancolle.su/kancolle";
    public static final String URL_DMM_LOGOUT = "https://www.dmm.com/my/-/login/logout/=/path=Sg9VTQFXDFcXFl5bWlcKGExKUVdUXgFNEU0KSVMVR28MBQ0BUwJZBwxK";
    public static final String URL_OOI_LOGOUT = "http://ooi.moe/logout";
    public static final String URL_KANSU_LOGOUT = "http://kancolle.su/logout";
    public static final String DMM_REDIRECT_CODE = "Sg9VTQFXDFcXFl5bWlcKGExKUVdUXgFNEU0KSVMVR28MBQ0BUwJZBwxK";

    public static final String ADD_VIEWPORT_META = "var metaTag=document.createElement('meta');metaTag.name='viewport',metaTag.content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=yes',document.getElementsByTagName('head')[0].appendChild(metaTag);";

    public static final String REFRESH_DETECT_CALL = "(function(){return document.getElementById(\"flashWrap\")==null})();";
    public static final String MUTE_SEND_DMM = "(function(){var msg={sound:%d};var origin=\"*\";document.getElementById(\"game_frame\").contentWindow.postMessage(msg,origin);return \"done\"})()";
    public static final String MUTE_SEND_OOI = "(function(){var msg={sound:%d};var origin=\"*\";document.getElementById(\"externalswf\").contentWindow.postMessage(msg,origin);return \"done\"})()";
    public static final String MUTE_SEND = "(function(){var msg={sound:%d};var origin=\"*\";var doc=document.getElementById(\"htmlWrap\");if(doc){doc.contentWindow.postMessage(msg,origin)}else{document.getElementsByTagName(\"iframe\")[0].contentWindow.postMessage(msg,origin)};return \"done\"})()";
    public static final String MUTE_LISTEN = "\nwindow.addEventListener(\"message\",function(e){(e.data.sound!=null)&&(global_mute=e.data.sound,Howler.mute(global_mute),(!global_mute&&gb_h&&gb_h&&!gb_h.playing())&&gb_h.play())});";
    public static final String DMM_COOKIE = "document.cookie='ckcy=1;expires=Thu, 16-Jan-2023 00:00:00 GMT;path=/netgame;domain=.dmm.com';";
    public static final String CAPTURE_SEND_DMM = "(function(){var msg={capture:true};var origin=\"*\";var doc=document.getElementById(\"game_frame\");if(doc){doc.contentWindow.postMessage(msg,origin)}else{document.getElementsByTagName(\"iframe\")[0].contentWindow.postMessage(msg,origin)};return\"done\"})()";
    public static final String CAPTURE_SEND_OOI = "(function(){var msg={capture:true};var origin=\"*\";var doc=document.getElementById(\"externalswf\");if(doc){doc.contentWindow.postMessage(msg,origin)}else{document.getElementsByTagName(\"iframe\")[0].contentWindow.postMessage(msg,origin)};return\"done\"})()";
    public static final String CAPTURE_SEND = "(function(){var msg={capture:true};var origin=\"*\";var doc=document.getElementById(\"htmlWrap\");if(doc){doc.contentWindow.postMessage(msg,origin)}else{document.getElementsByTagName(\"iframe\")[0].contentWindow.postMessage(msg,origin)};return\"done\"})()";
    public static final String CAPTURE_LISTEN = "window.addEventListener(\"message\",function(e){if(e.data.capture!=null){(async function(){{let canvas=document.querySelector('canvas');requestAnimationFrame(()=>{{if(canvas!=null){let dataurl=canvas.toDataURL('image/png');GotoBrowser.kcs_process_canvas_dataurl(dataurl);}}});}})();}});";

    public static final String CONNECT_NITRABBIT = "$(\"#viewform\").unbind(\"submit\");function connect(){var a=$(\"#viewform input[name=\\\"game_url\\\"]\").val();return location.href=a,!1}$(\"#viewform\").submit(connect);";
    public static final String AUTOCOMPLETE_OOI = "$('input[name=\"login_id\"]').val(\"%s\");$('input[name=\"password\"]').val(\"%s\");";
    public static final String AUTOCOMPLETE_NIT = "$(\"input[name=id]\").val(\"%s\");$(\"input[name=pw]\").val(\"%s\");";

    public static final String BROWSER_USERAGENT = String.format("Goto/%s ", BuildConfig.VERSION_NAME);

    public static final String GITHUBAPI_ROOT = "https://api.github.com/";
    public static final String SUBTITLE_ROOT = "https://raw.githubusercontent.com/";
    public static final String SUBTITLE_SIZE_PATH = "src/data/quotes_size.json";
    public static final String SUBTITLE_PATH_FORMAT = "data/%s/quotes.json";

    public static final String[] REQUEST_BLOCK_RULES = {
            "twitter.com/i/jot",
            "dmm.com/latest/js/dmm.tracking",
            "doubleclick.net",
            "googletagmanager.com/",
            "facebook.com",
            "pics.dmm.com/",
            "/uikit"
    };

    public static final String[] KANCOLLE_SERVER_LIST = {
            "203.104.209.71",
            "203.104.209.87",
            "125.6.184.215",
            "203.104.209.183",
            "203.104.209.150",
            "203.104.209.134",
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

    public static final String[] OOI_SERVER_LIST = {
            "ooi.moe",
            "kancolle.su"
    };

    public static final String GADGET_URL = "http://203.104.209.7/";
    public static final String DEFAULT_ALTER_GADGET_URL = "http://luckyjervis.com/";
    public static final String APP_UI_HELP_VER = "20211002";
}
