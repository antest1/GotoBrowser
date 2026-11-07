"""Build the manual's images (docs/md/*/) into out/docs/<locale>/<name>.webp for upload to S3.

Sources:
  - Compose renders from DocScreenshotTest: out/<locale>/<name>.png
  - Device captures with personal info blurred: input/captures/
Run: python build.py [name ...] [--locale ko,en]
Annotation style: README.md, "Annotation style". Renderer: annotate.py.
"""
import os
import sys

import numpy as np
from PIL import Image

from annotate import T, annotate, label_size, load_fonts

HERE = os.path.dirname(os.path.abspath(__file__))
RENDERS = os.environ.get("DOCSHOTS_OUT", os.path.join(HERE, "out"))
SHOTS = os.environ.get("DOCSHOTS_CAPTURES", os.path.join(HERE, "input", "captures"))
OUT = os.path.join(HERE, "out", "docs")
LOCALES = ["ko", "en", "ja", "zh-cn", "zh-tw"]
SHOT_SUFFIX = {"ko": "kr", "en": "en", "ja": "jp", "zh-cn": "zhcn", "zh-tw": "zhtw"}
SUBTITLE_SHOT = {
    "ko": "Screenshot_20261106_033818_GotoBrowser.jpg",
    "en": "KakaoTalk_20261106_053745347.jpg",
    "ja": "Screenshot_20261106_040738_GotoBrowser.jpg",
    "zh-cn": "8_subtitle_zhcn.jpg",
    "zh-tw": "8_subtitle_zhtw.jpg",
}
# Labels are tuned for an image shown at 840px that is 1560px wide (scale 1).
DISPLAY_REF = 840 / 1560

# Chinese labels, keyed by the Korean label. Terms follow the app's zh-rCN / zh-rTW strings.
ZH = {
    "툴바": ("工具栏", "工具列"),
    "새로고침": ("重新整理页面", "重新整理頁面"),
    "설정": ("设置", "設定"),
    "스크린샷": ("截图", "截圖"),
    "누른 채 위로 끌어서 선택": ("按住并向上拖动", "按住並向上拖曳"),
    "아래로 끌어서 닫기": ("向下拖动关闭", "向下拖曳關閉"),
    "도움말": ("帮助", "說明"),
    "브로드캐스트 모드 켜기": ("开启「使用广播讯息方式」", "開啟「使用廣播訊息方式」"),
    "고토브라우저": ("GotoBrowser", "GotoBrowser"),
    "자막 영역": ("字幕区域", "字幕區域"),
    "터치하여 숨기기": ("点按隐藏", "點按隱藏"),
    "PIP 모드": ("画中画模式", "畫中畫模式"),
    "멀티윈도우": ("多窗口", "多視窗"),
    "로딩 안내": ("加载提示", "載入提示"),
    "연결 오류": ("连接错误", "連線錯誤"),
    "오류 안내": ("错误提示", "錯誤提示"),
    "저장 알림": ("截图通知", "截圖通知"),
    "Gadget 서버 우회 켜기": ("开启封锁绕行", "開啟封鎖繞行"),
    "URL 교체 · Endpoint 입력": ("URL替换 · 输入端点", "URL Replacement · 輸入端點"),
}


def render(name):
    return {loc: os.path.join(RENDERS, loc, name + ".png") for loc in LOCALES}


def shot(pattern):
    return {loc: os.path.join(SHOTS, pattern.format(SHOT_SUFFIX[loc])) for loc in LOCALES}


def same(path):
    return {loc: os.path.join(SHOTS, path) for loc in LOCALES}


def L(ko, en, ja):
    zh_cn, zh_tw = ZH.get(ko, (en, en)) if isinstance(ko, str) else (en, en)
    return {"ko": ko, "en": en, "ja": ja, "zh-cn": zh_cn, "zh-tw": zh_tw}


def dialog_crop(path, pad=40):
    """Bounding box of the light dialog surface over the dimmed background."""
    a = np.asarray(Image.open(path).convert("L"))
    mask = a > 215
    rows = np.where(mask.sum(1) > a.shape[1] * 0.15)[0]
    cols = np.where(mask[rows.min():rows.max() + 1].sum(0) > (rows.max() - rows.min()) * 0.5)[0]
    h, w = a.shape
    return [max(0, cols.min() - pad), max(0, rows.min() - pad), min(w, cols.max() + pad), min(h, rows.max() + pad)]


# Loading / error notices: the card height follows the text, which differs per locale,
# so its box is measured from each render instead of being fixed.
GAME_X = (330, 2800)  # game area columns in the landscape renders (toolbar and settings button outside)
FAB = [84, 1084, 280, 1280]  # refresh button in the landscape renders
FAB_RING = [56, 1056, 308, 1308]  # refresh button with the app's attention ring around it
SHEET = [440, 0, 2680, 1438]  # settings sheet area in the landscape renders


def color_box(path, rgb, tol=6):
    """Bounding box of the pixels close to rgb inside the game area."""
    if not os.path.exists(path):
        return [0, 0, 1, 1]
    a = np.asarray(Image.open(path).convert("RGB")).astype(int)
    x0, x1 = GAME_X
    mask = (np.abs(a[:, x0:x1] - np.array(rgb)) <= tol).all(2)
    rows = np.where(mask.sum(1) > 40)[0]
    cols = np.where(mask[rows.min():rows.max() + 1].sum(0) > (rows.max() - rows.min()) * 0.5)[0]
    return [x0 + cols.min(), rows.min(), x0 + cols.max() + 1, rows.max() + 1]


def fab_box(path, pad=20):
    """The refresh button (with its attention ring), extended over the "Full Refresh"
    tooltip when one is shown beside it: the app's own UI goes inside one ring."""
    if not os.path.exists(path):
        return FAB_RING
    a = np.asarray(Image.open(path).convert("L")).astype(int)
    y = (FAB[1] + FAB[3]) // 2
    row = a[y, FAB_RING[2]:GAME_X[0] + 900]
    lit = np.where(row > 40)[0]
    right = FAB_RING[2] + lit.max() + pad if lit.size else FAB_RING[2]
    return [FAB_RING[0], FAB_RING[1], max(FAB_RING[2], right), FAB_RING[3]]


CARD = (39, 42, 47)  # SettingsTheme dark surfaceContainerHigh (loading and in-game notice cards)


def per_locale(name, fn, *args):
    return {loc: fn(path, *args) for loc, path in render(name).items()}


def grow(box, pad):
    return [box[0] - pad, box[1] - pad, box[2] + pad, box[3] + pad]


# Items: box [x1, y1, x2, y2] in the (cropped) panel's pixels, or a per-locale dict.
#   radius: ring corner in panel pixels; "round" = half the short side (round buttons);
#           default T["ring_radius"] (a ring grouping several elements)
#   area: true = dotted range (no ring), for ranges without a border such as the subtitle
#   side/align: where the label goes (right|left|top|bottom, start|center|end);
#               pos (0..1) instead of align centers the label at that point along the side
#   label / sub / num: label text, muted action hint, numbered badge
SPECS = [
    # browserui: game screen with toolbar and top-right buttons
    {
        "name": "game_screen", "src": render("ui_toolbar"), "display": 840, "out_width": 1560,
        "items": [
            {"box": [84, 158, 280, 1026], "radius": "round", "side": "right", "align": "start", "label": L("툴바", "Toolbar", "ツールバー")},
            {"box": FAB, "radius": "round", "side": "right", "label": L("새로고침", "Refresh", "リフレッシュ")},
            {"box": [2840, 56, 3036, 252], "radius": "round", "side": "left", "label": L("설정", "Settings", "設定")},
            {"box": [2842, 310, 3034, 502], "radius": 44, "side": "left", "label": L("스크린샷", "Screenshot", "スクリーンショット")},
        ],
    },
    # browserui: portrait layout (toolbar at the bottom)
    {
        "name": "game_screen_portrait", "src": render("ui_toolbar_portrait"), "display": 300, "out_width": 640,
        "items": [
            {"box": [400, 2860, 1285, 3065], "radius": "round", "side": "top", "align": "end", "label": L("툴바", "Toolbar", "ツールバー")},
            {"box": [155, 2860, 360, 3065], "radius": "round", "side": "top", "align": "start", "label": L("새로고침", "Refresh", "リフレッシュ")},
        ],
    },
    # browserui: refresh button held and dragged up
    {
        "name": "refresh_menu", "src": render("ui_refresh_menu"), "crop": [0, 560, 2000, 1438], "display": 560, "out_width": 1120,
        "items": [
            {"box": L([80, 300, 625, 720], [80, 300, 625, 720], [80, 300, 830, 720]),  # ja labels are wider
             "side": "right", "label": L("누른 채 위로 끌어서 선택", "Hold and drag up", "押したまま上にドラッグ")},
        ],
    },
    # settings: sheet with handle and help button
    {
        "name": "settings_main", "src": render("settings_main"), "crop": SHEET, "dim": False, "display": 560, "out_width": 1120,
        "items": [
            {"box": [1010, 50, 1230, 118], "radius": "round", "side": "bottom", "align": "center",
             "label": L("아래로 끌어서 닫기", "Drag down to close", "下にドラッグして閉じる")},
            {"box": [2080, 192, 2200, 312], "radius": "round", "side": "left", "label": L("도움말", "Help", "ヘルプ")},
        ],
    },
    # settings: "refresh required" prompt
    {"name": "settings_refresh_required", "src": render("dialog_refresh_required"), "crop": "dialog",
     "dim": False, "display": 480, "out_width": 900, "items": []},
    # gadgetbypass: settings -> login and connection, bypass section
    {
        "name": "gadget_options", "src": render("settings_gadget"), "crop": [320, 480, 2680, 1438], "dim": False,
        "display": 600, "out_width": 1180,
        "items": [
            {"box": [130, 220, 2350, 440], "num": 1, "side": "top", "align": "end",
             "label": L("Gadget 서버 우회 켜기", "Turn on Gadget Block Bypass", "国外ブロックのバイパスを有効化")},
            # the two value rows' text only, so the label can sit in the empty right half
            {"box": [130, 500, 1100, 925], "num": 2, "side": "right",
             "label": L("URL 교체 · Endpoint 입력", "URL Replacement · endpoint", "URL置換・エンドポイント入力")},
        ],
    },
    # gadgetbypass: IP block dialog
    {"name": "bypass_required", "src": render("dialog_ipblock"), "crop": "dialog",
     "dim": False, "display": 480, "out_width": 900, "items": []},
    # kcanotify: GotoBrowser broadcast mode + Kcanotify settings
    {
        "name": "kcanotify_settings", "dim": False, "display": 840, "out_width": 1680, "gap": 12,
        "panels": [
            {"src": render("settings_broadcast"), "crop": [320, 0, 2680, 1438], "items": [
                # between the row above's title and its switch
                {"box": [130, 1190, 2350, 1428], "num": 1, "side": "top", "pos": 0.585,
                 "label": L("브로드캐스트 모드 켜기", "Turn on Broadcast Mode", "ブロードキャストモードを有効化")}]},
            {"src": shot("4_kcanotify_setting_{}.jpg"), "crop": [0, 140, 1440, 1400], "items": "kcanotify"},
        ],
    },
    # voiceline: subtitle over the game
    {
        "name": "subtitle", "src": {loc: os.path.join(SHOTS, f) for loc, f in SUBTITLE_SHOT.items()}, "display": 840, "out_width": 1560,
        "items": [{"box": [440, 1190, 2820, 1432], "area": True, "radius": 24, "side": "top", "align": "end",
                   "label": L("자막 영역", "Subtitle area", "字幕エリア"), "sub": L("터치하여 숨기기", "Tap to hide", "タップで非表示")}],
    },
    # pipmode: PiP window and split screen
    {
        "name": "pip_multiwindow", "display": 550, "out_width": 1100, "gap": 24,
        "panels": [
            {"src": same("3_PIP.jpg"), "items": [
                {"box": [668, 2178, 1392, 2622], "radius": 40, "side": "top", "align": "end", "label": L("PIP 모드", "PIP Mode", "PIPモード")}]},
            {"src": same("3_Multiwindow.jpg"), "items": [
                {"box": [8, 160, 1432, 1600], "radius": 40, "side": "bottom", "align": "center",
                 "label": L("멀티윈도우", "Multi-Window", "マルチウィンドウ")}]},
        ],
    },
    # screenshot: saved notification
    {
        "name": "screenshot_noti", "src": shot("2_screenshot_noti_{}.jpg"), "crop": [560, 100, 2540, 1000], "display": 720, "out_width": 1200,
        "items": [{"box": [52, 612, 1918, 868], "radius": 80, "side": "top", "align": "end", "label": L("저장 알림", "Saved notification", "保存通知")}],
    },
    # screenshot: notification permission (Android 13+)
    {"name": "noti_permission", "src": shot("6_noti_alert_{}.jpg"), "crop": [880, 580, 2380, 1440],
     "dim": False, "display": 450, "out_width": 900, "items": []},
    # entrance: loading card while the first screen loads
    {
        "name": "loading_status", "src": render("ui_loading_status"), "display": 840, "out_width": 1560,
        "items": [
            {"box": per_locale("ui_loading_status", color_box, CARD), "radius": 24, "side": "right",
             "label": L("로딩 안내", "Loading notice", "読み込みの案内")},
        ],
    },
    # entrance: load error in the card, refresh button pointing at a full refresh
    {
        "name": "loading_error", "src": render("ui_loading_error"), "display": 840, "out_width": 1560,
        "items": [
            {"box": per_locale("ui_loading_error", color_box, CARD), "radius": 24, "side": "right",
             "label": L("연결 오류", "Connection error", "接続エラー")},
            {"box": per_locale("ui_loading_error", fab_box), "radius": "round", "side": "right",
             "label": L("새로고침", "Refresh", "リフレッシュ")},
        ],
    },
    # browserui: error notice card below the game's error message during play
    {
        "name": "game_notice", "src": render("ui_game_notice"), "display": 840, "out_width": 1560,
        "items": [
            {"box": per_locale("ui_game_notice", color_box, CARD), "radius": 24, "side": "right",
             "label": L("오류 안내", "Error notice", "エラー案内")},
            {"box": FAB_RING, "radius": "round", "side": "right",
             "label": L("새로고침", "Refresh", "リフレッシュ")},
        ],
    },
    # entrance: right after launch (DMM direct start page)
    {
        "name": "first_launch", "src": shot("5_dmm_direct_{}.jpg"), "display": 840, "out_width": 1560,
        "items": [
            {"box": [220, 155, 424, 1032], "radius": "round", "side": "right", "align": "start", "label": L("툴바", "Toolbar", "ツールバー")},
            {"box": [2836, 52, 3040, 256], "radius": "round", "side": "left", "label": L("설정", "Settings", "設定")},
        ],
    },
]

# Kcanotify rows (crop space), per locale: app selection and sniffer mode
KCANOTIFY_ROWS = {
    "ko": ([24, 712, 1416, 890], [24, 970, 1416, 1148]),
    "en": ([24, 715, 1416, 880], [24, 965, 1416, 1135]),
    "ja": ([24, 730, 1416, 900], [24, 985, 1416, 1155]),
    "zh-cn": ([24, 720, 1416, 910], [24, 970, 1416, 1160]),
    "zh-tw": ([24, 720, 1416, 910], [24, 970, 1416, 1160]),
}


def pick(v, loc):
    return v[loc] if isinstance(v, dict) else v


def load_panels(spec, loc):
    """Crop each panel, match heights, join side by side. Returns (image, items in image pixels)."""
    panels = spec.get("panels", [spec])
    imgs = []
    for p in panels:
        src = pick(p["src"], loc)
        img = Image.open(src).convert("RGB")
        crop = dialog_crop(src) if p.get("crop") == "dialog" else p.get("crop")
        imgs.append(img.crop(crop) if crop else img)
    h = min(i.size[1] for i in imgs)
    gap = spec.get("gap", 0)
    scaled = [i if i.size[1] == h else i.resize((round(i.size[0] * h / i.size[1]), h), Image.LANCZOS) for i in imgs]
    out = Image.new("RGB", (sum(i.size[0] for i in scaled) + gap * (len(scaled) - 1), h), (20, 22, 28))
    items, x = [], 0
    for p, img, sc in zip(panels, imgs, scaled):
        out.paste(sc, (x, 0))
        k = h / img.size[1]
        raw = p.get("items", [])
        if raw == "kcanotify":
            app, sniffer = KCANOTIFY_ROWS[loc]
            raw = [
                {"box": app, "num": 2, "side": "top", "align": "end", "label": L("고토브라우저", "GotoBrowser", "GotoBrowser")},
                {"box": sniffer, "num": 3, "side": "bottom", "align": "end", "label": "PASSIVE"},
            ]
        for it in raw:
            b = pick(it["box"], loc)
            items.append(dict(it, box=[x + b[0] * k, b[1] * k, x + b[2] * k, b[3] * k], _k=k))
        x += sc.size[0] + gap
    return out, items


def layout(items, loc, s, size):
    """Rings / areas / labels for the renderer, with each label placed outside its ring."""
    W, H = size
    fonts = load_fonts(loc, s)
    off = T["label_offset"] * s
    margin = 12 * s
    rings, areas, labels = [], [], []
    for it in items:
        x1, y1, x2, y2 = it["box"]
        w, h = x2 - x1, y2 - y1
        r = it.get("radius", T["ring_radius"] * s)
        r = min(w, h) / 2 if r == "round" else r * it["_k"] if "radius" in it else r
        target = {"box": [x1, y1, w, h], "radius": r}
        (areas if it.get("area") else rings).append(target)

        text = pick(it.get("label"), loc)
        if not text:
            continue
        lb = {"text": text}
        if it.get("sub"):
            lb["sub"] = pick(it["sub"], loc)
        if it.get("num") is not None:
            lb["badge"] = it["num"]
        lw, lh = label_size(lb, fonts, s)
        side, align = it.get("side", "right"), it.get("align", "center")
        inset = 6 * s  # start/end alignment sits a little inside the target's edge
        if side in ("right", "left"):
            ly = {"start": y1 + inset, "end": y2 - inset - lh}.get(align, (y1 + y2 - lh) / 2)
            lx = x2 + off if side == "right" else x1 - off - lw
            lb["tail"] = "left" if side == "right" else "right"
        else:
            lx = {"start": x1 + inset, "end": x2 - inset - lw}.get(align, (x1 + x2 - lw) / 2)
            if "pos" in it:
                lx = x1 + w * it["pos"] - lw / 2
            ly = y1 - off - lh if side == "top" else y2 + off
            lb["tail"] = "down" if side == "top" else "up"
            # the tail points into the target near the label's bar end
            tail_x = lx + 30 * s + T["tail"] * s / 2
            lb["tail_x"] = min(max(tail_x, x1 + 2 * off), x2 - 2 * off) if w > 4 * off else (x1 + x2) / 2
        lx = min(max(lx, margin), W - margin - lw)
        ly = min(max(ly, margin), H - margin - lh)
        lb["at"] = [round(lx), round(ly)]
        labels.append(lb)
    return rings, areas, labels


def build(spec, loc):
    img, items = load_panels(spec, loc)
    s = img.size[0] / spec["display"] * DISPLAY_REF
    rings, areas, labels = layout(items, loc, s, img.size)
    out = annotate(img, loc, s, spec.get("dim", True), rings, areas, labels).convert("RGB")
    ow = spec["out_width"]
    if out.size[0] > ow:
        out = out.resize((ow, round(out.size[1] * ow / out.size[0])), Image.LANCZOS)
    os.makedirs(os.path.join(OUT, loc), exist_ok=True)
    path = os.path.join(OUT, loc, spec["name"] + ".webp")
    out.save(path, "WEBP", quality=85, method=6)
    return f"{path} ({os.path.getsize(path) / 1024:.0f} KB)"


def sources(spec, loc):
    return [pick(p["src"], loc) for p in spec.get("panels", [spec])]


def main():
    args = sys.argv[1:]
    locales = LOCALES
    if "--locale" in args:
        i = args.index("--locale")
        locales = args[i + 1].split(",")
        args = args[:i] + args[i + 2:]
    only = set(args)
    for spec in SPECS:
        if only and spec["name"] not in only:
            continue
        for loc in locales:
            missing = [f for f in sources(spec, loc) if not os.path.exists(f)]
            if missing:
                print(f"skip {loc}/{spec['name']}: missing {', '.join(os.path.basename(f) for f in missing)}")
                continue
            print(build(spec, loc))


if __name__ == "__main__":
    main()
