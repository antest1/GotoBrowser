<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20,400,0,0&icon_names=help,settings" />

## Settings 

You can enter the settings by clicking the gear icon (<span class="material-symbols-outlined inline-icon">settings</span>) in the top right of the game screen while the toolbar is shown.

- The settings screen is shown on top of the game screen, and the game keeps running behind it.
- Drag the handle at the top downward or press the back button to close it.
- The help button (<span class="material-symbols-outlined inline-icon">help</span>) in the top right of the settings screen opens this manual.

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/en/settings_main.webp"  width="560" style="max-width: 100%;" class="mb-3"/>

##### How Settings Are Applied

| Applies | Description |
| --- | --- |
| Immediately | Applied as soon as the value is changed. |
| After reload | The "Reload Required" dialog is shown. "Reload Now" applies the change after a full refresh, and "Change Settings Without Reload" applies it on the next refresh. |

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/en/settings_refresh_required.webp"  width="480" style="max-width: 100%;" class="mb-3"/>

##### Login and Connection

| Group | Name | Description | Applies |
| --- | --- | --- | --- |
| Game Login | Login Autocomplete | ID and password to be filled into the login form automatically. Stored only within the device | After reload |
| Game Login | Logout | Logs out of the game and returns to the login page of the connector | |
| Browser Settings | Select Login Method | DMM direct (default) / ooi.moe<br/>A notice about game resources is shown when an unofficial connector is selected for the first time | After reload |
| Browser Settings | Start with Audio OFF | Sets the in-game BGM/SE/VOICE volume to 0 when the game starts (same as the official application function)<br/>Available only with DMM direct | After reload |
| Browser Settings | Broadcast Mode | Sends the KanColle-related communication to other applications such as Kcanotify (on by default)<br/>Must be turned on to use with Kcanotify | Immediately |
| Gadget Block Bypass | Gadget Block Bypass | Turn on if you are experiencing the foreign IP block issue (infinite black screen) | After reload |
| Gadget Block Bypass | Bypass Method | Select URL Replacement or KCCacheProxy (remote) (use "URL Replacement" in most cases) | After reload |
| Gadget Block Bypass | Bypass Endpoint Server | Server URL to use for bypass<br/>`https://kcwiki.github.io/cache/` (default) | After reload |
| Gadget Block Bypass | Auto-Disable Bypass | Turns the bypass off automatically once the game connects without being blocked (on by default) | Immediately |

For related information, refer to the <span class="link" data-move="gadgetbypass">IP Block Bypass (Non-Japan)</span> page. Bypass Method and Bypass Endpoint Server cannot be selected while Gadget Block Bypass is turned off.

##### Game Display

| Name | Description | Applies |
| --- | --- | --- |
| Lock to Landscape | Always play in landscape mode regardless of device orientation (on by default) | Immediately |
| Auto Layout Adjustment | Automatically scales the game screen to fit the device screen (recommended, on by default) | After reload |
| Game Vertical Position | Places the game at the center or the top when there is space above and below it, such as in portrait or split screen (center by default) | Immediately |
| Picture-in-picture mode | Shows the game in a small floating window when you leave for the home screen or pinch-zoom out on the game screen | Immediately |
| [Multi-Window] Add Margin from Divider | Adds a margin to prevent the screen from sticking to the divider when using the multi-window function | Immediately |
| Legacy Renderer | Force PixiJS game engine to use Canvas renderer instead of WebGL renderer (recommended to disable if no graphic related issues occur)<br/>Kantai3D cannot be used while this is on | After reload |

For PIP/multi-window information, refer to the <span class="link" data-move="pipmode">PIP/Multi-window</span> page.

##### Controls & Toolbar

| Group | Name | Description | Applies |
| --- | --- | --- | --- |
| Toolbar | Show Control Panel on Start | Starts with the toolbar shown when the app starts (on by default) | Next launch |
| Toolbar | Refresh Without Confirmation | The game screen is refreshed immediately without a confirmation dialog | Immediately |
| Toolbar | Toolbar Placement | Center (default) or Top · Classic. Top · Classic docks the toolbar at the top of the screen with the refresh button first, as in 3.x. Applies in landscape | Immediately |
| Toolbar | Swipe from Edge to Show Toolbar | Swipe in from the edge where the toolbar is hidden to show it (on by default)<br/>When off, edge swipes are left to the system gestures<br/>Without a margin around the game, the back button shows the toolbar | Immediately |
| Game Controls | Enable Keyboard Input | Whether keyboard input is available on the game screen (on by default) | Immediately |
| Game Controls | Optimize Pointer Device | Choose the input method when playing the game (Touch Screen or Mouse or Trackpad) | After reload |

##### Asset Caching

| Name | Description | Applies |
| --- | --- | --- |
| External Storage for Resource Caching | Use external storage instead of internal storage when caching game resources (recommended)<br/>(allows access to image/voice resources via PC) | After reload |
| Prefetch Font Assets | Use built-in font files instead of loading them from the game server (recommended, on by default) | After reload |
| Retry Game Asset Download | Prompts a dialog asking whether to retry when the client fails to download a sound or image asset<br/>Can sometimes avoid catbombs caused by poor network. Does not apply to game data API errors | Immediately |
| Clear Cache | Deletes the browser cache. Use this when images or audio are not loading correctly, or to switch between login methods<br/>You may need to log in again afterwards<br/>A reload prompt appears once clearing is done | Immediately |

##### Subtitle

| Name | Description | Applies |
| --- | --- | --- |
| Current Language | Select from English / 한국어 / 日本語 (KC3改), 简中 / 繁中 (kcwiki) | Immediately |
| Font Size | Adjust the font size of voice subtitles on screen (12 to 24, default: 18) | Immediately |
| Download Subtitle Data | You can download the subtitle data when an update is available. Applied right after the download | Immediately |

For information about the function, refer to the <span class="link" data-move="voiceline">Game Voiceline Subtitle</span> page.

##### Third Party Mods

<span class="text-danger">Use third party mods at your own risk.</span>

| Group | Name | Description | Applies | Author |
| --- | --- | --- | --- | --- |
| General Mods | FPS Rendering Mod | Use VSYNC rendering for smoother performance and better efficiency than the original game (supports high refresh rate devices) | After reload | [@laplamgor](https://x.com/laplamgor) |
| General Mods | FPS Limit | Select Default (Max Rate) or one of the refresh rates supported by the device. Lowering the target FPS saves battery life and reduces heating | Immediately | |
| General Mods | True Critical Hits | Removes some hard-coded obfuscation for hits. Only a critical hit (CL2) will be displayed as such | After reload | [@Oradimi](https://x.com/oradimi) |
| Kantai3D Mod | Kantai3D v4.0 | Add 3D-like visual effects and jiggle physics to some of your secretaries. Cannot be selected while Legacy Renderer is on | After reload | [@laplamgor](https://x.com/laplamgor) |
| Kantai3D Mod | About Kantai3D | Opens the [Kantai3D GitHub page](https://github.com/laplamgor/kantai3d) | | |
| Language Patch (KCCP) | KanColle Language Patch (KCCP) | Apply a translation patch to in-game text | After reload | [@Oradimi](https://x.com/oradimi) |
| Language Patch (KCCP) | Language Patch to Apply | [KanColle English Patch](https://github.com/Oradimi/KanColle-English-Patch-KCCP) (@Oradimi) or [KanColle Indonesia Patch](https://github.com/SLAVUSworks/KanColle-Indonesia-Patch-KCCP) (@SLAVUSworks) | After reload | |
| Language Patch (KCCP) | Download Patch Data | Downloads or updates the data of the selected patch. Refresh the game after it finishes to apply | After reload | |
| Language Patch (KCCP) | Delete Patch Data | Use this if the patch is presenting unexpected behaviour, or if something went wrong during download | After reload | |
| Language Patch (KCCP) | About KanColle English/Indonesia Patch | Opens the GitHub page of the selected patch | | |

##### App Information

| Name | Description | Applies |
| --- | --- | --- |
| Current Version | Installed app version | |
| Check Update | Shows the download page if a new version is available | |
| Source Code | Opens the [GitHub repository](https://github.com/antest1/GotoBrowser/) | |
| Enable DevTools Debugging | Enable [remote debugging](https://developer.chrome.com/docs/devtools/remote-debugging?hl=en) in browsers such as Chrome (chrome://inspect) | Immediately |
