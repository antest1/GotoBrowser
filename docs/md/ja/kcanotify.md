## Kcanotifyとの連動

GotoBrowserを[kcanotify](https://github.com/antest1/kcanotify)と使用するための設定方法です。

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/ja/kcanotify_settings.webp"  width="840" style="max-width: 100%;"/>

##### GotoBrowser設定

- 設定 → ログインと接続で<span class="text-danger">ブロードキャストモード</span>を有効にします。（デフォルトで有効になっています）
  - Kcanotifyがインストールされた状態でブロードキャストモードが無効の場合、起動時に有効にするかを確認する案内が表示されます。
  - 設定は再読み込みなしですぐに反映されます。

##### Kcanotify設定
- 右の例のように設定→Kcanotify設定で次のように設定します。
  - 実行するアプリケーション：<span class="text-danger">GotoBrowser</span>
  - Snifferモード: <span class="text-danger">PASSIVE</span>
- 注：これらの設定は、サービスがオフになっている状態でのみ変更できます。


