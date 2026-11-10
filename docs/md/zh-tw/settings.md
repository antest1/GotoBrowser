<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20,400,0,0&icon_names=help,settings" />

## 設定 

在工具列顯示的狀態下，按遊戲畫面右上角的齒輪圖示（<span class="material-symbols-outlined inline-icon">settings</span>）即可進入

- 設定畫面會顯示在遊戲畫面上方，遊戲會在後方持續執行。
- 將上方的拖曳把手往下拉，或按下返回鍵即可關閉。
- 按右上方的說明按鈕（<span class="material-symbols-outlined inline-icon">help</span>）即可開啟本說明書。

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/zh-tw/settings_main.webp"  width="560" style="max-width: 100%;" class="mb-3"/>

##### 設定套用方式

| 套用 | 說明 |
| --- | --- |
| 立即 | 變更數值後立即套用。 |
| 重新載入 | 會顯示「需要重新載入」提示。按「現在重新載入」會在完全重新整理後套用；按「暫不重新載入」則會在下次重新整理時套用。 |

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/zh-tw/settings_refresh_required.webp"  width="480" style="max-width: 100%;" class="mb-3"/>

##### 登入與連接

| 群組 | 名稱 | 說明 | 套用 |
| --- | --- | --- | --- |
| 遊戲登入 | 自動登入 | 自動填入登入表單的帳號與密碼。僅儲存在裝置內 | 重新載入 |
| 遊戲登入 | 登出 | 登出遊戲並返回登入方式的登入頁面 | |
| 瀏覽器設定 | 選擇登入方式 | DMM direct（預設值）/ ooi.moe<br/>第一次選擇非官方登入方式時，會顯示遊戲資源相關的聲明 | 重新載入 |
| 瀏覽器設定 | 以靜音模式啟動 | 遊戲啟動時將 BGM/SE/VOICE 音量設為 0（與官方應用程式的功能相同）<br/>僅限 DMM direct 使用 | 重新載入 |
| 瀏覽器設定 | 使用廣播訊息方式 | 將艦C相關的通訊內容傳送給 Kcanotify 等其他應用程式使用（預設開啟）<br/>若要與 Kcanotify 連動，必須保持開啟 | 立即 |
| 登入伺服器封鎖繞行 | 啟用登入伺服器封鎖繞行 | 發生海外 IP 封鎖問題（停在黑畫面）時開啟 | 重新載入 |
| 登入伺服器封鎖繞行 | 繞行方式 | 選擇 URL Replacement 或 KCCacheProxy (remote)（大多數情況下使用「URL Replacement」） | 重新載入 |
| 登入伺服器封鎖繞行 | 端點伺服器 | 繞行時使用的伺服器位址<br/>`https://kcwiki.github.io/cache/`（預設值） | 重新載入 |
| 登入伺服器封鎖繞行 | 自動停用繞行 | 未遭封鎖而正常連線時，自動關閉繞行設定（預設開啟） | 立即 |

繞行相關內容請參閱<span class="link" data-move="gadgetbypass">繞過海外 IP 封鎖</span>頁面。「啟用登入伺服器封鎖繞行」關閉時，無法選擇繞行方式與端點伺服器。

##### 遊戲畫面顯示

| 名稱 | 說明 | 套用 |
| --- | --- | --- |
| 固定橫向畫面 | 不論裝置方向為何，一律以橫向模式遊玩（預設開啟） | 立即 |
| 自動調整佈局 | 自動放大／縮小遊戲畫面以符合裝置螢幕大小（建議，預設開啟） | 重新載入 |
| 遊戲畫面垂直位置 | 直向畫面、分割畫面等遊戲上下留有空白時，將遊戲置中或置於頂部（預設置中） | 立即 |
| 啟用畫中畫模式 | 返回主畫面或在遊戲畫面上雙指縮小時，以浮動小視窗顯示遊戲畫面 | 立即 |
| [多視窗模式] 在分隔條增添間隔 | 使用多視窗功能時加上間隔，避免畫面緊貼分隔條 | 立即 |
| 改用舊式渲染器 | 將 WebGL 渲染模式改為 Canvas（若未發生圖形問題，建議關閉）<br/>開啟時無法使用 Kantai3D | 重新載入 |

PIP/多視窗相關內容請參閱<span class="link" data-move="pipmode">PIP/多視窗</span>頁面

##### 操作與工具列

| 群組 | 名稱 | 說明 | 套用 |
| --- | --- | --- | --- |
| 工具列 | 啟動時顯示控制台 | 應用程式啟動時，以顯示工具列的狀態開始（預設開啟） | 下次啟動 |
| 工具列 | 免確認直接重新整理 | 按下重新整理按鈕時，不顯示確認警告，直接重新整理畫面 | 立即 |
| 工具列 | 工具列顯示位置 | 置中（預設）或頂部 · 經典。頂部 · 經典會像 3.x 一樣把工具列靠在畫面上方，並把重新整理按鈕放在最上面。橫向畫面時適用 | 立即 |
| 工具列 | 從邊緣滑動顯示工具列 | 從工具列隱藏的畫面邊緣往內滑動以顯示工具列（預設開啟）<br/>關閉後，邊緣滑動僅用於系統手勢<br/>沒有空白處的畫面中，按返回鍵顯示工具列 | 立即 |
| 遊戲操作 | 允許鍵盤輸入 | 是否在遊戲畫面中使用鍵盤輸入（預設開啟） | 立即 |
| 遊戲操作 | 輸入方式 | 選擇遊玩時的輸入方式（螢幕觸控或使用滑鼠） | 重新載入 |

##### 快取設定

| 名稱 | 說明 | 套用 |
| --- | --- | --- |
| 將快取存放至外部儲存空間 | 快取遊戲資源時，使用外部儲存空間而非內部儲存空間（建議）<br/>（可透過電腦存取或編輯圖片／語音資源） | 重新載入 |
| 預先載入字體 | 使用應用程式內建的字體檔案，而不從遊戲伺服器載入字體（建議，預設開啟） | 重新載入 |
| 重新嘗試下載遊戲資源 | 聲音／圖片資源下載失敗時，顯示詢問是否重新下載的視窗<br/>可減少網路不穩定導致的貓襲。不適用於遊戲 API 通訊 | 立即 |
| 清除快取 | 刪除瀏覽器內的快取。在圖片或聲音無法正常載入，或切換登入方式時使用<br/>清除後可能需要重新登入<br/>清除完成後會顯示重新載入的提示 | 立即 |

##### 字幕設定

| 名稱 | 說明 | 套用 |
| --- | --- | --- |
| 字幕語言 | 從 English / 한국어 / 日本語（KC3改）、简中 / 繁中（kcwiki）中選擇 | 立即 |
| 字體大小 | 調整畫面中語音字幕的大小（12~24，預設值 18） | 立即 |
| 下載字幕資料 | 有資料更新時可以下載。下載後立即套用 | 立即 |

功能相關內容請參閱<span class="link" data-move="voiceline">遊戲內台詞字幕</span>頁面

##### 第三方MOD

<span class="text-danger">第三方 MOD 請自行承擔風險使用。</span>

| 群組 | 名稱 | 說明 | 套用 | 作者 |
| --- | --- | --- | --- | --- |
| 常規 | FPS渲染 Mod | 使用 VSYNC 渲染，比原版遊戲更順暢且運行效率更高（支援高螢幕更新率裝置） | 重新載入 | [@laplamgor](https://x.com/laplamgor) |
| 常規 | FPS限制 | 從預設（最高）或裝置支援的更新率中選擇。降低目標 FPS 可節省電量並減少發熱 | 立即 | |
| 常規 | 真正的Critical hit | 移除傷害的硬編碼混淆。僅在發生 CL2 時才顯示為 Critical hit | 重新載入 | [@Oradimi](https://x.com/oradimi) |
| Kantai3D 視覺模組 | Kantai3D v4.0 | 為部分秘書艦套用 3D 效果。使用舊式渲染器時無法選擇 | 重新載入 | [@laplamgor](https://x.com/laplamgor) |
| Kantai3D 視覺模組 | 關於Kantai3D | 開啟 [Kantai3D GitHub 頁面](https://github.com/laplamgor/kantai3d) | | |
| 語言翻譯模組 | KanColle Language Patch (KCCP) | 為遊戲內文字套用翻譯補丁 | 重新載入 | [@Oradimi](https://x.com/oradimi) |
| 語言翻譯模組 | 要使用的補丁 | [KanColle English Patch](https://github.com/Oradimi/KanColle-English-Patch-KCCP)（@Oradimi）或 [KanColle Indonesia Patch](https://github.com/SLAVUSworks/KanColle-Indonesia-Patch-KCCP)（@SLAVUSworks） | 重新載入 | |
| 語言翻譯模組 | 下載補丁資料 | 下載或更新所選補丁的資料。完成後需重新整理遊戲才會生效 | 重新載入 | |
| 語言翻譯模組 | 刪除補丁資料 | 補丁出現預期外的行為，或下載過程中發生問題時使用 | 重新載入 | |
| 語言翻譯模組 | KanColle English/Indonesia Patch關於 | 開啟所選補丁的 GitHub 頁面 | | |

##### 應用程式資訊

| 名稱 | 說明 | 套用 |
| --- | --- | --- |
| 目前版本 | 已安裝的應用程式版本 | |
| 檢查更新 | 有新版本時顯示下載頁面 | |
| 原始碼 | 開啟 [GitHub 儲存庫](https://github.com/antest1/GotoBrowser/) | |
| 啟用DevTools除錯 | 讓 Chrome 等瀏覽器可以進行[遠端偵錯](https://developer.chrome.com/docs/devtools/remote-debugging?hl=zh-tw)（chrome://inspect） | 立即 |
