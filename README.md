# ⚡ StarGate

一键切换 Android 网络网关配置的工具，专为使用旁路由的用户设计。

## 功能

- **一键切换网关**：在主路由和旁路由之间快速切换，无需手动修改静态 IP
- **DNS 自动切换**：切换网关时同步修改 DNS 配置
- **实时状态显示**：当前 IP、网关、DNS、Wi-Fi 名称、连接速度
- **配置持久化**：保存多组网络配置，支持自定义名称/图标/颜色
- **快捷模板**：内置"内网直连"和"旁路由代理"快捷模板

## 原理

Android 为每个网络接口分配了专属路由表。StarGate 通过 Root 权限修改 `wlan0` 路由表中的默认网关：

```bash
ip route replace default via <网关IP> dev wlan0 table wlan0
```

这只修改内存中的路由规则，**不会修改任何系统文件**，重启或重连 Wi-Fi 后自动恢复。

## 要求

- Android 10+
- **Root 权限**（APatch / Magisk / KernelSU）
- Wi-Fi 网络环境

## 使用场景

家里有主路由（192.168.50.1）和旁路由（192.168.50.3，运行代理），想在"直连"和"走代理"之间快速切换，不用每次去系统设置里手动改静态 IP 配置。

## 技术栈

- Kotlin + Jetpack Compose
- Material 3 暗色主题
- DataStore 数据持久化
- Root Shell 命令执行

## 构建

用 Android Studio 打开项目，连接设备后直接运行即可。

## 截图

（待添加）

## License

MIT
