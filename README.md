# Doubao Monet

为 Android 版豆包输入法提供 Material You / Monet 动态配色的 Xposed 模块。

当前实现不修改、不重签豆包输入法本体，而是在运行时覆盖豆包自己的键盘皮肤颜色，并同步 Android 侧候选栏、工具面板、键盘底板和导航区颜色。

## 当前状态

已在以下环境验证：

- 豆包输入法 1.4.5
- Android 16
- OnePlus Ace 3V
- Vector 2.2（Xposed-compatible framework）

理论上也可用于支持传统 Xposed API 的 LSPosed 类框架；不同豆包版本若调整类名或皮肤结构，可能需要重新适配。

## 功能

- 直接读取 Android framework 的 Monet 动态色，而不是自行计算壁纸色
- 浅色 / 深色模式独立映射
- 键盘主体、候选栏和底部导航区统一 surface
- 覆盖豆包工具面板、顶部工具栏、更多候选等独立背景，避免切换面板后回到原始灰色
- 普通字母键、功能键、候选高亮、按下态的 Monet 混色强度可独立调节
- 键盘底板也可单独调节 Monet 染色强度
- 首候选固定 `#4F84FF` 改为系统 Monet primary
- 中文首候选拼音高亮同步 Monet primary
- 搜索 / 确定 / 换行键默认统一为功能键层级，可选突出 action 键
- 模块自带设置界面
- 设置页使用豆包 1.4.5 真机键盘模板做实时预览，保留真实键位、字体、图标与间距
- 混色滑杆支持刻度和直接输入百分比数值
- 支持把设置直接热应用到当前键盘，不结束豆包输入法进程，也不切换默认输入法
- 颜色资源 ID、系统 Monet palette 与应用 Context 均做进程内缓存，降低输入热路径开销

## 默认配色策略

混色均以当前系统 Monet primary 为染色来源，0% 表示只使用对应 Material surface。

| 元素 | 默认值 |
| --- | ---: |
| 键盘底板 | 0% |
| 普通字母键 | 2% |
| 功能键 | 6% |
| 候选 / 面板高亮 | 5% |
| 按下态 | 12% |
| 按键阴影 | 0% |

文字与分隔线仍直接使用系统 Material token：

- 主文字：`system_on_surface_*`
- 次文字 / 拼音：`system_on_surface_variant_*`
- 首候选强调文字：`system_primary_*`
- 分隔线：`system_outline_variant_*`

## 安装

1. 安装 Actions 或 Release 生成的 APK。
2. 在 Vector / LSPosed 中启用 `Doubao Monet`。
3. 作用域只勾选 `com.bytedance.android.doubaoime`。
4. 强行停止一次豆包输入法进程，重新唤起键盘。
5. 从桌面打开 `Doubao Monet` 调整颜色策略。

如果豆包更新后模块失效，建议先确认：

- `com.bytedance.android.input.keyboard.KeyboardView#getAssetsMgr()` 是否仍存在
- `assets/skin/default/values/colors.xml` / `dark_colors.xml` 是否仍存在
- Android 资源 `navigation_bar_normal`、`candidate_item_text_highlighted` 是否仍存在

## 设置项

### 外观

- **启用 Monet 配色**：支持同进程开启 / 关闭；关闭后恢复豆包原始皮肤资源
- **扩展面板跟随键盘**：覆盖工具面板、顶部工具栏、更多候选等原始灰色区域

### 混色强度

分别提供滑杆调整：

- 键盘底板
- 普通字母键
- 功能键
- 候选 / 面板高亮
- 按下态

所有混色项统一为 0–100%。滑杆拖动时预览立即变化；松手或输入数字后写入设置。

设置页同时提供：

- 0 / 50 / 100 刻度
- 可直接输入百分比的数字框
- 按键阴影 0–100% 调节
- 普通、拼音输入中、工具面板、语音四种预览状态
- 预览基于真机键盘截图拆分出的前景和颜色蒙版，键位、文字、图标和几何位置直接沿用真机结果
- 预览与实际键盘共用同一套 Monet 取色函数
- **应用到当前键盘**：把整组设置直接发送给豆包进程，并热刷新 native 皮肤与 Android 资源，不结束输入法进程

### 候选与动作键

- **首候选使用 Monet 强调色**：替换豆包固定蓝 `#4F84FF`
- **突出搜索 / 确定键**：开启后 action 键使用更明显的 primary；关闭时与其它功能键统一

## 构建

### Windows

需要 JDK 17、Android SDK Platform 35、Build Tools 35.0.0：

```powershell
./build.ps1
```

### Linux / GitHub Actions

```bash
bash scripts/build.sh
```

构建产物：

```text
build/out/Doubao_Monet.apk
```

如果提供以下环境变量，脚本会使用指定 keystore 签名；否则会临时生成调试签名：

```text
KEYSTORE_PATH
KEYSTORE_PASSWORD
KEY_ALIAS
KEY_PASSWORD
```

## 实现思路

豆包 1.4.5 的主要键盘皮肤位于：

```text
assets/skin/default/
├── style.xml
└── values/
    ├── colors.xml
    ├── dark_colors.xml
    └── transparent_colors.xml
```

`libkeyboard.so` 通过 `KeyboardView.getAssetsMgr()` 获取 Java `AssetManager`，随后由 `libime_ui_android_platform.so` 的 Android assets 实现读取皮肤 XML。

模块为当前配色生成一个只包含覆盖颜色文件的小 ZIP，并在 `getAssetsMgr()` 路径中把它加入豆包正在使用的 `AssetManager`。设置变化时会替换这份皮肤资源，并调用豆包自己的颜色方案刷新入口，让 native 键盘在同一进程中重新读取颜色表，因此无需修改豆包 APK。

豆包另一部分 UI 来自 Android `res/color` / drawable，例如首候选固定蓝、键盘底部、工具箱、剪贴板、语音条和更多候选。模块统一处理 `Resources` 与 `TypedArray` 的颜色 / drawable 读取，把目标资源 ID 映射到 Monet 角色，因此布局 XML 在 inflate 时就能拿到正确背景。

设置值由模块应用保存。豆包进程启动时读取最新配置；点击“应用到当前键盘”时，设置页会把整组数值直接发给当前豆包进程，豆包侧保存一份配置并立即刷新当前键盘，避免跨进程偏好文件刷新不及时造成的旧值问题。

XML 中的 `android:background="@color/..."`、`setBackgroundResource(colorId)`、selector、圆角卡片等都在资源加载时直接得到替换后的颜色或 drawable。工具箱、剪贴板和语音状态不再依赖 View 挂载后的二次补色，也不会通过递归扫描 View 树修正背景。

## 致谢

设计思路参考了 [0x1e93d/WeType_Monet](https://github.com/0x1e93d/WeType_Monet)。WeType_Monet 主要使用 Android RRO；豆包键盘主体使用自定义 asset 皮肤，因此本项目采用 AssetManager 注入 + Android 资源映射的组合方式。

## 免责声明

本项目与字节跳动、豆包输入法没有关联。仅用于研究 Android 主题和输入法 UI 资源机制。

## License

MIT
