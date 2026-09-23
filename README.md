# KochMorseTrainer

一款基于 **Koch 学习法** 的摩尔斯电码（CW）听认训练 Android 应用。

## 简介

Koch 法的核心是：先只学极少数几个字符，反复听辨直到完全熟练，再逐步加入新字符，
从而建立稳定的“声音 → 字符”条件反射。本应用内置 Koch 推荐的学习顺序，并提供三种训练模式。

## 功能特性

底部导航包含三个页面，**各页的设置（等级、速度、音调等）完全独立**，切页或旋转屏幕后仍会保留。

### 1. 整串听认

- 从当前等级字符集中随机生成一串字符并播放摩尔斯音频
- 用户用候选按钮逐个拼出听到的内容，支持删除、提交
- 候选按钮**固定为当前等级的全部字符**（不可调）；可调：字符数量（1~10）

### 2. 单字·听音选字

- 自动播放一个随机字符，用户从候选中选出
- 显示对错后自动进入下一题，可手动“重播”
- 可调：候选数量（2~8）

### 3. 单字·选字听音

- 以网格形式列出当前等级的所有字符，点击任意字符即播放其摩尔斯音
- 适合自由练习与音感熟悉

### 通用设置区（每页独立）

| 设置 | 范围 | 说明 |
| --- | --- | --- |
| 等级 | 2 ~ 38 | 决定当前纳入训练的字符集 |
| 字符速度 | 5 ~ 50 WPM | 点划本身的发报速度 |
| 有效速度 | 3 ~ 字符速度 WPM | 字符之间的间隔速度（Koch 常用“快字符、慢间隔”） |
| 音调 | 200 ~ 1600 Hz | 侧音频率 |

## 音频原理

音频由 `MorseAudioGenerator` 生成：

- 将整段文本**一次性合成**为单个 PCM 缓冲（44.1kHz / 16bit / 单声道），点划之间的静音也按采样点精确排布；
- 用**一个** `AudioTrack`（`MODE_STATIC`）播放，避免逐音调创建/销毁音频轨道；
- 每个音调首尾加入约 4ms 的**淡入淡出包络**，消除波形突变的“咔哒”爆音；
- 因此发音时长稳定、节奏精确。

时间单位遵循标准：点 = 1 单位，划 = 3 单位，字符内间隔 = 1 单位，字符间 = 3 单位，词间 = 4 × 有效单位。

## 技术栈

- 语言：Kotlin
- UI：传统 View / XML + `BottomNavigationView` + `Fragment`
- 状态：AndroidX `ViewModel`（Activity 级，跨页/旋转保留）
- 音频：`AudioTrack` 实时合成
- 构建：Gradle Kotlin DSL + 版本目录（`libs.versions.toml`），AGP 9.3.2

## 项目结构

```
app/src/main/
├── java/com/example/kochmorsetrainer/
│   ├── MainActivity.kt            # 承载底部导航 + Fragment 切换，处理系统栏 insets
│   ├── BaseTrainerFragment.kt     # 公共基类：设置区绑定、候选按钮渲染、音频/等级管理
│   ├── SequenceFragment.kt        # 整串听认页
│   ├── SingleListenFragment.kt    # 单字·听音选字页
│   ├── SingleTapFragment.kt       # 单字·选字听音页
│   ├── TrainerViewModel.kt        # 各页独立状态（PageState / SequenceState）
│   ├── KochTrainerManager.kt      # Koch 序列、等级、出题与判分（纯逻辑）
│   └── MorseAudioGenerator.kt     # 摩尔斯音频合成与播放
└── res/
    ├── layout/
    │   ├── activity_main.xml          # 根布局 + 底部导航
    │   ├── view_audio_settings.xml    # 公共设置区（等级 + 音频参数）
    │   ├── fragment_sequence.xml
    │   ├── fragment_single_listen.xml
    │   └── fragment_single_tap.xml
    ├── menu/bottom_nav_menu.xml
    └── drawable/ic_nav_*.xml
```

## 构建与运行

环境要求：Android Studio（或 JDK 17+ 与 Android SDK），Android SDK Platform 37。

```bash
# Debug 构建
./gradlew assembleDebug

# 安装到已连接设备/模拟器
./gradlew installDebug

# 运行单元测试
./gradlew testDebugUnitTest
```

Windows 下使用 `gradlew.bat` 代替 `./gradlew`。

最低支持 Android 7.0（API 24），目标 API 37。

## 生成 Release

项目通过项目根目录的 `keystore.properties`（已被 `.gitignore` 忽略，切勿提交）读取签名信息：

```properties
storeFile=koch-release.jks
storePassword=******
keyAlias=koch
keyPassword=******
```

配置后执行：

```bash
./gradlew assembleRelease
```

产物位于 `app/build/outputs/apk/release/`：

- 已配置签名时生成可直接安装/分发的 `app-release.apk`
- 未配置签名时生成未签名的 `app-release-unsigned.apk`（需自行用 `apksigner` 签名）

## 许可

本项目仅供学习与交流使用。
