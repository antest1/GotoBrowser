# Doc screenshots

Renders the app's Compose UI (toolbar, settings sheet, dialogs) for the manual,
in every locale (ko / en / ja / zh-cn / zh-tw), without a device.

```
gradlew :app:testDebugUnitTest --tests "*DocScreenshotTest*" -Pdocshots
```

- Output: `tools/docshots/out/<locale>/<name>.png`
- Cases: `app/src/test/java/com/antest1/gotobrowser/docshots/DocScreenshotTest.kt`
- The game area is a WebView and cannot be rendered. Put a 5:3 capture of the
  game only (no toolbar, no subtitle) at `tools/docshots/input/game_bg.jpg`
  to use it as the background; otherwise the area is left dark.
  `ui_game_notice` uses `tools/docshots/input/error_screen.png` instead: a capture
  of the game's own connection error screen.
- Use JDK 17-21 (e.g. Android Studio's JBR 21). `-Pdocshots` gives the debug build
  the `.debug` id that `app/src/debug/google-services.json` expects.

## Manual images

`build.py` turns the renders and device captures into the annotated images the manual
uses (`docs/md/*/`), for every locale:

```
python build.py [name ...] [--locale ko,en]
```

- Needs Pillow and numpy.
- Inputs: renders in `out/<locale>/` and device captures (personal info blurred)
  in `input/captures/`.
- Output: `out/docs/<locale>/<name>.webp`. Upload to S3 (`gotobrowser-docs/<locale>/`)
  under the same name; the manual's links stay as they are.
- `SPECS` in `build.py` lists each image: source, crop, display width in the manual,
  and the items to highlight. `annotate.py` draws them.
- Fonts: Pretendard Bold/Medium (the manual's font) for ko/en, Noto Sans JP for ja,
  Microsoft YaHei / JhengHei for zh-cn / zh-tw. Pretendard has no kanji or hanzi. Fonts are
  looked up in `fonts/` (not committed), then in the system font folders.

### Annotation style

Highlight a target with a yellow ring and name it with a dark label beside it.

- **Ring**: a 2px `#FFD60A` line with a 10px glow (20%) around the target. A round button
  uses its own radius (`"radius": "round"`). A ring that groups several elements uses a
  square ring with radius 10. The app's own UI (tooltips, the attention ring around the
  refresh button) goes inside the ring with the element it belongs to.
- **Dim**: game screens are darkened 62% except inside the rings. Light settings screens
  are not (`"dim": False`).
- **Area**: a range with no border of its own (the subtitle) gets a dotted 60% yellow outline
  instead of a ring (`"area": True`).
- **Label**: a `#1C1C1E` 96% pill with a 1px 18% white border, a shadow, bold white text and a
  yellow bar on the side facing the target. Its tail points at the ring. Labels always sit
  outside the ring (30px from it). If they can't fit beside it, move them with `side`,
  `align` or `pos` until they cover no other target and no text that matters.
- **Numbers**: steps to follow in order use a numbered badge instead of the bar. Never mix
  bars and badges in one image.
- **Text**: use the same words as the manual body (if the body says "Toolbar", so does the
  label). A label is a name or one short action. Explanations go in the body. A muted
  `sub` text can add an action hint ("Tap to hide").
- **Size**: tokens are for a 1560px-wide image shown at 840px. `build.py` scales them by
  each image's display width so label text is about 15px in the manual at any size.
- **Don't**: use white or yellow-filled labels, bare text, thin leader lines, or any colour
  other than `#FFD60A` for annotations.
