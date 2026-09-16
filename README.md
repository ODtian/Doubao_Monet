# Doubao Monet

为 Android 版豆包输入法提供 Material You / Monet 动态配色的 Xposed 模块。

当前实现不修改、不重签豆包输入法本体，而是在运行时覆盖豆包自己的键盘皮肤颜色，并同步 Android 侧候选栏、键盘底板和导航区颜色。

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
- 普通字母键、功能键、按下态使用不同 Material 容器层级
- 首候选固定 `#4F84FF` 改为系统 Monet primary
- 中文首候选拼音高亮同步 Monet primary
- 搜索 / 确定 / 换行键默认统一为功能键容器色
- 可选突出搜索 / 确定 action 键
- 模块自带设置界面

## 默认颜色映射

| 豆包元素 | Material / Monet token |
| --- | --- |
| 键盘主体 / 候选栏 | `system_surface_container_*` |
| 普通字母键 | `system_surface_container_lowest_light` / `system_surface_container_highest_dark` |
| 功能键 | `system_secondary_container_*` |
| 功能键按下态 | `system_primary_container_*` |
| 主文字 | `system_on_surface_*` |
| 次文字 / 拼音 | `system_on_surface_variant_*` |
| 首候选强调文字 | `system_primary_*` |
| 首候选选中背景 | `system_primary_container_*` |
| 分隔线 | `system_outline_variant_*` |
| 底部导航区 | 与键盘主体相同的 surface |

## 安装

1. 安装 Actions 或 Release 生成的 APK。
2. 在 Vector / LSPosed 中启用 `Doubao Monet`。
3. 作用域只勾选 `com.bytedance.android.doubaoime`。
4. 强行停止一次豆包输入法进程，重新唤起键盘。
5. 从桌面打开 `Doubao Monet` 可调整颜色策略。

如果豆包更新后模块失效，建议先确认：

- `com.bytedance.android.input.keyboard.KeyboardView#getAssetsMgr()` 是否仍存在
- `assets/skin/default/values/colors.xml` / `dark_colors.xml` 是否仍存在
- Android 资源 `navigation_bar_normal`、`candidate_item_text_highlighted` 是否仍存在

## 设置项

- **启用模块**：关闭后需重启豆包输入法进程以完全恢复原始资源
- **统一键盘与底部导航区**：同步候选栏、键盘主体、系统导航区域
- **普通字母键轻微染色**：让普通键帽也带一点壁纸色
- **首候选使用 Monet 强调色**：替换豆包固定蓝 `#4F84FF`
- **突出搜索 / 确定键**：打开后 action 键使用 primary；关闭时与换行等功能键统一

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

豆包还有一部分颜色来自 Android `res/color`，例如首候选的固定蓝和键盘底部背景；这些通过针对性的运行时资源读取拦截映射到 Monet palette。

## 致谢

设计思路参考了 [0x1e93d/WeType_Monet](https://github.com/0x1e93d/WeType_Monet)。WeType_Monet 主要使用 Android RRO；豆包键盘主体使用自定义 asset 皮肤，因此本项目采用 AssetManager 注入 + Android 资源拦截的组合方式。

## 免责声明

本项目与字节跳动、豆包输入法没有关联。仅用于研究 Android 主题和输入法 UI 资源机制。

## License

MIT
