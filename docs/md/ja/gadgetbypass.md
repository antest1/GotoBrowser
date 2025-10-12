## 海外IPブロックバイパス

<div class="alert alert-danger" role="alert">日本内でプレイする場合には以下の内容を行う必要はありません。</div>

2020年2月以降、「艦これ」運営（C2機関）側でディドス攻撃などを理由に艦これのガゼットサーバーに対して海外IPをブロックした状態です。したがって、現在の時点で日本外の国で艦これをプレイするためには、このブロックを迂回する必要があります。

海外IPブロックが確認された場合は、ゲーム開始時に以下のような通知が表示されます。

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/ja/bypass_required.png"  width="480" style="max-width: 100%;"/>

##### 設定方法

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/ja/gadget_options.png"  width="480" style="max-width: 100%;" class="mb-3"/>

1. 設定で「国外ブロックのバイパス」オプションを有効にします。  
  （初期インストール時に日本語以外の言語に設定されている場合はデフォルトでオンになります）
2. バイパス方式は「URL置換」を、エンドポイントサーバーは以下の2つのURLのいずれかに設定してください。誤字に注意！

| エンドポイントサーバー URL | 備考 |
| --- | --- |
| `https://kcwiki.github.io/cache/` | デフォルト |
| `https://luckyjervis.com/` | | 