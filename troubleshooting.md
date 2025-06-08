# Asterisk SIP服务器问题排查指南

## RTP媒体流使用Docker内部IP问题

### 问题描述
Wireshark抓包显示RTP传输使用的是Docker内部IP地址（如172.18.0.2），而非宿主机IP地址，导致客户端无法接收媒体流。

### 解决方案
1. 确保pjsip.conf中正确配置了外部IP:
   ```
   [transport-udp]
   type=transport
   protocol=udp
   bind=0.0.0.0:5060
   local_net=10.0.0.0/8
   local_net=172.16.0.0/12
   external_media_address=<外部IP>
   external_signaling_address=<外部IP>
   ```

2. 确保rtp.conf中也设置了外部IP:
   ```
   [general]
   externip=<外部IP>
   localnet=10.0.0.0/8
   localnet=172.16.0.0/12
   nat=force_rport,comedia
   ```

3. 检查Docker环境变量是否正确传递:
   ```
   $env:EXTERNAL_IP=<外部IP>
   ```

4. 重启Asterisk服务:
   ```powershell
   docker-compose -f asterisk-docker-compose.yml down
   docker-compose -f asterisk-docker-compose.yml up -d
   ```

5. 检查配置是否生效:
   ```
   docker exec -it asterisk asterisk -rx "pjsip show transports"
   ```

## 排查步骤

### 1. 检查网络配置
```powershell
# 查看宿主机IP配置
ipconfig

# 查看Docker网络配置
docker network ls
docker network inspect <network_id>
```

### 2. 检查Asterisk日志
```powershell
# 查看Asterisk容器日志
docker-compose -f asterisk-docker-compose.yml logs -f

# 进入容器查看详细日志
docker exec -it asterisk bash
cat /var/log/asterisk/messages
```

### 3. 检查SIP/PJSIP配置
```powershell
# 查看PJSIP传输配置
docker exec -it asterisk asterisk -rx "pjsip show transports"

# 查看已注册的终端
docker exec -it asterisk asterisk -rx "pjsip show endpoints"
docker exec -it asterisk asterisk -rx "pjsip show contacts"
```

### 4. 检查RTP配置
```powershell
# 查看RTP统计信息
docker exec -it asterisk asterisk -rx "rtp show stats"
```

### 5. 实时监控通话
```powershell
# 在Asterisk CLI中开启SIP调试
docker exec -it asterisk asterisk -rvvv
# 在CLI中输入:
sip set debug on
rtp set debug on
```

## 常见配置错误

1. **错误**: 缺少external_media_address设置
   **解决方案**: 在transport配置中添加external_media_address=<外部IP>

2. **错误**: 未将Docker内网添加到local_net
   **解决方案**: 添加local_net=172.16.0.0/12到PJSIP配置

3. **错误**: RTP端口映射不正确
   **解决方案**: 确保docker-compose.yml中映射了正确的RTP端口范围

4. **错误**: NAT穿透配置不完整
   **解决方案**: 添加force_rport=yes, rtp_symmetric=yes和rewrite_contact=yes 