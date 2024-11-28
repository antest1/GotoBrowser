<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20,400,0,0&icon_names=settings" />

## 設定 

メイン画面の右上にある歯車アイコン(<span class="material-symbols-outlined inline-icon">settings</span>)をクリックすると設定に入ることができます。

##### ブラウザ設定

| 設定 | 説明 | 
| --- | -- |
| 横固定モード | 端末の向きに関係なく常に横向きでプレイする |
| フォントをプリロード | ゲームサーバーからフォントファイルを読み込むのではなく、アプリに埋め込まれたファイルを使用する（起動速度が若干速くなる） |
| キャッシングに外部ストレージを使用 | ゲームのリソースをキャッシュするときに内部ストレージの代わりに外部ストレージを使用する（PCで画像や音声リソースにアクセス可能） |
| PIPモードを有効にする | 他のアプリやホーム画面に切り替えるとき、小さな画面でゲームを表示する（Android 8.0以降） |
| [マルチウィンドウ] 分割バーから余白を追加 | マルチウィンドウ機能を使用するときに画面がバーに直接貼らないように余白を追加<br/>（Samsung Galaxy・Android 7.0限定） |
| レガシーレンダラを使用する | PixiJSゲームエンジンにWebGLレンダラーの代わりにCanvasレンダラーを使用するように強制する（画像関連の問題が発生しない場合は解除状態にしてください） | 

##### 接続設定

| 設定 | 説明 | 
| --- | --- |
| 国外ブロックのバイパス | 海外でIPブロックの問題が発生した場合は有効にする必要があります |
| バイパス方式 | 「URL置換」または「KCCacheProxy」を選択可能（ほとんどの場合「URL置換」を使用） |
| エンドポイントサーバー | バイパスに使用するサーバーURL<br/>`https://kcwiki.github.io/cache/`または`https://luckyjervis.com/`を使用 |

バイパスについては、<span class="link" data-move="gadgetbypass">海外IPブロックバイパス</span>ページをご覧ください。

##### 字幕設定

| 設定 | 説明 | 
| --- | --- |
| 字幕ロケール | 選択した言語でボイス字幕を表示 |
| 字幕データのダウンロード | データ更新が存在する場合はダウンロード可能 |

機能の詳細は、<span class="link" data-move="voiceline">ゲームボイス字幕</span>ページをご覧ください。

##### 非公式MOD

| 設定                     | 説明 | 作成者 |
|------------------------| --- | --- |
| 60FPS制限を削除します          | 高い画面再生率（120hz）に対応するデバイスでスムーズなアニメーション再生を支援 | [@laplamgor](https://x.com/laplamgor) |
| Kantai3D v4.0          | 一部の秘書艦に3Dのような視覚効果とおける乳揺れを追加 | [@laplamgor](https://x.com/laplamgor) |
| 真のクリティカルヒット            | ハードコードされたヒットの難読化を削除する（CL2のみ表示） | [@Oradimi](https://x.com/oradimi) |
| KanColle English Patch | ゲーム内リソースに[英語パッチ](https://github.com/Oradimi/KanColle-English-Patch-KCCP)を適用（ベータ版） | [@Oradimi](https://x.com/oradimi) |

##### アプリケーション情報

| 設定 | 説明 |
| --- | --- |
| 更新の確認 | 新しいバージョンがある場合はダウンロードページを表示 | 
| DevToolsデバッグを使用 | Chromeなどのブラウザで[リモートデバッグ](https://developer.chrome.com/docs/devtools/remote-debugging?hl=ja)ができるようにする (chrome://inspect) |