<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20,400,0,0&icon_names=help,settings" />

## Starting the Game

When you launch the app, the start page of the connector (default: DMM direct) opens right away. There is no separate main screen.  
Everything that used to be on the main screen before version 4.0 has moved to the <span class="link" data-move="settings">settings screen</span>.

##### Startup Flow
1. The start page of the connector opens. On the login page, the account information saved in the settings is filled in automatically.
2. After logging in, the game screen is fitted to your device screen. (Settings → Display → Auto Layout Adjustment)
3. Touch the black margin outside the game screen to show the <span class="link" data-move="browserui">toolbar</span>. The settings button (<span class="material-symbols-outlined inline-icon">settings</span>) in the top right opens the settings screen.

##### Selecting a Connector
- In general, you do not need to change the default value, DMM direct.
- If you want to use an OOI-based connector ([ooi.moe](https://ooi.moe/) or [kancolle.moe](https://kancolle.moe/)), change it in Settings → Login and Connection → Select Login Method.
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
| Show Control Panel on Start / Enable Keyboard Input | Settings → Controls |
| Clear Cache | Settings → Asset Caching → Clear Cache |
| Manual Website (<span class="material-symbols-outlined inline-icon">help</span>) | Help button at the top of the settings screen |
| Settings Menu (<span class="material-symbols-outlined inline-icon">settings</span>) | Settings button in the top right of the game screen |
