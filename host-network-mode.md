# 使用Docker Host网络模式的Asterisk部署

## 概述

在这个配置中，我们将Asterisk容器设置为使用宿主机的网络栈，而不是Docker默认的桥接网络。这种方式绕过了NAT映射问题，特别是解决了RTP媒体流显示Docker内部IP地址的问题。

## 优势

1. **无需NAT配置**：避免了复杂的NAT穿透和端口映射配置
2. **更低的延迟**：没有额外的网络层，通信延迟更低
3. **更简单的配置**：不需要维护external_media_address等额外配置
4. **SDP中地址正确**：媒体协商中会使用宿主机真实IP地址
5. **简化排错**：网络问题更容易诊断和解决

## 劣势

1. **降低隔离性**：容器直接访问宿主机网络，安全隔离性降低
2. **端口冲突风险**：需要确保宿主机上没有服务占用Asterisk所需端口
3. **无法使用Docker网络功能**：如Docker内部DNS，网络别名等
4. **多实例部署困难**：无法在同一主机上运行多个使用相同端口的Asterisk实例

## 实现方法

在`docker-compose.yml`文件中，只需设置：
```yaml
services:
  asterisk:
    network_mode: host
    # 不再需要ports部分
```

## 配置说明

1. **不需要端口映射**：使用host网络模式时，不需要配置`ports`部分

2. **pjsip.conf简化**：不再需要额外的NAT相关配置：
   ```
   [transport-udp]
   type=transport
   protocol=udp
   bind=0.0.0.0:5060
   ```

3. **RTP配置简化**：大部分NAT相关的RTP配置也可以简化

## 测试与验证

部署后，请验证：

1. 使用Wireshark抓包检查SDP中的媒体地址是否为宿主机IP
2. 确认语音通话和视频通话双向可通
3. 验证多客户端并发连接是否正常工作

## 注意事项

- 如果宿主机有多个网络接口，默认情况下Asterisk会绑定到所有接口
- 建议在SIP客户端设置中明确指定服务器地址，而非使用自动发现
- 在云环境中使用时，注意防火墙配置，确保所有必要端口开放

## 配置更改总结

以下是我们为迁移到Host网络模式所做的主要配置更改：

1. **docker-compose.yml**:
   - 添加 `network_mode: host` 配置
   - 移除了端口映射 (`ports` 部分)
   - 移除了自定义网络配置

2. **pjsip.conf**:
   - 简化了传输配置，移除了NAT相关参数
   - 注释掉了`external_media_address`和`external_signaling_address`
   - 移除了endpoint模板中的额外NAT处理选项

3. **rtp.conf**:
   - 保留了基本的RTP端口范围配置
   - 保留了STUN/ICE支持
   - 移除了NAT相关的配置(`externip`, `localnet`等)

4. **start-asterisk.ps1**:
   - 移除了外部IP地址的手动配置
   - 简化了启动过程

这种配置方法大大简化了Asterisk部署，适合在本地开发和测试环境中使用 