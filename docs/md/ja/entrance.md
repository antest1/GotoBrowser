<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20,400,0,0&icon_names=help,settings" />

## ゲームの開始

アプリを起動すると、メイン画面を経由せずにコネクタ（デフォルト: DMM direct）のスタートページがすぐに開きます。  
4.0より前のバージョンでメイン画面にあった項目は、すべて<span class="link" data-move="settings">設定画面</span>に移動しました。

##### 起動の流れ
1. コネクタのスタートページが開きます。ログインページでは、設定に保存したアカウント情報が自動で入力されます。
2. ログインすると、ゲーム画面がデバイスの画面に合わせて表示されます。（設定 → 表示 → レイアウト自動調整）
3. ゲーム画面の外側の黒い部分をタッチすると<span class="link" data-move="browserui">ツールバー</span>が表示され、右上の設定ボタン（<span class="material-symbols-outlined inline-icon">settings</span>）から設定画面を開けます。

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/ja/first_launch.webp"  width="840" style="max-width: 100%;" class="mb-3"/>

##### 接続方式の選択
- ほとんどの場合、デフォルトの「DMM direct」を変更する必要はありません。
- OOIベースのコネクタ（[ooi.moe](https://ooi.moe/)または[kancolle.moe](https://kancolle.moe/)）を使用したい場合は、設定 → ログインと接続 → 接続方式選択で変更できます。
  - 非公式のコネクタを初めて選択すると、ゲームリソースが本来のものと異なる場合があるという案内が表示されます。
- 接続方式を変更すると、再読み込み後に反映されます。

##### ログインオートコンプリート（自動入力）
- 設定 → ログインと接続 → ログインオートコンプリートで、ログインフォームに入力するIDとパスワードを保存できます。
- 入力した情報はデバイス内にのみ保存され、自動入力機能以外の目的には使用されません。

##### 起動時に表示されることがある案内
| 案内 | 説明 |
| --- | --- |
| アップデートの案内 | 新しいバージョンがある場合、ダウンロードするかを確認します。（設定 → アプリケーション情報 → 更新の確認と同じ） |
| Kcanotifyが検出されました | Kcanotifyがインストールされているのにブロードキャストモードが無効の場合に表示されます。「確認」を押すとブロードキャストモードが有効になります。（<span class="link" data-move="kcanotify">Kcanotifyとの連動</span>を参照） |
| 国外IPアクセス制限が検出されました | 国外ブロックのバイパスが無効の状態で海外IPブロックが確認された場合に表示されます。（<span class="link" data-move="gadgetbypass">海外IPブロックバイパス</span>を参照） |

##### 以前のバージョンとの違い
| 以前のバージョン（メイン画面） | 4.0以降 |
| --- | --- |
| 接続サイトの選択 | 設定 → ログインと接続 → 接続方式選択 |
| ログインオートコンプリート | 設定 → ログインと接続 → ログインオートコンプリート |
| 音量OFFでスタート / ブロードキャストモード / 国外ブロックのバイパス | 設定 → ログインと接続 |
| 起動時にパネルを表示 / キーボード入力を有効に | 設定 → 操作 |
| キャッシュ削除 | 設定 → キャッシュ → キャッシュ削除 |
| マニュアルサイト（<span class="material-symbols-outlined inline-icon">help</span>） | 設定画面上部のヘルプボタン |
| 設定画面（<span class="material-symbols-outlined inline-icon">settings</span>） | ゲーム画面右上の設定ボタン |
