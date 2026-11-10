## Browser UI
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@40,400,0,0&icon_names=closed_caption,crop_free,frame_reload,light_mode,photo_camera,refresh,screen_lock_rotation,settings,volume_off" />


<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/en/game_screen.webp"  width="840" style="max-width: 100%;" class="mb-3"/>

You can use various functions such as refreshing the game or taking a screenshot through the toolbar at the edge of the game screen.
- In portrait mode it appears horizontally at the bottom of the screen, and in landscape mode vertically on the left.
- Long-press a button to see its name.

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/en/game_screen_portrait.webp"  width="300" style="max-width: 100%;" class="mb-3"/>

##### Show/Hide Toolbar
- Here are the ways to make the toolbar appear:
  - Touch the black margin outside the game screen (recommended)
  - Swipe inward from the edge where the toolbar is hidden (portrait: bottom, landscape: left) (can be turned off in Settings → Controls & Toolbar → "Swipe from Edge to Show Toolbar")
  - If there is no margin outside the game screen, press the back button (or use the back gesture)
  - Turn on "Show Control Panel on Start" in Settings → Controls & Toolbar to show it on startup
- Conversely, here are the ways to make the toolbar disappear:
  - Touch the black margin outside the game screen again
  - Swipe the toolbar toward the edge of the screen

##### Refresh
The refresh button next to the toolbar offers two methods.
In landscape, setting Settings → Controls & Toolbar → "Toolbar Placement" to "Top · Classic" docks the toolbar at the top of the screen with the refresh button first, as in 3.x.

|  | Button | Description |
| --- | --- | --- |
| <span class="material-symbols-outlined">frame_reload</span> | Quick Refresh | Reloads only the game frame instead of the full web page.<br/>It is fast but may not always succeed. If a network error occurs, use Full Refresh. |
| <span class="material-symbols-outlined">refresh</span> | Full Refresh | Reloads the full web page.<br/>This is the most reliable way to refresh, but it takes longer than Quick Refresh. |

- Press and hold the button and drag toward the menu to choose the other method. The menu opens upward with the toolbar at the center, and downward with the toolbar at the top. The last method used is remembered as the default action of the button.
- Turn on Settings → Controls & Toolbar → "Refresh Without Confirmation" to refresh without the confirmation dialog.
- If you chose "Change Settings Without Reload" in the "Reload Required" dialog after changing a setting, refreshing with this button also applies the change.
- After a connection error while loading or a server error during play, the button runs a Full Refresh regardless of the saved method, and "Full Refresh" is shown next to it. The saved method does not change.

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/en/refresh_menu.webp"  width="560" style="max-width: 100%;" class="mb-3"/>

##### Toolbar Button Description
|  | Button | Description |
| --- | --- | --- |
| <span class="material-symbols-outlined">volume_off</span>  | Mute Audio | Mutes the whole sound playing in the browser. The audio files will continue to load.<br/>(Use this when you want to see subtitles without sound, etc.) |
| <span class="material-symbols-outlined">photo_camera</span> | Screenshot Mode | Turns screenshot mode on or off. When on, the screenshot button appears below the settings button in the top right of the screen. |
| <span class="material-symbols-outlined">screen_lock_rotation</span> | Auto-Rotate Lock | Prevents the screen from rotating regardless of device settings. |
| <span class="material-symbols-outlined">light_mode</span> | Screen Always-On | Keeps the screen from turning off in idle state. |
| <span class="material-symbols-outlined">closed_caption</span> | Subtitle | Shows or hides in-game subtitles at the bottom of the screen. Touching the subtitle hides it until the next line. |

##### Top Right Buttons
|  | Button | Description |
| --- | --- | --- |
| <span class="material-symbols-outlined">settings</span> | Settings | Opens the <span class="link" data-move="settings">settings screen</span>. The game keeps running behind it. |
| <span class="material-symbols-outlined">crop_free</span> | Screenshot | Saves the current game screen. Shown only while screenshot mode is on. (See <span class="link" data-move="screenshot">Saving Screenshots</span>) |

##### Error Notices During Play
If communication with the server fails during play, a notice is shown in the middle of the game screen, where it does not cover the game's own error message.

| Notice | Description |
| --- | --- |
| Connection error | The connection dropped or the server did not respond. Check your connection, then refresh. |
| Server response error | The server answered with an HTTP error. Refresh after a moment. |
| Server error | The server rejected the request (api_result). Quick Refresh cannot recover from it, so the refresh button runs a Full Refresh automatically. |

- Tap the notice to close it.
- Long-press the notice to copy its code, API and message. Include them when you report a problem.

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/en/game_notice.webp"  width="840" style="max-width: 100%;" class="mb-3"/>

##### Logout
- You can log out from Settings → Login and Connection → Logout.
- After logging out, the login page of the connector opens again.
