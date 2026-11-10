<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20,400,0,0&icon_names=help,settings" />

## 開始遊戲

啟動應用程式後，不會經過額外的主畫面，而是直接開啟登入方式的起始頁面（預設值：DMM direct）。  
4.0 之前版本主畫面上的項目，已全部移至<span class="link" data-move="settings">設定畫面</span>。

##### 啟動流程
1. 開啟登入方式的起始頁面。在登入頁面中，會自動填入已儲存在設定中的帳號資訊。
2. 登入後，遊戲畫面會配合裝置螢幕顯示。（設定 → 遊戲畫面顯示 → 自動調整佈局）
3. 載入遊戲期間，遊戲畫面上會顯示載入提示，遊戲開始後即會消失。（請參閱下方「載入提示」）
4. 輕觸遊戲畫面外側的黑色空白處即可顯示<span class="link" data-move="browserui">工具列</span>，按右上角的設定按鈕（<span class="material-symbols-outlined inline-icon">settings</span>）即可開啟設定畫面。

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/zh-tw/first_launch.webp"  width="840" style="max-width: 100%;" class="mb-3"/>

##### 載入提示
載入遊戲期間，遊戲畫面中央會顯示進度。很快載入完成時不會顯示。
- 依序經過 準備 → 連線至伺服器 → 下載資源，右上角會顯示目前階段（1/3～3/3）。
- 下載資源期間會縮小顯示於畫面下方，以免遮住遊戲本身的載入畫面。
- 不會顯示在登入頁面等一般網頁上。

| 狀態 | 說明 | 處理方式 |
| --- | --- | --- |
| 進行中 | 顯示目前階段與進度。 | 請稍候。 |
| 沒有回應（黃色） | 超過 20 秒沒有任何回應時顯示。 | 使用以黃色標示的重新整理按鈕再試一次。 |
| 連線錯誤（紅色） | 無法載入頁面時顯示原因，下方列出錯誤代碼與主機。 | 確認網路連線後按重新整理按鈕，此時會自動進行完全重新整理。 |

- 若工具列為隱藏狀態，會自動顯示，讓重新整理按鈕可見。

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/zh-tw/loading_status.webp"  width="840" style="max-width: 100%;" class="mb-3"/>
<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/zh-tw/loading_error.webp"  width="840" style="max-width: 100%;" class="mb-3"/>

##### 選擇登入方式
- 一般情況下，不需要變更預設的 DMM direct。
- 如果想使用 OOI 架構的登入方式（[ooi.moe](https://ooi.moe/)），可在設定 → 登入與連接 → 選擇登入方式中變更。
  - 第一次選擇非官方登入方式時，會顯示遊戲資源可能與原始資源不同的聲明。
- 變更登入方式後，需重新載入才會生效。

##### 自動登入
- 可在設定 → 登入與連接 → 自動登入中，儲存要填入登入表單的帳號與密碼。
- 這些資訊只會儲存在裝置內，不會用於自動填入以外的用途。

##### 啟動時可能出現的提示
| 提示 | 說明 |
| --- | --- |
| 更新提示 | 有新版本時，會詢問是否下載。（與設定 → 應用程式資訊 → 檢查更新相同） |
| 檢測到Kcanotify | 已安裝 Kcanotify 但「使用廣播訊息方式」為關閉狀態時顯示。按「確定」即會開啟。（請參閱<span class="link" data-move="kcanotify">搭配 Kcanotify 使用</span>） |
| 檢測到外國IP封锁 | 「啟用登入伺服器封鎖繞行」為關閉狀態，且偵測到海外 IP 封鎖時顯示。（請參閱<span class="link" data-move="gadgetbypass">繞過海外 IP 封鎖</span>） |

##### 與舊版本的差異
| 舊版本（主畫面） | 4.0 以後 |
| --- | --- |
| 選擇登入網站 | 設定 → 登入與連接 → 選擇登入方式 |
| 自動登入 | 設定 → 登入與連接 → 自動登入 |
| 以靜音模式啟動／使用廣播訊息方式／啟用登入伺服器封鎖繞行 | 設定 → 登入與連接 |
| 顯示控制台／允許鍵盤輸入 | 設定 → 操作與工具列 |
| 清除快取 | 設定 → 快取設定 → 清除快取 |
| 說明網站（<span class="material-symbols-outlined inline-icon">help</span>） | 設定畫面上方的說明按鈕 |
| 設定畫面（<span class="material-symbols-outlined inline-icon">settings</span>） | 遊戲畫面右上角的設定按鈕 |
