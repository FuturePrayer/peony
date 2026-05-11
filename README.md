# peony

`peony` 是一个面向 Java 生态的命令行启动器，目标是提供接近 `npx` 的使用体验：

- 从远程来源解析并下载可运行产物
- 默认在临时工作区内运行并自动清理
- 透传子进程参数与标准输入输出流
- 适合快速启动基于 stdio 的 Java MCP Server 或其他单文件 `.jar` 工具

当前已支持：

- GitHub 仓库最新 release 的 `.jar` 资产
- GitHub 私有仓库 token
- `http`、`https`、`socks5` 代理
- 前缀代理
- GraalVM Native Image 构建

后续可扩展来源：

- Gitee release
- 远程 Maven 仓库
- 其他自定义分发源

## 特性

- 尽量少依赖，目前运行时仅依赖 `jackson-core` 和 `hutool-http`
- 基于 JDK 21 与 Hutool HTTP 实现 HTTP、进程启动和配置加载
- 默认删除下载工作区，避免残留文件
- 支持通过 CLI、配置文件、环境变量组合配置
- 支持 native 可执行构建

## 命令格式

```bash
peony run github <owner/repo> [options] -- [program args]
```

示例：

```bash
peony run github modelcontextprotocol/servers --asset mcp-server.jar -- --stdio
```

```bash
peony run github owner/private-repo --github-token-env MY_GITHUB_TOKEN -- --port 3000
```

```bash
peony run github owner/repo --proxy socks5://127.0.0.1:1080 --prefix-proxy https://gp-proxy.com/ -- --help
```

```bash
peony run github owner/repo --asset app.jar --download-only
```

## CLI 选项

| 选项 | 说明 |
| --- | --- |
| `--asset <name>` | 当最新 release 中存在多个 `.jar` 文件时，指定要下载的资产名 |
| `--config <path>` | 指定配置文件路径 |
| `--java-home <path>` | 指定 JDK 路径 |
| `--workspace <path>` | 指定工作区父目录；程序会在其下创建独立临时子目录 |
| `--keep-workspace` | 运行结束后保留工作区 |
| `--download-only` | 只下载不运行，并保留工作区；程序会输出下载后的 jar 绝对路径 |
| `--proxy <url>` | 指定代理，支持 `http://`、`https://`、`socks5://` |
| `--prefix-proxy <url>` | 指定前缀代理，最终访问 URL 为 `<prefix><real-url>` |
| `--github-token <token>` | 直接指定 GitHub token |
| `--github-token-env <name>` | 指定承载 GitHub token 的环境变量名 |
| `-h`, `--help` | 输出帮助 |

## 配置优先级

大部分配置优先级为：

1. CLI 参数
2. 环境变量
3. 配置文件
4. 默认值

`java.home` 按当前实现的查找顺序为：

1. `--java-home`
2. 配置文件 `java.home`
3. `PEONY_JAVA_HOME`
4. `JAVA_HOME`

如果都没有，则直接报错退出。

## 配置文件

默认搜索顺序：

1. 当前目录下的 `peony.properties`
2. 用户目录下的 `.peony.properties`

也可通过 `--config` 显式指定。

示例：

```properties
java.home=D:/develop/jdk-21
workspace.parent=D:/temp/peony
github.token.env=PEONY_GITHUB_TOKEN
proxy.url=socks5://127.0.0.1:1080
proxy.prefix=https://gp-proxy.com/
```

支持的配置项：

| Key | 说明 |
| --- | --- |
| `java.home` | JDK 根目录 |
| `workspace.parent` | 工作区父目录 |
| `github.token` | GitHub token |
| `github.token.env` | GitHub token 对应环境变量名 |
| `proxy.url` | 普通代理地址 |
| `proxy.prefix` | 前缀代理地址 |

## 环境变量

| 环境变量 | 说明 |
| --- | --- |
| `PEONY_JAVA_HOME` | JDK 根目录 |
| `JAVA_HOME` | JDK 根目录回退 |
| `PEONY_GITHUB_TOKEN` | 默认 GitHub token |
| `GITHUB_TOKEN` | 默认 GitHub token 回退 |
| `PEONY_PROXY` | 默认代理 |
| `HTTPS_PROXY` | 默认代理回退 |
| `HTTP_PROXY` | 默认代理回退 |
| `ALL_PROXY` | 默认代理回退 |
| `PEONY_PREFIX_PROXY` | 默认前缀代理 |
| `PEONY_WORKSPACE_PARENT` | 默认工作区父目录 |

## 工作区与清理策略

- `--workspace` 指的是工作区父目录，不是最终工作目录
- 每次运行会创建一个独立临时子目录，例如 `peony-xxxxx`
- 下载的 `.jar` 文件会落在这个临时子目录里
- 默认退出后会删除整个临时子目录
- 指定 `--download-only` 时只下载不运行，且不会删除工作区
- 指定 `--keep-workspace` 后保留该目录以便排查问题

## 代理说明

### 普通代理

示例：

```bash
--proxy http://127.0.0.1:7890
--proxy https://127.0.0.1:7890
--proxy socks5://127.0.0.1:1080
```

### 前缀代理

如果实际访问地址是：

```text
https://github.com/example/repo
```

配置：

```text
--prefix-proxy https://gp-proxy.com/
```

则最终请求地址为：

```text
https://gp-proxy.com/https://github.com/example/repo
```

该策略对：

- GitHub API 请求
- release 资产下载
- 重定向后的下载请求

都会生效。

## 本地开发

本项目使用：

- JDK 21
- Maven 3.9+

Windows 本地示例：

```powershell
$env:JAVA_HOME="D:\develop\jdk-21"
& "D:\develop\apache-maven-3.9.6\bin\mvn.cmd" "-Dmaven.repo.local=D:/maven/repo" test
```

打包可运行 fat jar：

```powershell
$env:JAVA_HOME="D:\develop\jdk-21"
& "D:\develop\apache-maven-3.9.6\bin\mvn.cmd" "-Dmaven.repo.local=D:/maven/repo" package
```

产物位置：

```text
target/peony-<version>.jar
```

## GraalVM Native Image 构建

项目已在 `native` profile 中配置 `org.graalvm.buildtools:native-maven-plugin`。

### 本地前置条件

1. 安装 GraalVM for JDK 21
2. 安装 `native-image` 组件
3. 使用 GraalVM 作为 `JAVA_HOME`
4. 如果在 Windows 上构建，还需要安装 Visual Studio 2022 C++ Build Tools，或在 Native Tools Command Prompt 中执行构建

你的本地环境示例：

```powershell
$env:JAVA_HOME="D:\develop\graalvm-community-openjdk-21.0.1+12.1"
& "D:\develop\apache-maven-3.9.6\bin\mvn.cmd" "-Dmaven.repo.local=D:/maven/repo" -Pnative native:compile
```

生成产物通常位于：

```text
target/peony.exe
```

说明：

- 普通 `mvn test` 或 `mvn package` 不会加载 native 插件
- 只有显式指定 `-Pnative native:compile` 时才会触发 native 构建
- 当前配置使用 `--no-fallback`
- Windows 本地构建若缺少 `vcvarsall.bat`，说明未安装 Visual Studio C++ 原生工具链

## GitHub Release 自动发布

仓库中已提供 GitHub Actions 工作流，在满足以下条件时自动发布 release：

1. 推送了一个 tag
2. tag 名称与 `pom.xml` 中的 `<version>` 完全一致
3. 该 tag 指向的提交已包含在默认分支 `main` 或 `master` 中

工作流行为：

1. 运行测试
2. 构建 fat jar
3. 构建 GraalVM native 可执行文件
4. 自动创建或更新 GitHub Release
5. 上传 jar、Linux native 和 Windows native 二进制

当前工作流运行在 `ubuntu-latest`，因此会发布：

- 跨平台 fat jar
- Linux x86_64 native 可执行文件
- Windows x86_64 native 可执行文件

Pre-release 判定规则：

- 如果版本号包含 `SNAPSHOT`、`alpha`、`beta`、`rc`、`milestone`、`preview`，则标记为 Pre-release
- 否则发布为正式 release

推荐发版流程：

1. 修改 `pom.xml` 中的 `<version>`
2. 提交并合并到 `main` 或 `master`
3. 创建同名 tag，例如 `1.0` 或 `1.1.0-SNAPSHOT`
4. push tag

示例：

```bash
git tag 1.0
git push origin 1.0
```

```bash
git tag 1.1.0-SNAPSHOT
git push origin 1.1.0-SNAPSHOT
```

## 当前限制

- 目前只实现了 `github` source
- `http/https/socks5` 代理通过 Hutool HTTP 统一处理
- 目前尚未做下载缓存

## 后续计划

- 增加 Gitee 来源
- 增加 Maven 仓库来源
- 增加缓存与校验策略
- 增加更多端到端集成测试
