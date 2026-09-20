# 构建指南

当前 Java 工程依赖单独构建的 native 库。Native 与 JAR 的构建和打包流程近期会集成到 GitHub CI；在此之前，本地运行和构建都需要手动准备 native 库。

## 开发环境运行

1. 按照 native 仓库中的 `docs/build_cn.md` 构建当前平台的 native 库。
2. 在 Java 项目根目录修改 `local.properties`，将 `ysm.native_path` 设置为生成的 native 库的绝对路径。
    - 如果该文件尚不存在，先执行一次下一步的 `runClient`；Gradle 会在首次运行时自动生成该文件，修改后再重新运行。
    - `local.properties` 仅保存本机开发配置，已被 Git 忽略，不会被版本控制追踪。
    - Windows 路径建议使用 `/`，避免 `.properties` 文件将反斜杠解释为转义字符。
3. 在 Java 项目根目录运行客户端：

   ```bash
   ./gradlew runClient
   ```

   Windows PowerShell 使用：

   ```powershell
   .\gradlew.bat runClient
   ```

## 构建 JAR

1. 按照同一份 native 构建指南，构建需要支持的各平台 native 库。
2. 将这些库放入 `src/main/resources/META-INF/native/`。当前运行时使用的资源文件名为：Windows x86_64 的 `ysm.dll`、GNU/Linux x86_64 的 `libysm.so`，以及 Android arm64 的 `libysm-android.so`。Android 构建生成的 `libysm.so` 需要以 `libysm-android.so` 放入该目录。
3. 在 Java 项目根目录运行：

   ```bash
   ./gradlew shadowJar
   ```

   Windows PowerShell 使用：

   ```powershell
   .\gradlew.bat shadowJar
   ```

最终 JAR 位于 `build/libs/`。
