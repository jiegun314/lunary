# Lunary 发布准备

## 构建

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest
./gradlew :app:assembleRelease
```

- `minSdk 26`，`targetSdk 37`，`compileSdk 37`。
- Release 使用 R8（`optimization.enable = true`）。

## Release 签名

仓库不提交签名文件或密码。创建 `keystore.properties`（已在 `.gitignore` 中忽略）：

```properties
storeFile=release/lunary-release.jks
storePassword=***
keyAlias=lunary
keyPassword=***
```

`app/build.gradle.kts` 检测到该文件时会自动使用 release 签名；缺少时仍产出 `app-release-unsigned.apk`，不会静默使用调试密钥。CI 中应通过 Secret 注入该文件或环境变量，并确保日志不打印密码。

## Google Play 检查

- [ ] 隐私政策 URL（可用本仓库 `PRIVACY.md` 托管）
- [ ] Data safety 表单：当前版本不收集/共享数据；无网络请求
- [ ] 权限声明：通知、精确闹钟、开机广播用途
- [ ] 内容分级与目标受众
- [ ] targetSdk 满足 Play 当期要求（当前 37；提交前以 Play Console 提示为准）
- [ ] 正式签名与 Play App Signing
- [ ] 应用图标、截图、平板截图
- [ ] 预发布测试轨道

## 当前发布阻塞项

1. Google Drive 备份未接入，需要 Android OAuth 客户端 ID + Drive API 实现（本机 debug SHA-1：8E:3E:83:66:DD:41:B5:38:6B:4E:C5:1F:EB:30:37:3F:75:BB:BD:5C）。
2. Play App Signing / 上传资料（图标、截图、平板截图、预发布轨道）尚未配置；本机 keystore 已生成但不应提交仓库。
3. 备份包上限 60 MB、附件 40 MB，不支持分块/断点续传；大附件备份会拒绝并提示。
4. 仅有 API 37 系统镜像，未做多 API 级别/真机矩阵；性能基线为模拟器单点数据。
5. 锁屏 UI 自动化测试仍缺失；生物识别已在模拟器验证，但未在真实设备验证。
