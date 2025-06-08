# ==================================
# Asterisk SIP调试脚本 (PowerShell)
# ==================================

# 停止正在运行的服务
Write-Host "停止现有Asterisk容器..." -ForegroundColor Yellow
docker-compose -f asterisk-docker-compose.yml down
docker rm -f asterisk-debug 2>$null

# 设置环境变量
$env:EXTERNAL_IP="10.29.206.148"
Write-Host "使用IP地址: $env:EXTERNAL_IP" -ForegroundColor Cyan

# 启动调试容器
Write-Host "启动Asterisk调试容器..." -ForegroundColor Cyan
docker-compose -f docker-compose-debug.yml up -d

# 实时显示日志
Write-Host "显示Asterisk日志 (按Ctrl+C退出)..." -ForegroundColor Green
docker logs -f asterisk-debug