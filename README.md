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
- 设置页提供按豆包 1.4.5 真实 26 键比例绘制的实时预览
- 混色滑杆支持刻度和直接输入百分比数值
- 支持一键应用设置并重启豆包输入法；ColorOS 上会自动切回豆包，避免强停后回退到系统输入法
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
- `com.bytedance.common_biz.tool_box.ToolboxKeyboardView` 是否仍存在
- Android 资源 `navigation_bar_normal`、`candidate_item_text_highlighted` 是否仍存在

## 设置项

### 外观

- **启用 Monet 配色**：关闭后需重启豆包输入法进程以完全恢复原始资源
- **底部区域跟随键盘**：同步候选栏、键盘主体和系统导航区域
- **扩展面板跟随键盘**：覆盖工具面板、顶部工具栏、更多候选等原始灰色区域

### 混色强度

分别提供滑杆调整：

- 键盘底板
- 普通字母键
- 功能键
- 候选 / 面板高亮
- 按下态

滑杆拖动时实时预览立即变化；松手或输入数字后写入设置。

设置页同时提供：

- 0 / 中间值 / 最大值刻度
- 可直接输入百分比的数字框
- 按豆包 1.4.5 `input_kbd_pinyin26.xml` / `style.xml` 比例绘制的候选态键盘预览
- **应用并重启豆包输入法**：root 环境下一键强停并重新设回豆包输入法，让实际键盘立即读取新配置

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

模块在 `getAssetsMgr()` 返回后追加一个只包含覆盖颜色文件的小 ZIP。Android AssetManager 会优先读取后追加路径中的同名 asset，因此无需修改豆包 APK。

豆包还有一部分颜色来自 Android `res/color` / drawable，例如首候选固定蓝、键盘底部背景和候选高亮背景；这些通过针对性的运行时资源拦截映射到 Monet palette。0.3.2 起模块会在进程启动时把目标资源 ID 映射到内部角色，后续颜色读取不再反复解析资源名；Android Monet palette 也只在输入法生命周期刷新时重新读取。

工具面板等区域在 XML inflate 时已把 `navigation_bar_normal` 解析成 Drawable，单纯拦截 `getColor()` 无法覆盖。因此模块还会在 `ToolboxKeyboardView` / `InputViewRoot` 的 View 子树中，仅替换已知豆包原始灰色背景，避免全局改色影响其它 UI。

## 致谢

设计思路参考了 [0x1e93d/WeType_Monet](https://github.com/0x1e93d/WeType_Monet)。WeType_Monet 主要使用 Android RRO；豆包键盘主体使用自定义 asset 皮肤，因此本项目采用 AssetManager 注入 + Android 资源拦截 + 局部 View 背景处理的组合方式。

## 免责声明

本项目与字节跳动、豆包输入法没有关联。仅用于研究 Android 主题和输入法 UI 资源机制。

## License

MIT
