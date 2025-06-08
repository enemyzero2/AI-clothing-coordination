# Asterisk SIP服务器使用指南

## 简介

本指南提供了基于Asterisk的SIP服务器的部署和使用方法。Asterisk是一个功能丰富的开源通信服务器，可用于语音、视频和消息通信。它易于配置和使用，特别适合教学和演示场景。

## 文件结构

```
├── asterisk/                     - Asterisk配置文件目录
│   ├── sip.conf                  - SIP协议配置
│   ├── extensions.conf           - 拨号计划配置
│   └── users.conf                - 用户配置
├── certs/                        - TLS证书目录
│   ├── asterisk.pem              - Asterisk服务器证书和私钥
│   ├── ca.crt                    - CA证书
│   └── ...
├── asterisk-docker-compose.yml   - Docker部署配置
├── generate_asterisk_cert.ps1    - 证书生成脚本
├── start-asterisk.ps1            - 启动脚本
└── stop-asterisk.ps1             - 停止脚本
```

## 快速启动

1. 确保您已安装Docker和Docker Compose
2. 运行启动脚本：
   ```powershell
   .\start-asterisk.ps1
   ```
3. 如果首次运行，脚本会检测并引导您生成TLS证书
4. 服务器将在几秒钟内启动完成

## 功能特点

### 1. 用户账号

系统预配置了三个用户：
- **用户1**: user1/password1（基础语音通话）
- **用户2**: user2/password2（基础语音通话）
- **技术支持**: support/support123（支持视频通话）

### 2. 特殊功能号码

- **999**: 回音测试 - 测试语音质量
- **1000**: 语音演示 - 播放欢迎消息
- **2000**: 系统时间 - 语音播报当前系统时间
- **3000**: 会议室 - 多人语音会议
- **8000**: 语音信箱 - 访问语音邮箱系统
- **600**: 消息测试 - 发送SIP消息示例
- **700**: 呼叫转移 - 转移到user1的示例
- **110**: 紧急呼叫模拟

### 3. 支持的功能

- SIP注册和呼叫
- TLS加密通信
- 用户认证
- 点对点通话
- 语音信箱
- 视频通话（support用户）
- 会议通话
- 呼叫转移
- 三方通话
- NAT穿透
- 多种编解码器支持（ulaw, alaw, gsm, opus等）

## 使用SIP客户端连接

### 配置参数

在您的SIP客户端（如Linphone、Zoiper、X-Lite等）中使用以下参数：

- **服务器地址**: 本机IP地址（启动脚本会显示）
- **端口**:
  - 5060 (UDP/TCP)
  - 5061 (TLS)
- **用户名**: user1、user2或support
- **密码**: password1、password2或support123
- **认证ID**: 与用户名相同
- **传输方式**: UDP (默认)、TCP或TLS

### Android客户端配置（Linphone SDK）

```java
// Linphone SIP配置示例
LinphonePreferences.instance().setTransport(TransportType.Udp);  // 可选: Udp, Tcp, Tls
LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
if (lc != null) {
    // 注册SIP账号
    LinphoneAuthInfo authInfo = Factory.instance().createAuthInfo(
        username,                 // SIP用户名 (user1/user2/support)
        null,                     // 用户ID (一般与用户名相同)
        password,                 // SIP密码
        null,                     // HA1
        null,                     // Realm (留空自动检测)
        domainAddress             // SIP域 (本地Asterisk服务器IP)
    );
    lc.addAuthInfo(authInfo);
    
    // 配置代理
    String identity = "sip:" + username + "@" + domainAddress;
    String proxy = "sip:" + domainAddress;
    LinphoneProxyConfig proxyConfig = lc.createProxyConfig();
    proxyConfig.setIdentity(identity);
    proxyConfig.setServerAddr(proxy);
    proxyConfig.setRegisterEnabled(true);
    lc.addProxyConfig(proxyConfig);
    lc.setDefaultProxyConfig(proxyConfig);
}
```

### 测试呼叫

1. 使用两个不同的客户端分别注册user1和user2
2. 从一个客户端拨打另一个用户的ID（例如，从user1拨打user2）
3. 接听电话并测试通话质量
4. 尝试拨打特殊号码进行功能测试

## 管理命令

### 查看日志

```powershell
docker-compose -f asterisk-docker-compose.yml logs -f
```

### 进入Asterisk CLI

```powershell
docker exec -it asterisk asterisk -rvvv
```

常用CLI命令：
- `sip show peers`: 显示所有SIP用户状态
- `sip show registry`: 显示SIP注册状态
- `core show channels`: 显示活动通话
- `core show applications`: 显示可用的应用程序
- `dialplan show`: 显示拨号计划
- `exit`: 退出CLI

### 停止服务

```powershell
.\stop-asterisk.ps1
```

## TLS证书管理

### 自动生成证书

使用提供的脚本生成新证书:

```powershell
.\generate_asterisk_cert.ps1
```

### 手动配置证书

1. 将证书文件放置在`certs`目录下
2. 确保文件名符合`sip.conf`中的引用:
   - `asterisk.pem`: 包含服务器私钥和证书
   - `ca.crt`: CA证书

## 配置文件说明

### sip.conf

包含SIP协议和用户配置:
- `[general]` 部分定义全局SIP设置
- 用户部分（如`[user1]`）定义每个用户的参数
- TLS配置在general部分中设置

### extensions.conf

定义拨号计划，控制呼叫路由逻辑:
- `[internal]` 上下文包含所有内部分机和功能
- 特殊号码（如999、1000等）在此定义

### users.conf

使用模板简化用户配置管理:
- `[template]` 定义共享设置
- 各用户部分引用模板并添加特定设置

## 故障排除

1. **客户端无法注册**
   - 确保Docker容器正在运行: `docker ps | grep asterisk`
   - 检查防火墙是否允许5060端口(UDP/TCP)和5061端口(TLS)
   - 确认用户名和密码正确
   - 尝试使用不同的传输方式（UDP、TCP、TLS）

2. **TLS连接问题**
   - 确认证书已正确生成并挂载
   - 检查客户端是否支持服务器使用的TLS版本
   - 使用`openssl s_client -connect IP:5061`测试TLS连接

3. **用户可以注册但无法呼叫**
   - 检查拨号计划配置
   - 确认两个用户都已成功注册
   - 使用Asterisk CLI检查用户状态: `sip show peers`
   - 尝试使用完整SIP URI（如`sip:user2@服务器IP`）

4. **音频问题**
   - 确保RTP端口范围（10000-10100）已开放
   - 尝试使用不同的编解码器（在SIP客户端设置中）
   - 尝试回音测试（拨打999）确认音频通路

5. **查看详细日志**
   - 进入Asterisk CLI并使用 `core set verbose 5` 增加日志详细程度
   - 使用 `core set debug 5` 开启调试信息

## 与移动客户端集成

### Android（使用Linphone SDK）

1. 确保在项目中正确引入Linphone SDK
2. 使用本指南中的示例代码配置SIP账户
3. 确保使用正确的服务器地址（本机网络IP，而非localhost）
4. 启用IPv4而非IPv6
5. 添加STUN服务器以改善NAT穿透（如：stun.l.google.com:19302）

### iOS

1. 使用如Linphone或Zoiper等SIP应用中添加账户
2. 配置步骤与Android类似
3. 确保应用允许在后台运行以接收呼叫
4. 如果需要开发自定义iOS应用，可使用Linphone SDK的iOS版本

## 性能优化

对于单个演示服务器，默认配置已经足够使用。如需处理更多并发呼叫，可考虑以下优化：

1. 增加RTP端口范围（在asterisk-docker-compose.yml中）
2. 调整SIP连接限制（在sip.conf中）
3. 使用G.729等高压缩编解码器降低带宽需求
4. 对于大规模部署，考虑使用Kamailio负载均衡多个Asterisk实例

## 高级特性

本配置已启用但需进一步配置的高级功能：

1. **视频通话**: 目前仅support用户配置支持视频
2. **语音邮箱**: 可扩展配置更多个性化选项
3. **自动接听系统**: 可扩展为完整IVR系统
4. **呼叫队列和分配**: 可实现呼叫中心场景
5. **WebRTC整合**: 可与WebRTC网关整合提供网页通话