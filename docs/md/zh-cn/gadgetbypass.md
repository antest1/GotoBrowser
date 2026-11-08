## 海外IP封锁绕行

<div class="alert alert-danger" role="alert">自 2024/12/03 起，海外 IP 封锁已解除，无需再使用此功能。</div>

自 2020 年 2 月起，舰C运营方（C2机关）以 DDoS 攻击等为由，在舰C登录服务器上封锁了海外 IP。  
因此，目前若要在日本以外的国家/地区正常游玩舰C，需要绕过上述封锁。

检测到 IP 封锁时，游戏启动时会显示如下提示。

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/zh-cn/bypass_required.webp"  width="480" style="max-width: 100%;"/>

##### 设置方法

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/zh-cn/gadget_options.webp"  width="600" style="max-width: 100%;" class="mb-3"/>

1. 在设置 → 登录与连接的“登录服务器封锁绕行”部分中，开启“启用登入服务器封锁绕行”选项。
2. 将绕行方式设为“URL替换”，端点服务器设为以下地址即可。（注意不要输错！）
3. 显示“需要重新加载”提示后，点击“现在重新加载”重新加载游戏。

| 端点服务器地址 | 备注 |
| --- | --- |
| `https://kcwiki.github.io/cache/` | 默认值 |

##### 补充说明
- 在绕行开启的状态下，如确认封锁已解除，绕行选项将自动关闭。如需保持开启，请关闭同一部分中的“自动停用绕行”。
- 反之，在绕行关闭的状态下检测到封锁时，会再次显示上述提示。
