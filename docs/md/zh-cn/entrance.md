<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20,400,0,0&icon_names=help,settings" />

## 开始游戏

启动应用后不会显示单独的主界面，而是直接打开登入方式的起始页面（默认：DMM direct）。  
4.0 之前版本主界面中的项目均已移至<span class="link" data-move="settings">设置界面</span>。

##### 启动流程
1. 打开登入方式的起始页面。在登录页面中，会自动填入设置中保存的账号信息。
2. 登录后，游戏画面会按设备屏幕调整显示。（设置 → 游戏画面显示 → 自动调整佈局）
3. 加载游戏期间，游戏画面上会显示加载提示，游戏开始后即消失。（参见下方“加载提示”）
4. 轻触游戏画面外的黑色空白区域即可显示<span class="link" data-move="browserui">工具栏</span>，点击右上角的设置按钮（<span class="material-symbols-outlined inline-icon">settings</span>）可打开设置界面。

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/zh-cn/first_launch.webp"  width="840" style="max-width: 100%;" class="mb-3"/>

##### 加载提示
加载游戏期间，游戏画面中央会显示进度。加载很快完成时不会显示。
- 依次经过 准备 → 连接服务器 → 下载资源，右上角显示当前阶段（1/3～3/3）。
- 下载资源期间会缩小显示在画面下方，以免遮挡游戏自身的加载画面。
- 不会显示在登录页面等普通网页之上。

| 状态 | 说明 | 处理方法 |
| --- | --- | --- |
| 进行中 | 显示当前阶段和进度。 | 请稍候。 |
| 无响应（黄色） | 20 秒以上没有任何响应时显示。 | 使用以黄色突出显示的刷新按钮重试。 |
| 连接错误（红色） | 无法加载页面时显示原因，下方列出错误代码和主机。 | 检查网络连接后按刷新按钮，此时会自动进行完全刷新。 |

- 如果工具栏处于隐藏状态，会自动显示，以便看到刷新按钮。

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/zh-cn/loading_status.webp"  width="840" style="max-width: 100%;" class="mb-3"/>
<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/zh-cn/loading_error.webp"  width="840" style="max-width: 100%;" class="mb-3"/>

##### 选择登入方式
- 一般情况下，无需更改默认值 DMM direct。
- 如需使用基于 OOI 的登入方式（[ooi.moe](https://ooi.moe/)），可在设置 → 登录与连接 → 选择登入方式中更改。
  - 首次选择非官方登入方式时，会显示游戏资源可能与原版不同的提示。
- 更改登入方式后，需重新加载才会生效。

##### 自动登录
- 可在设置 → 登录与连接 → 自动登录中保存要填入登录表单的账号和密码。
- 该信息仅保存在设备内，不会用于自动填写以外的用途。

##### 启动时可能显示的提示
| 提示 | 说明 |
| --- | --- |
| 更新提示 | 有新版本时询问是否下载。（与设置 → 应用程式资讯 → 检查更新相同） |
| 检测到Kcanotify | 已安装 Kcanotify 但“使用广播讯息方式”处于关闭状态时显示。点击“确定”即可开启。（参见<span class="link" data-move="kcanotify">与Kcanotify联动</span>） |
| 检测到外国IP阻止 | 在“启用登入服务器封锁绕行”关闭的状态下检测到海外 IP 封锁时显示。（参见<span class="link" data-move="gadgetbypass">海外IP封锁绕行</span>） |

##### 与旧版本的区别
| 旧版本（主界面） | 4.0 及以后 |
| --- | --- |
| 选择连接站点 | 设置 → 登录与连接 → 选择登入方式 |
| 自动登录 | 设置 → 登录与连接 → 自动登录 |
| 以静音模式启动 / 使用广播讯息方式 / 启用登入服务器封锁绕行 | 设置 → 登录与连接 |
| 启动时显示控制台 / 允许键盘输入 | 设置 → 操作与工具栏 |
| 清除快取 | 设置 → 缓存设定 → 清除快取 |
| 手册网站（<span class="material-symbols-outlined inline-icon">help</span>） | 设置界面顶部的帮助按钮 |
| 设置界面（<span class="material-symbols-outlined inline-icon">settings</span>） | 游戏画面右上角的设置按钮 |
