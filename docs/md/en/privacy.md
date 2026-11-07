## Privacy Policy

**Updated: 2026.11.07** (initial version)

GotoBrowser ("the app") is an open-source browser for playing KanColle (HTML5) on Android devices. The developer does not operate any server for collecting user information. This policy explains what information the app handles and how.

##### Information Stored on Your Device

The following information is stored only on your device and is never sent to the developer.

- App settings
- Login autofill credentials (ID/password): stored only if you enter them in the settings, and used only to fill in the login page. The password is encrypted with a key held in the Android Keystore. The key never leaves your device, so if the app data is restored from a backup on another device, the password cannot be read and is deleted.
- Cookies, login sessions and game resource cache
- Screenshots: saved to the pictures folder on your device when you take them.
- Game traffic data for Kcanotify: a few of the most recent records are kept, with the authentication token removed.

##### Communication with Game and Connection Services

The app communicates with the DMM and KanColle servers to play the game. If you choose a web proxy service run by a third party as the connection method, your game traffic, including your login credentials, passes through that service. Such services are unrelated to the developer, and their own privacy policies apply. If you enable Gadget Server Bypass, some game resource requests are sent to the configured alternative server.

##### Effect on Gameplay

The app does not modify any traffic exchanged with the KanColle/DMM servers, and it does not provide any macro or cheat features that affect gameplay.

##### Kcanotify Integration

When Broadcast Mode is enabled (default), the app passes game traffic to the Kcanotify app installed on the same device, with the authentication token replaced by a dummy value. This data does not leave your device, and you can turn off Broadcast Mode in the settings.

##### Error Reports and Usage Statistics

To find and fix problems, the app uses Google Firebase (Crashlytics and Analytics). Error details, device information such as the device model, OS and app version, and app usage data may be collected. Game account information, login credentials and gameplay data are not collected. Collected information is handled under Google's policies. (https://policies.google.com/privacy, https://firebase.google.com/support/privacy)

##### External Data Downloads

Some features, such as the app update check, voiceline subtitles and third-party mods, download the data they need from external servers. Standard network request information such as your IP address may be visible to those servers, but no user information is sent.

##### App Permissions

- Internet, network state: connecting to the game and downloading data
- Storage (Android 12 and below): saving screenshots and cache
- Notifications: showing a notification when a screenshot is saved

##### Deleting Your Information

You can delete your login autofill credentials in the settings. Clearing the app data or uninstalling the app deletes the information stored on your device, except saved screenshots, which remain on your device.

##### Notification of Policy Changes

Important updates about user privacy will be announced through GitHub Releases or the README. (https://github.com/antest1/GotoBrowser)

##### Questions

If you have any questions about this privacy policy, feel free to contact the developer.

antest1(IE10), E-mail: kcanotify@gmail.com
