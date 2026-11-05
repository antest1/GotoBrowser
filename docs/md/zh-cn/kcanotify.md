## 与Kcanotify联动

以下是将 GotoBrowser 与 [Kcanotify](https://github.com/antest1/kcanotify) 配合使用的设置方法。

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/zh-cn/kcanotify_settings.webp"  width="840" style="max-width: 100%;"/>

##### GotoBrowser 设置

- 在设置 → 登录与连接中开启<span class="text-danger">使用广播讯息方式</span>。（默认为开启状态）
  - 已安装 Kcanotify 但该选项处于关闭状态时，应用启动时会显示询问是否开启的提示。
  - 该设置无需重新加载，会立即生效。

##### Kcanotify 设置
- 在设定 → Kcanotify设定中进行如下设置。
  - 选择要执行的应用程式：<span class="text-danger">GotoBrowser</span>
  - Sniffer模式：<span class="text-danger">PASSIVE</span>
- 注意：上述设置仅可在服务关闭的状态下更改。
