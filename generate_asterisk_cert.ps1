# =================================================
# Asterisk SIP服务器证书生成脚本 (PowerShell)
# =================================================

$ErrorActionPreference = "Stop"
$certDir = ".\certs"
$hostname = [System.Net.Dns]::GetHostName()
$ipAddress = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object {$_.InterfaceAlias -like "*Ethernet*" -or $_.InterfaceAlias -like "*Wi-Fi*"}).IPAddress | Select-Object -First 1

# 创建证书目录（如果不存在）
if(-not (Test-Path $certDir)) {
    New-Item -ItemType Directory -Path $certDir | Out-Null
    Write-Host "创建证书目录: $certDir" -ForegroundColor Green
}

Write-Host "生成Asterisk SIP服务器证书..." -ForegroundColor Cyan
Write-Host "主机名: $hostname" -ForegroundColor Cyan
Write-Host "IP地址: $ipAddress" -ForegroundColor Cyan

# 检查OpenSSL是否安装
try {
    $opensslVersion = openssl version
    Write-Host "已检测到OpenSSL: $opensslVersion" -ForegroundColor Green
}
catch {
    Write-Host "错误: 找不到OpenSSL。请安装OpenSSL并确保它在PATH环境变量中。" -ForegroundColor Red
    Write-Host "您可以从 https://slproweb.com/products/Win32OpenSSL.html 下载" -ForegroundColor Yellow
    exit 1
}

# 创建CA私钥和证书
Write-Host "`n1. 生成CA私钥和证书..." -ForegroundColor Cyan
openssl genrsa -out "$certDir/ca.key" 2048
openssl req -new -x509 -days 365 -key "$certDir/ca.key" -out "$certDir/ca.crt" -subj "/CN=AI衣搭CA/O=AI衣搭/C=CN"

# 创建服务器私钥和证书签名请求
Write-Host "`n2. 生成服务器私钥和证书请求..." -ForegroundColor Cyan
openssl genrsa -out "$certDir/asterisk.key" 2048
openssl req -new -key "$certDir/asterisk.key" -out "$certDir/asterisk.csr" -subj "/CN=$hostname/O=AI衣搭/C=CN"

# 创建openssl的扩展配置文件，添加SAN扩展
$extFile = "$certDir/san.ext"
@"
[SAN]
subjectAltName=DNS:$hostname,DNS:localhost,IP:$ipAddress,IP:127.0.0.1
"@ | Out-File -FilePath $extFile -Encoding ascii

# 使用CA签名服务器证书
Write-Host "`n3. 使用CA签名服务器证书..." -ForegroundColor Cyan
openssl x509 -req -days 365 -in "$certDir/asterisk.csr" -CA "$certDir/ca.crt" -CAkey "$certDir/ca.key" -CAcreateserial -out "$certDir/asterisk.crt" -extfile $extFile -extensions SAN

# 创建Asterisk PEM文件
Write-Host "`n4. 创建Asterisk PEM文件..." -ForegroundColor Cyan
Get-Content "$certDir/asterisk.key", "$certDir/asterisk.crt" | Out-File -FilePath "$certDir/asterisk.pem" -Encoding ascii

# 成功消息
Write-Host "`n证书生成完成！" -ForegroundColor Green
Write-Host "证书路径: $certDir" -ForegroundColor Green
Write-Host "以下文件已创建:" -ForegroundColor Green
Get-ChildItem $certDir | ForEach-Object { Write-Host "- $($_.Name)" -ForegroundColor Cyan }

# 提示
Write-Host "`n注意: 请将这些证书文件挂载到Asterisk容器中。" -ForegroundColor Yellow
Write-Host "在asterisk-docker-compose.yml中添加:" -ForegroundColor Yellow
Write-Host "  volumes:" -ForegroundColor Yellow
Write-Host "    - ./certs:/etc/asterisk/certs" -ForegroundColor Yellow

Read-Host "`n按Enter键继续" 