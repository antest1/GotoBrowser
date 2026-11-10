## 瀏覽器介面
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@40,400,0,0&icon_names=closed_caption,crop_free,frame_reload,light_mode,photo_camera,refresh,screen_lock_rotation,settings,volume_off" />


<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/zh-tw/game_screen.webp"  width="840" style="max-width: 100%;" class="mb-3"/>

透過遊戲畫面邊緣的工具列，可以執行重新整理、螢幕截圖等多種瀏覽器功能。  
- 直向模式下會橫向顯示於畫面下方，橫向模式下則會直向顯示於畫面左側。
- 長按按鈕即可顯示按鈕名稱。

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/zh-tw/game_screen_portrait.webp"  width="300" style="max-width: 100%;" class="mb-3"/>

##### 顯示／隱藏工具列
- 顯示工具列的方法如下：
  - 輕觸遊戲畫面外側的黑色空白處（建議）
  - 從工具列隱藏的畫面邊緣（直向模式：下方，橫向模式：左側）往內滑動（可在設定 → 操作與工具列 → 「從邊緣滑動顯示工具列」中關閉）
  - 遊戲畫面外側沒有空白處時，按返回鍵（或使用返回手勢）顯示
  - 在設定 → 操作與工具列中開啟「啟動時顯示控制台」，讓工具列在應用程式啟動時顯示
- 反之，隱藏工具列的方法如下：
  - 再次輕觸遊戲畫面外側的黑色空白處
  - 將工具列往畫面邊緣方向滑動

##### 重新整理
工具列旁的重新整理按鈕提供兩種方式。
橫向畫面時，將設定 → 操作與工具列 → 「工具列顯示位置」改為「頂部 · 經典」後，工具列會像 3.x 一樣靠在畫面上方，重新整理按鈕位於最上面。

|  | 功能 | 說明 |
| --- | --- | --- |
| <span class="material-symbols-outlined">frame_reload</span> | 快速重新整理 | 僅重新載入遊戲框架，而不重新整理整個網頁。<br/>速度較快但未必總能成功，若出現網路錯誤，請使用完全重新整理。 |
| <span class="material-symbols-outlined">refresh</span> | 完全重新整理 | 重新載入整個網頁。<br/>這是最可靠的重新整理方式，但比快速重新整理耗時更長。 |

- 按住按鈕並往選單展開的方向拖曳，即可選擇另一種方式。工具列顯示位置為「置中」時選單往上展開，為「頂部 · 經典」時往下展開。最後使用的方式會被記住，作為按鈕的預設動作。
- 開啟設定 → 操作與工具列 → 「免確認直接重新整理」後，重新整理前不再顯示確認視窗。
- 變更設定後，即使在「需要重新載入」提示中選擇了「暫不重新載入」，只要使用此按鈕重新整理，變更的設定就會生效。
- 載入中發生連線錯誤，或遊戲中發生伺服器錯誤後，不論儲存的方式為何，按鈕都會以完全重新整理執行，並在按鈕旁顯示「完全重新整理」。儲存的方式不會改變。

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/zh-tw/refresh_menu.webp"  width="560" style="max-width: 100%;" class="mb-3"/>

##### 工具列按鈕說明
|  | 功能 | 說明 |
| --- | --- | --- |
| <span class="material-symbols-outlined">volume_off</span>  | 靜音 | 將瀏覽器中播放的聲音靜音。語音檔案仍會持續載入。<br/>（適用於只想看字幕而不想聽聲音等情況） |
| <span class="material-symbols-outlined">photo_camera</span> | 螢幕截圖 | 開啟或關閉截圖模式。開啟後，畫面右上角的設定按鈕下方會顯示截圖按鈕。 |
| <span class="material-symbols-outlined">screen_lock_rotation</span> | 自動旋轉鎖定 | 不論裝置設定為何，都將畫面固定在目前的方向，不會旋轉。 |
| <span class="material-symbols-outlined">light_mode</span> | 保持螢幕開啟 | 待機時不讓螢幕變暗。 |
| <span class="material-symbols-outlined">closed_caption</span> | 字幕 | 在畫面下方顯示或隱藏遊戲內字幕。輕觸顯示中的字幕，即可將其隱藏到下一句台詞出現為止。 |

##### 右上角按鈕
|  | 功能 | 說明 |
| --- | --- | --- |
| <span class="material-symbols-outlined">settings</span> | 設定 | 開啟<span class="link" data-move="settings">設定畫面</span>。遊戲會在設定畫面後方持續執行。 |
| <span class="material-symbols-outlined">crop_free</span> | 截圖 | 儲存目前的遊戲畫面。僅在截圖模式開啟時顯示。（請參閱<span class="link" data-move="screenshot">儲存螢幕截圖</span>） |

##### 遊戲中的錯誤提示
遊戲中與伺服器的通訊發生問題時，遊戲畫面中央會顯示提示，不會遮住遊戲本身的錯誤訊息。

| 提示 | 說明 |
| --- | --- |
| 通訊錯誤 | 連線中斷或伺服器沒有回應時顯示。請確認連線後重新整理。 |
| 伺服器回應錯誤 | 伺服器回傳 HTTP 錯誤時顯示。請稍後重新整理。 |
| 伺服器錯誤 | 伺服器拒絕請求（api_result）時顯示。快速重新整理無法恢復，因此重新整理按鈕會自動以完全重新整理執行。 |

- 輕觸提示即可關閉。
- 長按提示會複製代碼、API 與訊息。回報問題時請一併附上。

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/zh-tw/game_notice.webp"  width="840" style="max-width: 100%;" class="mb-3"/>

##### 登出
- 可在設定 → 登入與連接 → 登出中登出。
- 登出後，會重新開啟登入方式的登入頁面。
