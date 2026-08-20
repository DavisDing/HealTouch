# HealTouch 项目环境与安装记录

> 本文件用于记录 HealTouch 项目开发、构建、测试、打包过程中涉及的本机软件、项目依赖、CI 工具、插件和项目外文件。
>
> **归因说明**：Git 和 Homebrew 当前状态只能证明软件“现在存在”以及项目“需要什么”，不能完全证明某个软件一定是在本项目期间安装的。因此本文件区分“已确认”“高度相关”“可能相关”“未确认”，项目结束时按此状态审计，避免误删其他项目正在使用的工具。

## 1. 本机全局软件 / Homebrew 包

| 状态 | 软件 | 版本 | 安装方式 | 本机位置 / 当前用途 | 与本项目关系 | 项目结束时处理 | 卸载方式 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 高度相关 | Apache Maven | 3.9.16 | Homebrew | `/opt/homebrew/Cellar/maven/3.9.16`；执行 Maven 构建、测试、打包 | 项目使用 `pom.xml` 管理 Java 构建；README 要求 Maven 3.9.16 | 先确认没有其他项目使用，再决定卸载 | `brew uninstall maven` |
| 可能相关 / 用户主动安装 | OpenJDK 21 | 21.0.12.1 | Homebrew | `/opt/homebrew/Cellar/openjdk@21/21.0.12.1`；Java 开发运行时候选 | 项目目标为 Java 8，当前项目配置没有直接锁定 JDK 21；可能是为本项目开发环境安装，也可能供其他项目使用 | 不得默认删除；确认其他项目无依赖后再决定 | `brew uninstall openjdk@21` |
| 间接相关 | OpenJDK | 26.0.2 | Homebrew 依赖安装 | `/opt/homebrew/Cellar/openjdk/26.0.2`；当前 Maven 依赖它运行 | Homebrew 显示它是 `maven` 的依赖；不是本项目单独直接声明的依赖 | 只有在 Maven 卸载后且无其他使用者时，才评估是否删除 | `brew uninstall openjdk`（执行前必须重新检查依赖） |
| 未确认属于本项目 | SQLite | 3.53.4 | Homebrew 依赖安装 | `/opt/homebrew/Cellar/sqlite/3.53.4`；为 `python@3.14` 依赖 | 项目运行时使用 Maven 的 `org.xerial:sqlite-jdbc:3.36.0.3`，不要求本机 SQLite 命令行工具 | 默认保留；不得因 HealTouch 单独删除 | `brew uninstall sqlite`（仅在确认 Python 和其他项目均不依赖时） |
| 未确认属于本项目 | Python | 3.14.7 | Homebrew，用户主动安装 | `/opt/homebrew/Cellar/python@3.14/3.14.7` | 项目是 Java/Maven 项目，没有 Python 配置、虚拟环境或 Python 构建脚本 | 默认保留 | `brew uninstall python@3.14`（仅在确认无其他用途时） |

### 当前 Homebrew 依赖关系

- `maven` 依赖 `openjdk`。
- `python@3.14` 依赖 `sqlite`。
- 因此不能仅凭“项目用过 Maven”就直接删除 `openjdk`；也不能仅凭项目使用 SQLite 数据库就删除系统 `sqlite`。
- Homebrew 未提供足够的历史安装日志来证明上述每个包的准确安装日期和安装原因。

## 2. 项目声明的 Maven 依赖（项目级，不是全局安装）

这些依赖由 `/Users/dinghao/Downloads/HealTouch/pom.xml` 声明，正常情况下由 Maven 下载到 Maven 本地仓库或 CI Runner 缓存中。它们不应通过 Homebrew 单独卸载。

| 依赖 | 版本 | 用途 | 是否项目级 | 清理方式 |
| --- | --- | --- | --- | --- |
| `org.xerial:sqlite-jdbc` | 3.36.0.3 | SQLite JDBC 驱动 | 是 | 删除本项目对应 Maven 缓存坐标前，先确认没有其他项目使用 |
| `com.zaxxer:HikariCP` | 3.4.5 | 数据库连接池 | 是 | 同上 |
| `org.flywaydb:flyway-core` | 6.5.7 | 数据库迁移 | 是 | 同上 |
| `org.mindrot:jbcrypt` | 0.4 | 密码哈希 | 是 | 同上 |
| `org.slf4j:slf4j-simple` | 1.7.36 | 日志实现 | 是 | 同上 |
| `junit:junit` | 4.13.2 | 测试 | 是 | 同上 |

### 项目声明的 Maven 构建插件

| 插件 | 版本 | 用途 |
| --- | --- | --- |
| `maven-compiler-plugin` | 3.8.1 | Java 编译 |
| `maven-shade-plugin` | 3.2.4 | 生成可执行 shaded/fat JAR |
| `maven-surefire-plugin` | 2.22.2 | 执行测试 |
| `maven-dependency-plugin` | 3.6.1 | CI 预解析项目依赖与构建插件，在编译前暴露依赖解析问题 |

## 3. CI / Windows 打包环境（未安装到本机）

以下工具由 `.github/workflows/build.yml` 和 `packaging/` 配置使用，目标是 GitHub-hosted Windows Runner；当前没有证据表明它们安装在本机 Mac 上。

| 工具 / 内容 | 版本 | 使用位置 | 用途 | 本机状态 |
| --- | --- | --- | --- | --- |
| BellSoft Liberica JDK + FX / Java 8 | 工作流分别取得 x64 与 x86 的 Java 8 `jdk+fx` 包 | GitHub Actions Windows Runner 临时工具缓存 | 编译 JavaFX 项目，并从各 JDK 的 `jre/` 准备 x64/x86 安装包运行时 | 未发现本机对应 Java 8 / JavaFX 安装记录 |
| Apache Maven | 3.9.16 | GitHub Actions Windows Runner | CI 构建、测试、打包 | 本机另有 Homebrew Maven，见第 1 节 |
| Launch4j | 3.14 | GitHub Actions 通过 Chocolatey 安装（当前公开源可用的固定版本） | 将 JAR 包装为 Windows `.exe` | 未发现本机安装 |
| Inno Setup | Runner 预装版本（当前日志显示为 6.7.1）；缺失时通过 Chocolatey 安装 | GitHub-hosted Windows Runner；工作流仅在 `iscc` 命令缺失时安装 | 生成 Windows 安装程序 | 未发现本机安装 |
| JavaFX 8 x86/x64 Runtime | 来自对应架构 Liberica JDK 8 `jdk+fx` 的内置 `jre/` | Runner 临时目录 / 构建时 `runtime/` | 随 Windows 安装包提供运行时；工作流验证 JavaFX 文件与位数 | 未发现本机安装 |

## 4. Codex 插件 / 技能

- 当前没有发现为 HealTouch 单独安装的第三方 Codex 插件。
- 项目使用的 `spreadsheets`、`documents`、`presentations`、`browser` 等属于 Codex 可用技能/运行时能力，不是写入 HealTouch 项目的本机软件安装，也不应在项目结束时卸载。
- 用户提供的“推荐但未安装”插件列表中的插件，不计入本项目已安装内容；除非后续明确安装并在本文件登记。

## 5. 项目外文件与构建产物

| 路径 | 内容 | 当前状态 / 处理建议 |
| --- | --- | --- |
| `/Users/dinghao/Downloads/HealTouch/target/` | Maven 编译、测试和打包输出 | 可在项目结束并确认不需要构建产物后删除 |
| `/Users/dinghao/.m2/repository/` | Maven 本地依赖缓存 | 当前检查时未发现该目录；如果后续构建生成，只清理可确认属于本项目且未被其他项目使用的坐标 |
| `/Users/dinghao/HealTouch/healtouch.db` | 程序首次运行时可能创建的 SQLite 业务数据库 | 当前未发现；如后续出现，可能包含业务数据，必须先确认再删除 |
| `/Users/dinghao/HealTouch/` 下的日志、备份、导出文件 | 运行数据 | 不得默认删除，先确认是否需要保留 |
| `/private/tmp/healtouch-actions-32227961421/` | GitHub Actions 失败作业日志的临时下载目录；GitHub API 返回 403，未取得日志文件 | 可在项目结束时清理；不含源代码或业务数据 |
| `/private/tmp/healtouch-local-compile.log` | 本地 Maven 验证的失败输出（受限网络下的依赖解析错误） | 可在项目结束时清理；仅用于排查记录 |

## 6. 已执行的环境操作记录

| 日期 | 操作 | 结果 |
| --- | --- | --- |
| 2026-08-20 | 配置 GitHub Actions 自动版本与 Release，并验证版本计算逻辑 | 修改 `.github/workflows/build.yml`：每次 `main` 提交自动递增第二位（次版本号）并在打包成功后创建标签和 GitHub Release；手动 `workflow_dispatch` 选择 `major` 时第一位加一并重置后两位。使用 Ruby 标准库解析 YAML；在 `/private/tmp/healtouch-version-test.*` 创建临时 Git 仓库和 Bash 脚本，验证首个版本 `1.0.0`、次版本递增 `1.10.0`、主版本递增 `2.0.0`，命令退出时已清理该临时目录。未安装、升级、下载或卸载任何软件。 |
| 2026-08-18 | 克隆 HealTouch Git 仓库 | 项目源代码位于 `/Users/dinghao/Downloads/HealTouch` |
| 2026-08-19 | 检查本机 Homebrew、项目配置和缓存位置 | 确认第 1 节所列包当前存在；未执行安装、升级、卸载操作 |
| 2026-08-19 | 创建 / 更新本记录文件 | 本文件 |
| 2026-08-19 | 更新 GitHub Actions Java 配置为 BellSoft Liberica JDK 8 `jdk+fx` | CI Runner 临时下载带 JavaFX 的 JDK 8；移除无法解析的 `org.openjfx:javafx-controls:8.0.202` Maven 依赖；未在本机安装或下载 |
| 2026-08-19 | 增加 CI 构建前自检 | 使用 `javap` 检查 JavaFX 类是否存在，并通过 `maven-dependency-plugin:3.6.1:go-offline` 预解析 Maven 依赖和构建插件；仅在 GitHub-hosted Runner 的临时环境下载缓存，未在本机安装或下载 |
| 2026-08-20 | 查询并修正 GitHub Actions Windows 打包工具配置 | 查询 Chocolatey 公共 V2 源：`launch4j` 当前可安装的最新版是 `3.14`，不存在配置中的 `3.50`；GitHub Runner 日志显示已预装 Inno Setup `6.7.1`，因此移除会触发降级失败的固定安装。工作流改为安装 Launch4j 3.14、按需安装 Inno Setup，并在打包前检查 `launch4jc` / `iscc` 命令；未在本机安装、升级或下载软件。 |
| 2026-08-20 | 修复 GitHub Actions JavaFX Runtime 准备步骤卡死 | 运行 `32322862463` 的 `Prepare and verify bundled JavaFX 8 runtimes` 从 09:59（中国标准时间）开始未结束。根因是工作流用 `ProcessStartInfo` 先同步读取 Java 的 stdout、再读取 stderr；`java -XshowSettings:properties -version` 将大量属性写入 stderr，stderr 管道缓冲区写满后子进程阻塞，而父进程又在等待 stdout 关闭，形成死锁。改为并发 `ReadToEndAsync()` 消耗两个流，并增加 60 秒超时、终止子进程和明确报错；未安装、升级、下载或卸载任何软件，也未生成项目外文件。 |
| 2026-08-20 | 修复 Launch4j 原生启动器找不到 Java 8 | GitHub Actions 的 x86 安装包步骤显示 `launch4j: This application requires a Java Runtime Environment 1.8.0`。这不是生成的 HealTouch 程序在找运行时，而是 Launch4j 3.14 的原生 `launch4jc.exe` 未能通过传统 Windows JRE 发现路径定位 `actions/setup-java` 提供的 JDK。工作流现显式定位 `launch4j.jar`，打包脚本用已保存的 x64 Liberica Java 8 `java.exe -jar launch4j.jar <config>` 运行，绕过原生启动器的 JRE/注册表检测；未在本机安装、升级、下载或卸载软件，也未生成项目外文件。 |
| 2026-08-20 | 检查 Chocolatey Launch4j 包安装布局并修正命令路径 | 临时下载 `launch4j.3.14.nupkg` 至 `/private/tmp/healtouch-launch4j-choco-inspect/launch4j.3.14.nupkg` 进行只读检查，确认该包调用原始安装程序且不创建 Chocolatey 命令行 shim。工作流曾据此显式定位 `launch4jc.exe`；后续发现其原生 JRE 查找机制不兼容 `actions/setup-java`，已由本表中“修复 Launch4j 原生启动器找不到 Java 8”记录替换为定位并运行 `launch4j.jar`。临时目录可在项目结束时清理；未在本机安装、升级或卸载软件。 |
| 2026-08-20 | 移除 JavaFX Runtime 下载 Secrets 依赖 | CI 已成功取得 Liberica JDK 8 `jdk+fx`；工作流改为保留 x64 JDK，并额外通过 `actions/setup-java@v5` 取得 x86 JDK，从两个 JDK 内置的 `jre/` 复制运行时。复制前检查 `jfxrt.jar`、`java.exe` 及 32/64 位属性，因此不再需要 `HEALTOUCH_RUNTIME_X86_*` / `HEALTOUCH_RUNTIME_X64_*` Secrets。仅发生在 GitHub-hosted Runner 临时环境，未在本机安装或下载软件。 |
| 2026-08-20 | 修复 Java 运行时位数检查的 PowerShell stderr 处理 | `java -XshowSettings:properties -version` 将属性写入 stderr；PowerShell 在 `$ErrorActionPreference = Stop` 且使用 `2>&1` 时，将正常的 stderr 输出提升为 `NativeCommandError`。工作流改用 `.NET ProcessStartInfo` 分别捕获 stdout/stderr，并继续验证 `sun.arch.data.model` 的 32/64 位值。未安装、升级、卸载或下载软件。 |
| 2026-08-19 | 排查 GitHub Actions 打包失败并进行本地验证 | 只读查询公开 GitHub Actions API，确认运行 `32227961421` 在 `Verify runner Maven 3.9.16` 失败；该固定补丁版本检查已改为仅要求 Maven 3.x。本地曾使用 `mvn --batch-mode --errors -Dmaven.repo.local=target/maven-repository clean verify` 临时下载项目级 Maven 依赖到项目内 `target/`，未全局安装；因本机受限网络的后续重试失败，输出保存到 `/private/tmp/healtouch-local-compile.log`。另建 `/private/tmp/healtouch-actions-32227961421/` 以尝试下载公开作业日志，但 API 返回 403；两处临时文件均可在项目结束时清理。 |
| 2026-08-19 | 修复 GitHub Actions Maven 版本检查退出码异常 | GitHub Windows Runner 中 `mvn --version | Select-Object -First 1` 仅输出版本后仍以退出码 1 结束；改为先完整捕获 `mvn --version` 输出和退出码，再读取第一行验证 Maven 3.x，以避免 PowerShell 提前关闭原生命令管道。未安装、升级或卸载任何软件；未生成项目外文件。 |

## 7. 项目结束清理规则

1. 先重新扫描并列出项目目录、运行数据目录、Maven 缓存和 Homebrew 包。
2. 先删除或确认项目构建产物；不删除源代码、Git 历史、真实业务数据、备份或导出文件。
3. 对 Maven、OpenJDK 21、OpenJDK、SQLite、Python 等全局软件，逐项检查其他项目依赖。
4. 只有在用户明确确认后，才执行 Homebrew 卸载。
5. 卸载前记录精确命令、影响和依赖关系；卸载后更新本文件。
