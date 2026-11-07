[한국어](#개인정보처리방침) | [English](#privacy-policy) | [日本語](#プライバシーポリシー)

## 개인정보처리방침

**Updated: 2026.11.07** (최초 작성)

GotoBrowser(이하 "앱")는 Android 기기에서 칸코레(HTML5)를 플레이하기 위한 오픈소스 브라우저입니다. 개발자는 사용자 정보를 수집하기 위한 별도의 서버를 운영하지 않으며, 본 방침은 앱이 다루는 정보와 그 처리 방식을 설명합니다.

1. **기기 내에 저장되는 정보** 아래 정보는 사용자의 기기 내에만 저장되며, 개발자에게 전송되지 않습니다.
   - 앱 설정값
   - 로그인 자동 입력 정보(ID/비밀번호): 사용자가 설정에서 직접 입력한 경우에만 저장되며, 로그인 페이지의 자동 입력에만 사용됩니다. 비밀번호는 Android 키스토어의 키로 암호화하여 저장합니다. 이 키는 기기 밖으로 나가지 않으므로, 앱 데이터 백업을 다른 기기에 복원하면 비밀번호를 읽을 수 없으며 저장된 비밀번호는 삭제됩니다.
   - 쿠키 및 로그인 세션, 게임 리소스 캐시
   - 스크린샷: 사용자가 촬영한 경우 기기의 `Pictures/GotoBrowser` 폴더에 저장됩니다.
   - Kcanotify 연동용 게임 통신 데이터: 최근 통신 내역 최대 10건이 저장되며, 인증 토큰(`api_token`)은 제외됩니다.

2. **게임 및 접속 서비스와의 통신** 앱은 게임 플레이를 위해 DMM 및 칸코레 서버와 통신합니다. 사용자가 접속 방식으로 제3자가 운영하는 웹 프록시 서비스를 선택한 경우, 로그인 정보를 포함한 게임 통신이 해당 서비스를 거쳐 전달됩니다. 이러한 서비스는 개발자와 무관하며, 해당 서비스의 개인정보 처리방침이 적용됩니다. Gadget 서버 우회 기능을 활성화한 경우, 일부 게임 리소스 요청이 설정된 대체 서버로 전달됩니다.

3. **게임 플레이에 대한 영향** 앱은 칸코레 및 DMM 서버와 주고받는 통신 내용을 변경하지 않으며, 매크로나 치트 등 게임 플레이에 영향을 주는 기능을 제공하지 않습니다.

4. **Kcanotify 연동** 브로드캐스트 모드가 활성화된 경우(기본값), 게임 통신 내용을 같은 기기에 설치된 Kcanotify 앱으로 전달합니다. 이때 인증 토큰(`api_token`)은 임의의 값으로 바꾸어 전달합니다. 이 데이터는 기기 외부로 전송되지 않으며, 설정에서 브로드캐스트 모드를 끌 수 있습니다.

5. **오류 보고 및 사용 통계** 앱의 문제를 진단하고 해결하기 위해 Google Firebase(Crashlytics, Analytics)를 사용합니다. 오류 발생 시의 오류 정보, 기기 모델, OS 및 앱 버전과 같은 기기 정보, 앱 사용 현황 등이 수집될 수 있습니다. 게임 계정 정보, 로그인 정보, 게임 플레이 데이터는 수집하지 않습니다. 수집된 정보의 처리는 Google의 정책을 따릅니다. (https://policies.google.com/privacy, https://firebase.google.com/support/privacy)

6. **외부 데이터 다운로드** 앱 업데이트 확인, 음성 자막, 서드파티 모드 등 일부 기능은 외부 서버에서 필요한 데이터를 내려받습니다. 이 과정에서 IP 주소 등 일반적인 네트워크 요청 정보가 해당 서버에 전달될 수 있으며, 사용자 정보는 전송되지 않습니다.

7. **앱 권한**
   - 인터넷, 네트워크 상태: 게임 접속 및 데이터 다운로드
   - 저장소(Android 12 이하): 스크린샷 및 캐시 저장
   - 알림: 스크린샷 저장 알림 표시

8. **정보의 삭제** 로그인 자동 입력 정보는 설정에서 삭제할 수 있습니다. 앱 데이터를 삭제하거나 앱을 제거하면 기기 내에 저장된 정보가 삭제됩니다. 단, 저장된 스크린샷은 앱을 제거해도 기기에 남습니다.

9. **처리방침의 변경** 개인정보와 관련하여 중요한 변경이 있는 경우에는 GitHub의 Release나 README를 통해 알립니다. (https://github.com/antest1/GotoBrowser)

10. **문의** 개인정보 처리방침과 관련하여 문의사항이 있는 경우 개발자에게 연락 바랍니다.

antest1(IE10), E-mail: kcanotify@gmail.com

<hr/>

## Privacy Policy

**Updated: 2026.11.07** (initial version)

GotoBrowser ("the app") is an open-source browser for playing KanColle (HTML5) on Android devices. The developer does not operate any server for collecting user information. This policy explains what information the app handles and how.

1. **Information Stored on Your Device** The following information is stored only on your device and is never sent to the developer.
   - App settings
   - Login autofill credentials (ID/password): stored only if you enter them in the settings, and used only to fill in the login page. The password is encrypted with a key held in the Android Keystore. The key never leaves your device, so if the app data is restored from a backup on another device, the password cannot be read and is deleted.
   - Cookies, login sessions and game resource cache
   - Screenshots: saved to the `Pictures/GotoBrowser` folder on your device when you take them.
   - Game traffic data for Kcanotify: up to the 10 most recent records are kept, with the authentication token (`api_token`) removed.

2. **Communication with Game and Connection Services** The app communicates with the DMM and KanColle servers to play the game. If you choose a web proxy service run by a third party as the connection method, your game traffic, including your login credentials, passes through that service. Such services are unrelated to the developer, and their own privacy policies apply. If you enable Gadget Server Bypass, some game resource requests are sent to the configured alternative server.

3. **Effect on Gameplay** The app does not modify any traffic exchanged with the KanColle/DMM servers, and it does not provide any macro or cheat features that affect gameplay.

4. **Kcanotify Integration** When Broadcast Mode is enabled (default), the app passes game traffic to the Kcanotify app installed on the same device, with the authentication token (`api_token`) replaced by a dummy value. This data does not leave your device, and you can turn off Broadcast Mode in the settings.

5. **Error Reports and Usage Statistics** To find and fix problems, the app uses Google Firebase (Crashlytics and Analytics). Error details, device information such as the device model, OS and app version, and app usage data may be collected. Game account information, login credentials and gameplay data are not collected. Collected information is handled under Google's policies. (https://policies.google.com/privacy, https://firebase.google.com/support/privacy)

6. **External Data Downloads** Some features, such as the app update check, voiceline subtitles and third-party mods, download the data they need from external servers. Standard network request information such as your IP address may be visible to those servers, but no user information is sent.

7. **App Permissions**
   - Internet, network state: connecting to the game and downloading data
   - Storage (Android 12 and below): saving screenshots and cache
   - Notifications: showing a notification when a screenshot is saved

8. **Deleting Your Information** You can delete your login autofill credentials in the settings. Clearing the app data or uninstalling the app deletes the information stored on your device, except saved screenshots, which remain on your device.

9. **Notification of Policy Changes** Important updates about user privacy will be announced through GitHub Releases or the README. (https://github.com/antest1/GotoBrowser)

10. **Questions** If you have any questions about this privacy policy, feel free to contact the developer.

antest1(IE10), E-mail: kcanotify@gmail.com

<hr/>

## プライバシーポリシー

**Updated: 2026.11.07** (初版)

GotoBrowser(以下「本アプリ」)は、Android端末で艦これ(HTML5)をプレイするためのオープンソースブラウザです。開発者はユーザー情報を収集するためのサーバーを運営していません。本ポリシーでは、本アプリが扱う情報とその取り扱いについて説明します。

1. **端末内に保存される情報** 以下の情報はユーザーの端末内にのみ保存され、開発者に送信されることはありません。
   - アプリの設定
   - ログイン自動入力情報(ID/パスワード):ユーザーが設定で入力した場合のみ保存され、ログインページの自動入力にのみ使用されます。パスワードはAndroidキーストアの鍵で暗号化して保存します。この鍵は端末の外に出ないため、アプリデータのバックアップを別の端末に復元した場合、パスワードは読み取れず削除されます。
   - Cookie、ログインセッション、ゲームリソースのキャッシュ
   - スクリーンショット:撮影した場合、端末の `Pictures/GotoBrowser` フォルダに保存されます。
   - Kcanotify連携用のゲーム通信データ:直近の通信最大10件が保存され、認証トークン(`api_token`)は除外されます。

2. **ゲームおよび接続サービスとの通信** 本アプリはゲームをプレイするためにDMMおよび艦これのサーバーと通信します。接続方式として第三者が運営するWebプロキシサービスを選択した場合、ログイン情報を含むゲーム通信は当該サービスを経由します。これらのサービスは開発者とは関係がなく、各サービスのプライバシーポリシーが適用されます。Gadgetサーバー迂回機能を有効にした場合、一部のゲームリソースのリクエストは設定された代替サーバーに送信されます。

3. **ゲームプレイへの影響** 本アプリは艦これおよびDMMのサーバーとの通信内容を改変せず、マクロやチートなどゲームプレイに影響を与える機能は一切提供しません。

4. **Kcanotify連携** ブロードキャストモードが有効な場合(初期設定)、ゲーム通信の内容を同じ端末にインストールされたKcanotifyアプリに渡します。その際、認証トークン(`api_token`)は任意の値に置き換えて渡します。このデータが端末の外に送信されることはなく、ブロードキャストモードは設定で無効にできます。

5. **エラーレポートおよび利用統計** 問題の把握と解決のため、Google Firebase(Crashlytics、Analytics)を使用しています。エラー情報、端末の機種・OS・アプリのバージョンなどの端末情報、アプリの利用状況が収集されることがあります。ゲームのアカウント情報、ログイン情報、ゲームプレイデータは収集しません。収集された情報はGoogleのポリシーに従って取り扱われます。(https://policies.google.com/privacy, https://firebase.google.com/support/privacy)

6. **外部データのダウンロード** アップデートの確認、ボイス字幕、サードパーティModなど一部の機能は、必要なデータを外部サーバーからダウンロードします。その際、IPアドレスなど一般的なネットワークリクエストの情報が当該サーバーに伝わることがありますが、ユーザー情報は送信されません。

7. **アプリの権限**
   - インターネット、ネットワーク状態:ゲームへの接続およびデータのダウンロード
   - ストレージ(Android 12以下):スクリーンショットおよびキャッシュの保存
   - 通知:スクリーンショット保存時の通知表示

8. **情報の削除** ログイン自動入力情報は設定から削除できます。アプリデータの消去またはアプリのアンインストールにより、端末内に保存された情報は削除されます。ただし、保存したスクリーンショットはアンインストール後も端末に残ります。

9. **ポリシーの変更** 個人情報に関する重要な変更がある場合は、GitHubのReleaseまたはREADMEでお知らせします。(https://github.com/antest1/GotoBrowser)

10. **お問い合わせ** 本ポリシーに関するご質問は、開発者までご連絡ください。

antest1(IE10), E-mail: kcanotify@gmail.com
