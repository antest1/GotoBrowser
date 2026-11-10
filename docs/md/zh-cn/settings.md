<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20,400,0,0&icon_names=help,settings" />

## 设置 

在显示工具栏的状态下，点击游戏画面右上角的齿轮图标（<span class="material-symbols-outlined inline-icon">settings</span>）即可进入

- 设置界面显示在游戏画面上方，游戏会在后方继续运行。
- 向下拖动顶部的把手或按返回键即可关闭。
- 点击右上角的帮助按钮（<span class="material-symbols-outlined inline-icon">help</span>）可打开本手册。

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/zh-cn/settings_main.webp"  width="560" style="max-width: 100%;" class="mb-3"/>

##### 设置生效方式

| 生效 | 说明 |
| --- | --- |
| 立即 | 更改数值后立即生效。 |
| 重新加载 | 会显示“需要重新加载”提示。点击“现在重新加载”将在完全刷新后生效，点击“暂不重新加载”将在下次刷新时生效。 |

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/zh-cn/settings_refresh_required.webp"  width="480" style="max-width: 100%;" class="mb-3"/>

##### 登录与连接

| 分组 | 名称 | 说明 | 生效 |
| --- | --- | --- | --- |
| 游戏登录 | 自动登录 | 自动填入登录表单的账号和密码。仅保存在设备内 | 重新加载 |
| 游戏登录 | 登出 | 登出游戏并返回登入方式的登录页面 | |
| 浏览器设定 | 选择登入方式 | DMM direct（默认）/ ooi.moe<br/>首次选择非官方登入方式时，会显示有关游戏资源的提示 | 重新加载 |
| 浏览器设定 | 以静音模式启动 | 游戏启动时将 BGM/SE/VOICE 音量设为 0（与官方应用程序的功能相同）<br/>仅适用于 DMM direct | 重新加载 |
| 浏览器设定 | 使用广播讯息方式 | 将舰C相关的通信内容发送给 Kcanotify 等其他应用程序使用（默认开启）<br/>与 Kcanotify 联动时必须开启 | 立即 |
| 登录服务器封锁绕行 | 启用登入服务器封锁绕行 | 出现海外 IP 封锁问题（卡在黑屏）时开启 | 重新加载 |
| 登录服务器封锁绕行 | 绕行方式 | 选择 URL替换 或 KCCacheProxy（远程服务器）（大多数情况下使用“URL替换”） | 重新加载 |
| 登录服务器封锁绕行 | 端点服务器 | 绕行所用的服务器地址<br/>`https://kcwiki.github.io/cache/`（默认） | 重新加载 |
| 登录服务器封锁绕行 | 自动停用绕行 | 未遭封锁而正常连接时，自动关闭绕行设定（默认开启） | 立即 |

绕行相关内容请参阅<span class="link" data-move="gadgetbypass">海外IP封锁绕行</span>页面。“启用登入服务器封锁绕行”关闭时，无法选择绕行方式和端点服务器。

##### 游戏画面显示

| 名称 | 说明 | 生效 |
| --- | --- | --- |
| 固定横屏 | 无论设备方向如何，始终以横屏模式游玩（默认开启） | 立即 |
| 自动调整佈局 | 自动缩放游戏画面以适应设备屏幕大小（推荐，默认开启） | 重新加载 |
| 游戏画面垂直位置 | 竖屏、分屏等游戏上下留有空白时，将游戏居中或置于顶部（默认居中） | 立即 |
| 启用画中画模式 | 返回主屏幕或在游戏画面上双指缩小时，以浮动小窗显示游戏画面 | 立即 |
| [多视窗模式]在分隔条增添间隔 | 使用多窗口功能时添加间隔，使画面不会紧贴分隔条显示 | 立即 |
| 改用旧式渲染器 | 将 WebGL 渲染模式改为 Canvas（若未出现图形问题，建议关闭）<br/>开启后无法使用 Kantai3D | 重新加载 |

PIP/多窗口相关内容请参阅<span class="link" data-move="pipmode">PIP/多窗口</span>页面

##### 操作与工具栏

| 分组 | 名称 | 说明 | 生效 |
| --- | --- | --- | --- |
| 工具栏 | 启动时显示控制台 | 应用启动时以显示工具栏的状态启动（默认开启） | 下次启动 |
| 工具栏 | 无需确认直接刷新 | 点击刷新按钮时不显示确认提示，直接刷新画面 | 立即 |
| 工具栏 | 工具栏显示位置 | 居中（默认）或顶部 · 经典。顶部 · 经典会像 3.x 一样把工具栏靠在屏幕上方，并把刷新按钮放在最上面。横屏时适用 | 立即 |
| 工具栏 | 从边缘滑动显示工具栏 | 从隐藏工具栏的屏幕边缘向内滑动以显示工具栏（默认开启）<br/>关闭后，边缘滑动仅用于系统手势<br/>没有空白区域的画面中，按返回键显示工具栏 | 立即 |
| 游戏操作 | 允许键盘输入 | 是否在游戏画面中使用键盘输入（默认开启） | 立即 |
| 游戏操作 | 输入方式 | 选择游玩时的输入方式（屏幕触摸或使用鼠标） | 重新加载 |

##### 缓存设定

| 名称 | 说明 | 生效 |
| --- | --- | --- |
| 将快取存放至外部储存空间 | 缓存游戏资源时使用外部存储空间而非内部存储空间（推荐）<br/>（可通过电脑访问或编辑图片/语音资源） | 重新加载 |
| 预先载入字体 | 使用应用内置的字体文件，而不从游戏服务器加载字体（推荐，默认开启） | 重新加载 |
| 重新尝试下载游戏资源 | 声音/图片资源下载失败时，显示询问是否重新下载的对话框<br/>可减少因网络不稳定导致的猫袭。不适用于游戏 API 通信 | 立即 |
| 清除快取 | 删除浏览器内的缓存。在图片或声音无法正常加载，或需要切换登录方式时使用<br/>清除后可能需要重新登录<br/>清除完成后会显示重新加载的提示 | 立即 |

##### 字幕设定

| 名称 | 说明 | 生效 |
| --- | --- | --- |
| 字幕语言 | 从 English / 한국어 / 日本語 (KC3改)、简中 / 繁中 (kcwiki) 中选择 | 立即 |
| 字体大小 | 调整画面中语音字幕的大小（12~24，默认 18） | 立即 |
| 下载字幕资料 | 有数据更新时可下载。下载后立即生效 | 立即 |

功能相关内容请参阅<span class="link" data-move="voiceline">游戏内台词字幕</span>页面

##### 第三方MOD

<span class="text-danger">使用第三方 MOD 的风险需自行承担。</span>

| 分组 | 名称 | 说明 | 生效 | 作者 |
| --- | --- | --- | --- | --- |
| 常规 | FPS渲染 Mod | 使用 VSYNC 渲染，比原版游戏更流畅、运行效率更高（支持高刷新率设备） | 重新加载 | [@laplamgor](https://x.com/laplamgor) |
| 常规 | FPS限制 | 从默认（最高）或设备支持的刷新率中选择。降低目标 FPS 可节省电量并减少发热 | 立即 | |
| 常规 | 真正的Critical hit | 移除硬编码的伤害混淆。仅在发生 CL2 时显示 Critical hit | 重新加载 | [@Oradimi](https://x.com/oradimi) |
| Kantai3D 视觉补丁 | Kantai3D v4.0 | 为部分秘书舰添加 3D 效果。使用旧式渲染器时无法选择 | 重新加载 | [@laplamgor](https://x.com/laplamgor) |
| Kantai3D 视觉补丁 | 关于Kantai3D | 打开 [Kantai3D GitHub 页面](https://github.com/laplamgor/kantai3d) | | |
| 语言翻译补丁 | KanColle Language Patch (KCCP) | 为游戏内文本应用翻译补丁 | 重新加载 | [@Oradimi](https://x.com/oradimi) |
| 语言翻译补丁 | 要使用的补丁 | [KanColle English Patch](https://github.com/Oradimi/KanColle-English-Patch-KCCP)（@Oradimi）或 [KanColle Indonesia Patch](https://github.com/SLAVUSworks/KanColle-Indonesia-Patch-KCCP)（@SLAVUSworks） | 重新加载 | |
| 语言翻译补丁 | 下载补丁资料 | 下载或更新所选补丁的数据。完成后需刷新游戏才能生效 | 重新加载 | |
| 语言翻译补丁 | 删除补丁资料 | 在补丁出现意外行为或下载过程中出现问题时使用 | 重新加载 | |
| 语言翻译补丁 | KanColle English/Indonesia Patch关于 | 打开所选补丁的 GitHub 页面 | | |

##### 应用程式资讯

| 名称 | 说明 | 生效 |
| --- | --- | --- |
| 目前版本 | 已安装的应用版本 | |
| 检查更新 | 有新版本时显示下载页面 | |
| 原始码 | 打开 [GitHub 仓库](https://github.com/antest1/GotoBrowser/) | |
| 启用DevTools除错 | 允许在 Chrome 等浏览器中进行[远程调试](https://developer.chrome.com/docs/devtools/remote-debugging?hl=zh-cn)（chrome://inspect） | 立即 |
