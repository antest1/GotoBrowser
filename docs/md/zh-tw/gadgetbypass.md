## 繞過海外 IP 封鎖

<div class="alert alert-danger" role="alert">2024/12/03 以後海外 IP 封鎖已解除，不再需要使用此功能。</div>

自 2020 年 2 月起，艦C營運方（C2機關）以 DDoS 攻擊等為由，對艦C登入伺服器封鎖了海外 IP。  
因此，目前若要在日本以外的國家正常遊玩艦C，必須繞過上述封鎖。

偵測到 IP 封鎖時，遊戲啟動時會出現如下的通知。

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/zh-tw/bypass_required.webp"  width="480" style="max-width: 100%;"/>

##### 設定方法

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/zh-tw/gadget_options.webp"  width="600" style="max-width: 100%;" class="mb-3"/>

1. 在設定 → 登入與連接的「登入伺服器封鎖繞行」區塊中，開啟「啟用登入伺服器封鎖繞行」選項。
2. 將繞行方式設為「URL Replacement」，端點伺服器設為以下位址即可。（注意不要打錯字！）
3. 出現「需要重新載入」提示時，按「現在重新載入」重新載入遊戲。

| 端點伺服器位址 | 備註 |
| --- | --- |
| `https://kcwiki.github.io/cache/` | 預設值 |

##### 參考
- 在繞行開啟的狀態下，若確認封鎖已解除，繞行選項會自動關閉。若要保持開啟，請關閉同一區塊中的「自動停用繞行」。
- 反之，在繞行關閉的狀態下，若偵測到封鎖，會再次顯示上述通知。
