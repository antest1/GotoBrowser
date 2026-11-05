# Doc screenshots

Renders the app's Compose UI (toolbar, settings sheet, dialogs) for the manual,
in every locale (ko / en / ja), without a device.

```
gradlew :app:testDebugUnitTest --tests "*DocScreenshotTest*" -Pdocshots
```

- Output: `tools/docshots/out/<locale>/<name>.png`
- Cases: `app/src/test/java/com/antest1/gotobrowser/docshots/DocScreenshotTest.kt`
- The game area is a WebView and cannot be rendered. Put a 5:3 capture of the
  game only (no toolbar, no subtitle) at `tools/docshots/input/game_bg.jpg`
  to use it as the background; otherwise the area is left dark.
- Use JDK 17-21 (e.g. Android Studio's JBR 21). `-Pdocshots` gives the debug build
  the `.debug` id that `app/src/debug/google-services.json` expects.
