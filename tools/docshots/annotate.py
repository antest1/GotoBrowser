"""Manual screenshot annotations: yellow highlight rings and dark labels.

Implements the manual annotation style (see README.md, "Annotation style"). All sizes
are tokens at a 1560px-wide image shown at 840px in the manual; `s` scales them.

    img = annotate(img, locale, s, dim=True,
                   rings=[{"box": [x, y, w, h], "radius": 50}],
                   areas=[{"box": [x, y, w, h], "radius": 12}],
                   labels=[{"text": "Toolbar", "at": [x, y], "tail": "left"}])

label fields:
  text    label text (same words as the manual body)
  at      [x, y] reference point of the label box
  anchor  "left" (default, at = top-left) | "right" (at = top-right) | "center" (at = top-center)
  tail    "left" | "right" | "up" | "down" | none. The bar sits on the tail side (left/right)
  tail_x  for up/down tails: x of the tail tip (default: near the bar end of the label)
  badge   number. Replaces the bar with a numbered badge (never mix bars and badges in one image)
  sub     muted secondary text (a short action hint) after the label
"""
import os
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

# Tokens (1560px-wide original)
ACCENT = (255, 214, 10)
ACCENT_GLOW_A = 51  # 20%
LABEL_BG = (28, 28, 30, 245)  # 96%
LABEL_BORDER = (255, 255, 255, 46)  # 18%
LABEL_INK = (255, 255, 255, 255)
LABEL_INK_MUTED = (255, 255, 255, 179)  # 70%
BADGE_INK = (20, 20, 20, 255)
DIM_A = 158  # 62%

T = dict(
    label_size=28, line_h=34, sub_size=22, badge_font=18,
    pad_y=12, pad_x=24, pad_start=18, pad_badge=12, gap=12,
    bar_w=4, bar_h=26, badge=32, tail=16, radius=14,
    ring_stroke=2, ring_glow=10, ring_radius=10, label_offset=30,
    shadow_dy=6, shadow_blur=10, shadow_a=128,
    area_stroke=2, area_dash=14,
)

HERE = Path(__file__).resolve().parent
FONT_DIRS = [
    HERE / "fonts",
    HERE.parent.parent / "misc" / "manual-annotation" / "fonts",
    Path(os.environ.get("LOCALAPPDATA", "")) / "Microsoft" / "Windows" / "Fonts",
    Path(os.environ.get("WINDIR", "C:/Windows")) / "Fonts",
    Path.home() / "Library/Fonts", Path("/Library/Fonts"),
    Path.home() / ".fonts", Path.home() / ".local/share/fonts", Path("/usr/share/fonts"),
]
# (bold, medium) candidates per locale. Pretendard (the manual's font) has no kanji/hanzi,
# so ja and zh use a CJK font of the same weight.
FONTS = {
    "ko": (["Pretendard-Bold.otf"], ["Pretendard-Medium.otf"]),
    "en": (["Pretendard-Bold.otf"], ["Pretendard-Medium.otf"]),
    "ja": (["PretendardJP-Bold.otf", "NotoSansJP-Bold.ttf", "NotoSansCJKjp-Bold.otf"],
           ["PretendardJP-Medium.otf", "NotoSansJP-Medium.ttf", "NotoSansCJKjp-Medium.otf"]),
    "zh-cn": (["msyhbd.ttc", "NotoSansSC-Bold.ttf", "NotoSansCJKsc-Bold.otf"],
              ["msyh.ttc", "NotoSansSC-Medium.ttf", "NotoSansCJKsc-Medium.otf"]),
    "zh-tw": (["msjhbd.ttc", "NotoSansTC-Bold.ttf", "NotoSansCJKtc-Bold.otf"],
              ["msjh.ttc", "NotoSansTC-Medium.ttf", "NotoSansCJKtc-Medium.otf"]),
}


def find_font(names):
    for n in names:
        for d in FONT_DIRS:
            p = d / n
            if p.exists():
                return str(p)
    return None


def load_fonts(locale, s):
    bold_names, med_names = FONTS.get(locale, FONTS["en"])
    bold = find_font(bold_names)
    if not bold:
        raise SystemExit(f"No font for {locale}: put one of {bold_names} in {FONT_DIRS[0]}")
    med = find_font(med_names) or bold

    def tt(path, size):
        return ImageFont.truetype(path, max(1, round(size * s)))

    return dict(label=tt(bold, T["label_size"]), sub=tt(med, T["sub_size"]), badge=tt(bold, T["badge_font"]))


def px(v, s):
    return max(1, round(v * s))


def rr(draw, box, r, **kw):
    x, y, w, h = box
    draw.rounded_rectangle([x, y, x + w, y + h], radius=r, **kw)


def label_size(lb, fonts, s):
    """(w, h) of a label box."""
    tw = fonts["label"].getlength(lb["text"])
    sw = fonts["sub"].getlength(lb["sub"]) if lb.get("sub") else 0
    badge = lb.get("badge") is not None
    lead_w = px(T["badge"] if badge else T["bar_w"], s)
    gap = px(T["gap"], s)
    pad_lead = px(T["pad_badge"] if badge else T["pad_start"], s)
    w = pad_lead + lead_w + gap + tw + ((gap + sw) if sw else 0) + px(T["pad_x"], s)
    h = px(T["pad_y"], s) * 2 + round(T["line_h"] * s)
    return round(w), round(h)


def draw_dim_and_rings(base, rings, areas, dim, s):
    W, H = base.size
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    if dim:
        d.rectangle([0, 0, W, H], fill=(0, 0, 0, DIM_A))
        for rg in rings + areas:  # highlighted targets stay bright
            rr(d, rg["box"], rg.get("radius", 0), fill=(0, 0, 0, 0))
    stroke, glow = px(T["ring_stroke"], s), px(T["ring_glow"], s)
    for rg in rings:
        x, y, w, h = rg["box"]
        r = rg.get("radius", 0)
        o = stroke + glow
        rr(d, (x - o, y - o, w + 2 * o, h + 2 * o), r + o, outline=ACCENT + (ACCENT_GLOW_A,), width=glow)
        rr(d, (x - stroke, y - stroke, w + 2 * stroke, h + 2 * stroke), r + stroke, outline=ACCENT + (255,), width=stroke)
    return Image.alpha_composite(base, layer)


def draw_areas(base, areas, s):
    layer = Image.new("RGBA", base.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    w_ = px(T["area_stroke"], s)
    dash, gap = px(T["area_dash"], s), px(T["area_dash"] * 0.7, s)
    col = ACCENT + (153,)  # 60%
    for a in areas:
        x, y, w, h = a["box"]
        r = a.get("radius", 0)
        for (x1, y1, x2, y2) in [(x + r, y, x + w - r, y), (x + r, y + h, x + w - r, y + h),
                                 (x, y + r, x, y + h - r), (x + w, y + r, x + w, y + h - r)]:
            length, t = max(abs(x2 - x1), abs(y2 - y1)), 0
            while t < length:
                seg = min(dash, length - t)
                if x1 == x2:
                    d.line([x1, y1 + t, x1, y1 + t + seg], fill=col, width=w_)
                else:
                    d.line([x1 + t, y1, x1 + t + seg, y1], fill=col, width=w_)
                t += dash + gap
        if r:
            for cx, cy, a0 in [(x + r, y + r, 180), (x + w - r, y + r, 270), (x + w - r, y + h - r, 0), (x + r, y + h - r, 90)]:
                d.arc([cx - r, cy - r, cx + r, cy + r], a0, a0 + 90, fill=col, width=w_)
    return Image.alpha_composite(base, layer)


def draw_labels(base, labels, fonts, s):
    W, H = base.size
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    r = px(T["radius"], s)
    tail_sz = px(T["tail"], s)
    half = tail_sz / 2 * 1.4142  # half diagonal of the square turned 45 degrees

    for lb in labels:
        w, h = label_size(lb, fonts, s)
        ax, ay = lb["at"]
        anchor = lb.get("anchor", "left")
        x = ax - w if anchor == "right" else ax - w / 2 if anchor == "center" else ax
        y = ay
        tail = lb.get("tail")
        lead_right = tail == "right"  # labels left of their target keep the bar on the right
        badge = lb.get("badge") is not None
        lead_w = px(T["badge"] if badge else T["bar_w"], s)
        pad_lead = px(T["pad_badge"] if badge else T["pad_start"], s)

        rr(sd, (x, y + px(T["shadow_dy"], s), w, h), r, fill=(0, 0, 0, T["shadow_a"]))
        rr(d, (x, y, w, h), r, fill=LABEL_BG, outline=LABEL_BORDER, width=1)
        if tail:
            cy = y + h / 2
            k = 3  # overlap the box a little so its border doesn't show at the tail base
            if tail in ("up", "down"):
                lo, hi = x + r + half, x + w - r - half
                tx = lb.get("tail_x", x + px(30, s) + tail_sz / 2)
                tx = min(max(tx, lo), hi)
            if tail == "left":
                pts = [(x - half + 1, cy), (x + k, cy - half - k), (x + k, cy + half + k)]
            elif tail == "right":
                pts = [(x + w + half - 1, cy), (x + w - k, cy - half - k), (x + w - k, cy + half + k)]
            elif tail == "up":
                pts = [(tx, y - half + 1), (tx - half - k, y + k), (tx + half + k, y + k)]
            else:  # down
                pts = [(tx, y + h + half - 1), (tx - half - k, y + h - k), (tx + half + k, y + h - k)]
            d.polygon(pts, fill=LABEL_BG)
            if tail in ("left", "right"):
                e = x + 1 if tail == "left" else x + w - 1
                d.line([(e, cy - half), pts[0], (e, cy + half)], fill=LABEL_BORDER, width=1)
            else:
                e = y + 1 if tail == "up" else y + h - 1
                d.line([(tx - half, e), pts[0], (tx + half, e)], fill=LABEL_BORDER, width=1)

        cy = y + h / 2
        lead_x = x + w - pad_lead - lead_w if lead_right else x + pad_lead
        if badge:
            d.ellipse([lead_x, cy - lead_w / 2, lead_x + lead_w, cy + lead_w / 2], fill=ACCENT + (255,))
            d.text((lead_x + lead_w / 2, cy), str(lb["badge"]), font=fonts["badge"], fill=BADGE_INK, anchor="mm")
        else:
            bh = px(T["bar_h"], s)
            d.rounded_rectangle([lead_x, cy - bh / 2, lead_x + lead_w, cy + bh / 2], radius=px(2, s), fill=ACCENT + (255,))
        gap = px(T["gap"], s)
        text_x = x + px(T["pad_x"], s) if lead_right else lead_x + lead_w + gap
        d.text((text_x, cy), lb["text"], font=fonts["label"], fill=LABEL_INK, anchor="lm")
        if lb.get("sub"):
            sx = text_x + fonts["label"].getlength(lb["text"]) + gap
            d.text((sx, cy), lb["sub"], font=fonts["sub"], fill=LABEL_INK_MUTED, anchor="lm")

    shadow = shadow.filter(ImageFilter.GaussianBlur(px(T["shadow_blur"], s)))
    return Image.alpha_composite(Image.alpha_composite(base, shadow), layer)


def annotate(img, locale, s, dim=True, rings=(), areas=(), labels=()):
    img = img.convert("RGBA")
    fonts = load_fonts(locale, s)
    img = draw_dim_and_rings(img, list(rings), list(areas), dim, s)
    img = draw_areas(img, list(areas), s)
    return draw_labels(img, list(labels), fonts, s)
