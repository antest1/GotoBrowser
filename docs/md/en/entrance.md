<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20,400,0,0&icon_names=help,settings" />

## Starting the Game

When you launch the app, the start page of the connector (default: DMM direct) opens right away. There is no separate main screen.  
Everything that used to be on the main screen before version 4.0 has moved to the <span class="link" data-move="settings">settings screen</span>.

##### Startup Flow
1. The start page of the connector opens. On the login page, the account information saved in the settings is filled in automatically.
2. After logging in, the game screen is fitted to your device screen. (Settings → Game Display → Auto Layout Adjustment)
3. While the game loads, a loading notice is shown on the game screen. It disappears when the game starts. (See Loading Notice below)
4. Touch the black margin outside the game screen to show the <span class="link" data-move="browserui">toolbar</span>. The settings button (<span class="material-symbols-outlined inline-icon">settings</span>) in the top right opens the settings screen.

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/en/first_launch.webp"  width="840" style="max-width: 100%;" class="mb-3"/>

##### Loading Notice
While the game loads, its progress is shown in the middle of the game screen. It does not appear if loading finishes quickly.
- Loading goes through Preparing → Connecting to server → Downloading resources, with the current step (1/3–3/3) in the top right.
- While resources download, the notice shrinks to the bottom of the screen so that it does not cover the game's own loading screen.
- It is never shown over ordinary web pages such as the login page.

| State | Description | What to do |
| --- | --- | --- |
| In progress | Shows the current step and progress. | Wait. |
| No response (yellow) | Shown when nothing has responded for 20 seconds or more. | Try again with the refresh button, which is highlighted in yellow. |
| Connection error (red) | Shown with the cause when the page could not be loaded. The error code and host are listed below it. | Check your internet connection, then press the refresh button. It runs a Full Refresh automatically. |

- If the toolbar was hidden, it opens automatically so that the refresh button is visible.

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/en/loading_status.webp"  width="840" style="max-width: 100%;" class="mb-3"/>
<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/en/loading_error.webp"  width="840" style="max-width: 100%;" class="mb-3"/>

##### Selecting a Connector
- In general, you do not need to change the default value, DMM direct.
- If you want to use an OOI-based connector ([ooi.moe](https://ooi.moe/)), change it in Settings → Login and Connection → Select Login Method.
  - When you select an unofficial connector for the first time, a notice that game resources may differ from the original ones is shown.
- Changing the connector takes effect after a reload.

##### Login Autocomplete
- You can save the ID and password to be entered in the login form in Settings → Login and Connection → Login Autocomplete.
- The account information is only stored within the device and not used for purposes other than autocompletion.

##### Dialogs That May Appear on Startup
| Dialog | Description |
| --- | --- |
| Update available | Asks whether to download a new version if one exists. (Same as Settings → App Information → Check Update) |
| Kcanotify Detected | Shown when Kcanotify is installed but Broadcast Mode is turned off. Press OK to turn it on. (See <span class="link" data-move="kcanotify">Using with Kcanotify</span>) |
| Foreign IP Block Detected | Shown when a foreign IP block is detected while Gadget Block Bypass is turned off. (See <span class="link" data-move="gadgetbypass">IP Block Bypass (Non-Japan)</span>) |

##### Changes from Previous Versions
| Previous versions (main screen) | 4.0 and later |
| --- | --- |
| Select Connector Site | Settings → Login and Connection → Select Login Method |
| Login Autocompletion | Settings → Login and Connection → Login Autocomplete |
| Start with Audio OFF / Broadcast Mode / Gadget Server Bypass | Settings → Login and Connection |
| Show Control Panel on Start / Enable Keyboard Input | Settings → Controls & Toolbar |
| Clear Cache | Settings → Asset Caching → Clear Cache |
| Manual Website (<span class="material-symbols-outlined inline-icon">help</span>) | Help button at the top of the settings screen |
| Settings Menu (<span class="material-symbols-outlined inline-icon">settings</span>) | Settings button in the top right of the game screen |
