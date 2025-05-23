package com.antest1.gotobrowser;

public class Constants {
    public static final int VERSION_TABLE_VERSION = 1;
    public static final int CACHE_SIZE_BYTES = 1024 * 1024 * 2;

    public static final int REQUEST_NOTIFICATION_PERMISSION = 100;

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
    public static final String PREF_DISABLE_REFRESH_DIALOG = "pref_disable_refresh_dialog";
    public static final String PREF_LATEST_URL = "pref_latest_url";
    public static final String PREF_DMM_ID = "pref_autocomplete_id";
    public static final String PREF_DMM_PASS = "pref_autocomplete_pass";
    public static final String PREF_SUBTITLE_LOCALE = "pref_subtitle_locale";
    public static final String PREF_SUBTITLE_UPDATE = "pref_subtitle_update";
    public static final String PREF_FONT_PREFETCH = "pref_font_prefetch";
    public static final String PREF_PIP_MODE = "pref_pip_mode";
    public static final String PREF_ALTER_GADGET = "pref_alter_gadget";
    public static final String PREF_APP_VERSION = "pref_app_version";
    public static final String PREF_CHECK_UPDATE = "pref_check_update";
    public static final String PREF_MULTIWIN_MARGIN = "pref_multiwin_margin";
    public static final String PREF_DEVTOOLS_DEBUG = "pref_devtools_debug";
    public static final String PREF_ALTER_METHOD = "pref_alter_method";
    public static final String PREF_ALTER_ENDPOINT = "pref_alter_endpoint";
    public static final String PREF_TP_DISCLAIMED = "pref_tp_disclaimed";
    public static final String PREF_LEGACY_RENDERER = "pref_legacy_renderer";
    public static final String PREF_MOD_KANTAI3D = "pref_mod_kantai3d";
    public static final String PREF_MOD_KANTAIEN = "pref_mod_kantaien";
    public static final String PREF_MOD_KANTAIEN_UPDATE = "pref_mod_kantaien_update";
    public static final String PREF_MOD_KANTAIEN_DELETE = "pref_mod_kantaien_delete";
    public static final String PREF_MOD_FPS = "pref_mod_fps";
    public static final String PREF_MOD_CRIT = "pref_mod_crit";
    public static final String PREF_USE_EXTCACHE = "pref_use_extcache";
    public static final String PREF_UI_HELP_CHECKED = "pref_ui_help_checked";
    public static final String PREF_DOWNLOAD_RETRY = "pref_retry";
    public static final String PREF_CURSOR_MODE = "pref_cursor_mode";
    public static final String PREF_SUBTITLE_FONTSIZE = "pref_subtitle_size";

    public static final String[] PREF_SETTINGS = {
            PREF_LANDSCAPE,
            PREF_ADJUSTMENT,
            PREF_FONT_PREFETCH,
            PREF_BROADCAST,
            PREF_USE_EXTCACHE,
            PREF_PIP_MODE,
            PREF_MULTIWIN_MARGIN,
            PREF_LEGACY_RENDERER,
            PREF_ALTER_GADGET,
            PREF_ALTER_METHOD,
            PREF_ALTER_ENDPOINT,
            PREF_DOWNLOAD_RETRY,
            PREF_SUBTITLE_LOCALE,
            PREF_LEGACY_RENDERER,
            PREF_MOD_KANTAI3D,
            PREF_MOD_KANTAIEN,
            PREF_MOD_FPS,
            PREF_MOD_CRIT,
            PREF_DEVTOOLS_DEBUG,
            PREF_CURSOR_MODE,
            PREF_DISABLE_REFRESH_DIALOG,
            PREF_SUBTITLE_FONTSIZE
    };

    public static final String[] PREF_CLICK_SETTINGS = {
            PREF_CHECK_UPDATE,
            PREF_SUBTITLE_UPDATE,
            PREF_MOD_KANTAIEN_UPDATE,
            PREF_MOD_KANTAIEN_DELETE,
            PREF_SUBTITLE_FONTSIZE
    };

    public static final String PREF_ALTER_METHOD_URL = "1";
    public static final String PREF_ALTER_METHOD_PROXY = "2";

    public static final String ACTION_SHOWPANEL = "with_layout_control_";
    public static final String ACTION_SHOWKEYBOARD = "with_keyboard_";

    public static final String CONN_DMM = "DMM direct";
    public static final String CONN_KANMOE = "kancolle.moe";
    public static final String CONN_OOI = "ooi.moe";

    public static final String URL_DMM = "http://www.dmm.com/netgame/social/-/gadgets/=/app_id=854854/";
    public static final String URL_OOI = "https://ooi.moe/";
    public static final String URL_KANMOE = "https://kancolle.moe/";
    public static final String[] URL_LIST = {URL_DMM, URL_KANMOE, URL_OOI};

    public static final String URL_DMM_LOGIN = "www.dmm.com/my/-/login/";
    public static final String URL_DMM_LOGIN_2 = "accounts.dmm.com/service/login/password";
    public static final String URL_DMM_FOREIGN = "www.dmm.com/netgame/foreign";
    public static final String URL_DMM_FOREIGN_2 = "special.dmm.com/not-available-in-your-region";
    public static final String URL_OOI_1 = "ooi.moe/kancolle";
    public static final String URL_KANMOE_1 = "kancolle.moe/kancolle";
    public static final String URL_DMM_LOGOUT = "https://www.dmm.com/my/-/login/logout/=/path=Sg9VTQFXDFcXFl5bWlcKGExKUVdUXgFNEU0KSVMVR28MBQ0BUwJZBwxK";
    public static final String URL_OOI_LOGOUT = "https://ooi.moe/logout";
    public static final String URL_KANMOE_LOGOUT = "https://kancolle.moe/logout";

    public static final String ADD_VIEWPORT_META = "var metaTag=document.createElement('meta');metaTag.name='viewport',metaTag.content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0',document.getElementsByTagName('head')[0].appendChild(metaTag);";

    public static final String MUTE_SEND_DMM = "(function(){var msg={sound:%d};var origin=\"*\";var game_frame=document.getElementById(\"game_frame\");if(game_frame!=null){game_frame.contentWindow.postMessage(msg,origin)};return \"done\"})()";
    public static final String MUTE_SEND_OOI = "(function(){var msg={sound:%d};var origin=\"*\";var game_frame=document.getElementById(\"externalswf\");if(game_frame!=null){game_frame.contentWindow.postMessage(msg,origin)};return \"done\"})()";
    public static final String MUTE_LISTEN = "\nwindow.addEventListener(\"message\",function(e){(e.data.sound!=null)&&(global_mute=e.data.sound,Howler.mute(global_mute),(!global_mute&&gb_h&&gb_h&&!gb_h.playing())&&gb_h.play())});";
    public static final String DMM_COOKIE = "document.cookie='ckcy=1;expires={date};path=/netgame;domain=.dmm.com';";
    public static final String CAPTURE_SEND_DMM = "(function(){var msg={capture:true};var origin=\"*\";var doc=document.getElementById(\"game_frame\");if(doc){doc.contentWindow.postMessage(msg,origin)}else{document.getElementsByTagName(\"iframe\")[0].contentWindow.postMessage(msg,origin)};return\"done\"})()";
    public static final String CAPTURE_SEND_OOI = "(function(){var msg={capture:true};var origin=\"*\";var doc=document.getElementById(\"externalswf\");if(doc){doc.contentWindow.postMessage(msg,origin)}else{document.getElementsByTagName(\"iframe\")[0].contentWindow.postMessage(msg,origin)};return\"done\"})()";
    public static final String CAPTURE_LISTEN = "window.addEventListener(\"message\",function(e){if(e.data.capture!=null){(async function(){{let canvas=document.querySelector('canvas');requestAnimationFrame(()=>{{if(canvas!=null){let dataurl=canvas.toDataURL('image/png');GotoBrowser.kcs_process_canvas_dataurl(dataurl);}}});}})();}});";

    public static final String AUTOCOMPLETE_DMM = "function v(e,t){let o=Object.getOwnPropertyDescriptor(e,\"value\").set,s=Object.getPrototypeOf(e),l=Object.getOwnPropertyDescriptor(s,\"value\").set;o&&o!==l?l.call(e,t):o.call(e,t)}v(document.forms.loginForm.elements.login_id,\"%s\"),document.forms.loginForm.elements.login_id.dispatchEvent(new Event(\"input\",{bubbles:!0})),v(document.forms.loginForm.elements.password,\"%s\"),document.forms.loginForm.elements.password.dispatchEvent(new Event(\"input\",{bubbles:!0}));";
    public static final String AUTOCOMPLETE_OOI = "$('input[name=\"login_id\"]').val(\"%s\");$('input[name=\"password\"]').val(\"%s\");";

    public static final String ADJUST_JS = "((e,t)=>{let n=0;function d(){document.getElementById('game_frame').style.transform=`scale(${window.innerWidth/1200})`,document.getElementById('game_frame').style.transformOrigin='top center'}window.addEventListener('load',()=>{clearTimeout(n),n=setTimeout(d,10)}),window.addEventListener('resize',()=>{clearTimeout(n),n=setTimeout(d,10)}),d()})(document,window);";

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

    public static final String GADGET_URL = "http://w00g.kancolle-server.com/";
    public static final String DEFAULT_ALTER_GADGET_URL = "https://kcwiki.github.io/cache/";

    public static final int DEFAULT_SUBTITLE_FONT_SIZE = 18;

    public static final String PREF_CURSOR_MODE_TOUCH = "1";
    public static final String PREF_CURSOR_MODE_MOUSE = "2";

    public static final String APP_UI_HELP_VER = "20211002";
    public static final String KCANOTIFY_PACKAGE_NAME = "com.antest1.kcanotify";
}
