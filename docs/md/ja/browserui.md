## ブラウザUI
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@40,400,0,0&icon_names=closed_caption,crop_free,frame_reload,light_mode,photo_camera,refresh,screen_lock_rotation,settings,volume_off" />


<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/ja/game_screen.webp"  width="840" style="max-width: 100%;" class="mb-3"/>

ゲーム画面の端にあるツールバーを使用すると、ブラウザでさまざまな機能を利用できます。（ページの再読み込みやスクリーンショットなど）
- 縦向きでは画面下部に横向きに、横向きでは画面左側に縦向きに表示されます。
- ボタンを長押しすると、ボタンの名前が表示されます。

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/ja/game_screen_portrait.webp"  width="300" style="max-width: 100%;" class="mb-3"/>

##### ツールバーを表示/非表示
- ツールバーを表示するには、次の方法があります。
  - ゲーム画面の外側の黒い部分をタッチ（おすすめ）
  - ツールバーが隠れている画面の端（縦向き: 下、横向き: 左）から内側にスワイプ（設定 → 操作とツールバー → 「端からスワイプしてツールバーを表示」で無効にできます）
  - ゲーム画面の外側に余白がない場合は、戻るボタン（または戻るジェスチャー）で表示
  - 設定 → 操作とツールバー → 「起動時にパネルを表示」を有効にして起動時に表示する
- 逆にツールバーを消すには、次の方法があります。
  - 表示されている状態でゲーム画面の外側の黒い部分を再びタッチ
  - ツールバーを画面の端の方向にスワイプ

##### リフレッシュ
ツールバーの横にあるリフレッシュボタンには、2つの方式があります。
横画面では、設定 → 操作とツールバー → 「ツールバーの表示位置」を「上・クラシック」にすると、3.xと同じくツールバーが画面の上に寄り、リフレッシュボタンが一番上に来ます。

|  | 機能 | 説明 |
| --- | --- | --- |
| <span class="material-symbols-outlined">frame_reload</span> | クイックリフレッシュ | ページ全体ではなく、ゲームフレームのみを再読み込みします。<br/>速いですが常に成功するとは限りません。ネットワークエラーが発生した場合はフルリフレッシュを使用してください。 |
| <span class="material-symbols-outlined">refresh</span> | フルリフレッシュ | ページ全体を再読み込みします。<br/>最も確実なリフレッシュ方法ですが、クイックリフレッシュより時間がかかります。 |

- ボタンを押したままメニューが開く方向にドラッグすると、もう一方の方式を選択できます。ツールバーの表示位置が「中央」なら上に、「上・クラシック」なら下に開きます。最後に使用した方式がボタンのデフォルト動作として記憶されます。
- 設定 → 操作とツールバー → 「確認なしでリフレッシュ」を有効にすると、リフレッシュ前の確認ダイアログが表示されません。
- 設定を変更した後の「再読み込みが必要です」ダイアログで「再読み込みせずに設定を変更」を選んだ場合も、このボタンでリフレッシュすると変更した設定が反映されます。
- 読み込み中の接続エラーやゲーム中のサーバーエラーの後は、保存された方式に関係なくボタンがフルリフレッシュとして動作し、ボタンの横に「フルリフレッシュ」と表示されます。保存された方式は変わりません。

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/ja/refresh_menu.webp"  width="560" style="max-width: 100%;" class="mb-3"/>

##### ツールバーボタンの説明
|  | 機能 | 説明 |
| --- | --- | --- |
| <span class="material-symbols-outlined">volume_off</span>  | ミュート | ブラウザで再生される音をミュートします。音声ファイルは引き続き読み込みます。<br/>（音声なしで字幕だけを見たい場合などに使用） |
| <span class="material-symbols-outlined">photo_camera</span> | スクリーンショット | スクリーンショットモードを切り替えます。有効にすると、画面右上の設定ボタンの下にスクリーンショットボタンが表示されます。 |
| <span class="material-symbols-outlined">screen_lock_rotation</span> | 回転ロック | 現在の画面状態でデバイス設定に関係なく回転しないように設定します。 |
| <span class="material-symbols-outlined">light_mode</span> | 常時画面オン | 待機状態で画面が暗くならないように設定します。 |
| <span class="material-symbols-outlined">closed_caption</span> | 字幕 | 画面下部にインゲーム音声の字幕を表示または非表示にします。表示中の字幕をタッチすると、次のセリフまで非表示になります。 |

##### 右上のボタン
|  | 機能 | 説明 |
| --- | --- | --- |
| <span class="material-symbols-outlined">settings</span> | 設定 | <span class="link" data-move="settings">設定画面</span>を開きます。ゲームは設定画面の後ろで動作し続けます。 |
| <span class="material-symbols-outlined">crop_free</span> | スクリーンショット | 現在のゲーム画面を保存します。スクリーンショットモードが有効なときのみ表示されます。（<span class="link" data-move="screenshot">スクリーンショット保存</span>を参照） |

##### ゲーム中のエラー案内
ゲーム中にサーバーとの通信に問題が起きると、ゲーム画面の中央に案内が表示されます。ゲーム自体のエラーメッセージを隠さない位置です。

| 案内 | 説明 |
| --- | --- |
| 通信エラー | 接続が切れたり応答がなかったりした場合に表示されます。接続を確認してからリフレッシュしてください。 |
| サーバー応答エラー | サーバーがHTTPエラーを返した場合に表示されます。しばらくしてからリフレッシュしてください。 |
| サーバーエラー | サーバーがリクエストを拒否した場合（api_result）に表示されます。クイックリフレッシュでは復旧できないため、リフレッシュボタンは自動的にフルリフレッシュとして動作します。 |

- 案内をタッチすると閉じます。
- 案内を長押しすると、コード・API・メッセージがコピーされます。お問い合わせの際に一緒にお送りください。

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/ja/game_notice.webp"  width="840" style="max-width: 100%;" class="mb-3"/>

##### ログアウト
- ログアウトは、設定 → ログインと接続 → ログアウトから行えます。
- ログアウトすると、コネクタのログインページが再び開きます。
