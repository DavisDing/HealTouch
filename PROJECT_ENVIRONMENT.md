# 项目环境与安装记录

本文件记录为 HealTouch 构建、测试和打包所需的环境变更与外部依赖。

| 日期 | 命令 / 方式 | 软件或依赖 | 版本 | 位置 | 用途 | 是否全局安装 | 是否可清理 / 卸载方式 | 可能被其他项目使用 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 2026-08-19 | Maven `pom.xml` 依赖声明；由 Maven 在 CI 缓存中解析 | `org.openjfx:javafx-controls` | 8.0.202 | Maven 本地仓库（CI Runner：`~/.m2/repository`）；不写入本机全局环境 | 让不含 JavaFX 的 Temurin JDK 8 在 CI 中编译 JavaFX 源码 | 否 | CI Runner 为临时环境，运行结束自动清理；本项目未在本机下载或安装 | 否（仅本项目构建配置使用） |
| 2026-08-19 | GitHub Actions `windows-latest` 预装工具，工作流只做版本验证 | Apache Maven | 3.9.16 | GitHub-hosted Windows Runner | 执行 CI 编译、测试与打包 | 否；未在本机安装 | GitHub-hosted Runner 为临时环境，运行结束自动清理 | 可能被同一 CI Runner 的其他任务预装，但本项目不管理其卸载 |
| 2026-08-19 | GitHub Actions Secrets 指定受控 HTTPS URL，工作流下载并校验 SHA-256 | Java 8 Runtime（含 JavaFX）x86 / x64 | 由受控 Runtime 压缩包版本确定 | GitHub-hosted Windows Runner 的临时目录与项目 `runtime/` 临时构建目录 | 生成可独立运行的 Windows 安装包 | 否；未在本机安装 | GitHub-hosted Runner 为临时环境，运行结束自动清理；构建产物不提交 Git | 否（仅本项目安装包构建使用） |

## 本机状态

截至 2026-08-19，未因本项目在本机全局安装、升级或卸载 Java、Maven、Chocolatey、Launch4j、Inno Setup 或 JavaFX Runtime。
