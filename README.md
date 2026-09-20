# AutoSkipAd

Android 自动跳过广告应用。检测屏幕右上角的"跳过"按钮并自动点击。

## 使用

1. 从 [Actions](../../actions) 页面下载最新编译的 APK（或本地编译）
2. 安装到手机
3. 打开应用 → 点击"开启无障碍服务" → 在系统设置中启用
4. 完成，广告跳过按钮会被自动点击

## 本地编译

需要 JDK 17 + Android SDK，然后：

```bash
./gradlew assembleDebug
```

APK 输出: `app/build/outputs/apk/debug/app-debug.apk`
