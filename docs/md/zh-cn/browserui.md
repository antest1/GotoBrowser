## 浏览器界面
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@40,400,0,0&icon_names=closed_caption,crop_free,frame_reload,light_mode,photo_camera,refresh,screen_lock_rotation,settings,volume_off" />


<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/zh-cn/game_screen.webp"  width="840" style="max-width: 100%;" class="mb-3"/>

通过游戏画面边缘的工具栏，可以在浏览器中执行刷新、截图等多种功能。  
- 竖屏模式下横向显示在屏幕底部，横屏模式下纵向显示在屏幕左侧。
- 长按按钮可显示按钮名称。

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/zh-cn/game_screen_portrait.webp"  width="300" style="max-width: 100%;" class="mb-3"/>

##### 显示/隐藏工具栏
- 显示工具栏的方法如下。
  - 轻触游戏画面外的黑色空白区域（推荐）
  - 从隐藏工具栏的屏幕边缘（竖屏：底部，横屏：左侧）向内滑动（可在设置 → 操作与工具栏 → “从边缘滑动显示工具栏”中关闭）
  - 游戏画面外没有空白区域时，按返回键（或使用返回手势）显示
  - 在设置 → 操作与工具栏中开启“启动时显示控制台”，使其在应用启动时显示
- 隐藏工具栏的方法如下。
  - 再次轻触游戏画面外的黑色空白区域
  - 将工具栏向屏幕边缘方向滑动

##### 刷新
工具栏旁的刷新按钮提供两种方式。
横屏时，将设置 → 操作与工具栏 → “工具栏显示位置”改为“顶部 · 经典”后，工具栏会像 3.x 一样靠在屏幕上方，刷新按钮位于最上面。

|  | 功能 | 说明 |
| --- | --- | --- |
| <span class="material-symbols-outlined">frame_reload</span> | 快速刷新 | 仅重新加载游戏框架，而不刷新整个页面。<br/>速度快但未必总能成功，若出现网络错误，请使用完全刷新。 |
| <span class="material-symbols-outlined">refresh</span> | 完全刷新 | 重新加载整个页面。<br/>这是最可靠的刷新方式，但比快速刷新耗时更长。 |

- 按住按钮并向菜单展开的方向拖动，可选择另一种方式。工具栏显示位置为“居中”时菜单向上展开，为“顶部 · 经典”时向下展开。最后使用的方式会被记住，作为按钮的默认操作。
- 开启设置 → 操作与工具栏 → “无需确认直接刷新”后，刷新前不再显示确认对话框。
- 更改设置后，即使在“需要重新加载”提示中选择了“暂不重新加载”，使用此按钮刷新后更改的设置也会生效。
- 加载中出现连接错误或游戏中出现服务器错误后，无论保存的方式如何，按钮都会以完全刷新执行，并在按钮旁显示“完全刷新”。保存的方式不会改变。

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/zh-cn/refresh_menu.webp"  width="560" style="max-width: 100%;" class="mb-3"/>

##### 工具栏按钮说明
|  | 功能 | 说明 |
| --- | --- | --- |
| <span class="material-symbols-outlined">volume_off</span>  | 静音 | 将浏览器中正在播放的声音静音。语音文件仍会继续加载。<br/>（适用于只想看字幕而不想听声音等情况） |
| <span class="material-symbols-outlined">photo_camera</span> | 萤幕截图 | 开启或关闭截图模式。开启后，屏幕右上角设置按钮的下方会显示截图按钮。 |
| <span class="material-symbols-outlined">screen_lock_rotation</span> | 自动旋转锁定 | 锁定当前画面方向，无论设备设置如何都不会旋转。 |
| <span class="material-symbols-outlined">light_mode</span> | 保持萤幕开启 | 使屏幕在待机状态下不会变暗。 |
| <span class="material-symbols-outlined">closed_caption</span> | 字幕 | 在屏幕底部显示或隐藏游戏内字幕。轻触显示的字幕，可将其隐藏至下一句台词出现。 |

##### 右上角按钮
|  | 功能 | 说明 |
| --- | --- | --- |
| <span class="material-symbols-outlined">settings</span> | 设置 | 打开<span class="link" data-move="settings">设置界面</span>。游戏会在设置界面后方继续运行。 |
| <span class="material-symbols-outlined">crop_free</span> | 截图 | 保存当前游戏画面。仅在截图模式开启时显示。（参见<span class="link" data-move="screenshot">保存截图</span>） |

##### 游戏中的错误提示
游戏中与服务器通信出现问题时，游戏画面中央会显示提示，不会遮挡游戏自身的错误信息。

| 提示 | 说明 |
| --- | --- |
| 通信错误 | 连接中断或服务器无响应时显示。请检查连接后刷新。 |
| 服务器响应错误 | 服务器返回 HTTP 错误时显示。请稍后刷新。 |
| 服务器错误 | 服务器拒绝请求（api_result）时显示。快速刷新无法恢复，因此刷新按钮会自动以完全刷新执行。 |

- 轻触提示即可关闭。
- 长按提示会复制代码、API 和消息。反馈问题时请一并附上。

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/zh-cn/game_notice.webp"  width="840" style="max-width: 100%;" class="mb-3"/>

##### 登出
- 可在设置 → 登录与连接 → 登出中登出。
- 登出后，会重新打开登入方式的登录页面。
