# ==================================
# Asterisk SIP服务器启动脚本 (PowerShell)
# ==================================

# 检查Docker是否已安装
Write-Host "正在检查Docker环境..." -ForegroundColor Cyan
try {
    $dockerVersion = docker --version
    Write-Host "Docker环境正常: $dockerVersion" -ForegroundColor Green
} catch {
    Write-Host "错误：Docker未安装或未运行！请确保Docker Desktop已安装并正在运行。" -ForegroundColor Red
    Read-Host "按Enter键退出"
    exit 1
}

# 检查必要的配置文件
Write-Host "正在检查配置文件..." -ForegroundColor Cyan
$configFiles = @(
    "asterisk/pjsip.conf",
    "asterisk/extensions.conf",
    "asterisk/users.conf"
)

$missingFiles = $false
foreach ($file in $configFiles) {
    if (-not (Test-Path $file)) {
        Write-Host "错误: 缺少配置文件 $file" -ForegroundColor Red
        $missingFiles = $true
    }
}

if ($missingFiles) {
    Write-Host "请确保所有必要的配置文件都存在后再运行此脚本。" -ForegroundColor Red
    Read-Host "按Enter键退出"
    exit 1
}

# 检查并生成证书
if (-not (Test-Path "certs/asterisk.pem")) {
    Write-Host "未检测到证书文件，需要生成..." -ForegroundColor Yellow
    $genCert = Read-Host "是否现在生成证书? (y/n) [默认: y]"
    if ($genCert -ne "n") {
        try {
            & .\generate_asterisk_cert.ps1
        } catch {
            Write-Host "证书生成失败: $_" -ForegroundColor Red
            Write-Host "将使用非TLS模式继续..." -ForegroundColor Yellow
        }
    } else {
        Write-Host "将使用非TLS模式继续..." -ForegroundColor Yellow
    }
} else {
    Write-Host "证书文件已存在，将使用现有证书。" -ForegroundColor Green
}

# 询问是否清理已有容器
$cleanAll = Read-Host "是否要清理现有的SIP服务器容器? (y/n) [默认: n]"
if ($cleanAll.ToLower() -eq "y") {
    Write-Host "正在清理环境..." -ForegroundColor Yellow
    docker-compose -f asterisk-docker-compose.yml down
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "清理过程中遇到问题，但将继续尝试启动服务。" -ForegroundColor Yellow
    } else {
        Write-Host "环境已成功清理。" -ForegroundColor Green
    }
}

# 检查已有的容器卷是否存在
$volumes = docker volume ls --filter "name=asterisk_" -q
if ($volumes) {
    Write-Host "检测到现有的Asterisk数据卷:" -ForegroundColor Cyan
    foreach ($volume in $volumes) {
        Write-Host "  - $volume" -ForegroundColor Cyan
    }
    $cleanVolumes = Read-Host "是否要清理这些数据卷? 这将删除所有语音邮件和自定义音频文件! (y/n) [默认: n]"
    if ($cleanVolumes.ToLower() -eq "y") {
        Write-Host "正在清理Asterisk数据卷..." -ForegroundColor Yellow
        foreach ($volume in $volumes) {
            docker volume rm $volume
            if ($LASTEXITCODE -eq 0) {
                Write-Host "  - $volume 已删除" -ForegroundColor Green
            } else {
                Write-Host "  - 删除 $volume 失败" -ForegroundColor Red
            }
        }
    }
}

# 注意：IP地址已手动设置在下面的环境变量中
Write-Host "使用手动设置的IP地址..." -ForegroundColor Cyan

# 设置外部IP地址
$externalIP = "192.168.113.158"  # 固定使用此IP地址
Write-Host "使用外部IP地址: $externalIP" -ForegroundColor Green

# 启动SIP服务器
Write-Host "正在启动Asterisk SIP服务器..." -ForegroundColor Cyan
$env:EXTERNAL_IP=$externalIP
docker-compose -f asterisk-docker-compose.yml up -d

if ($LASTEXITCODE -ne 0) {
    Write-Host "错误：启动SIP服务器失败！请检查日志以获取详细信息。" -ForegroundColor Red
    Write-Host "运行 'docker-compose -f asterisk-docker-compose.yml logs' 查看日志。" -ForegroundColor Yellow
    Read-Host "按Enter键退出"
    exit 1
}

# 检查容器是否正常运行
Start-Sleep -Seconds 5
$containerStatus = docker ps --filter "name=asterisk" --format "{{.Status}}"
if ($containerStatus -match "Up") {
    Write-Host "Asterisk容器已成功启动并运行。" -ForegroundColor Green
} else {
    Write-Host "警告: Asterisk容器状态异常，请检查日志。" -ForegroundColor Yellow
    Write-Host "运行 'docker-compose -f asterisk-docker-compose.yml logs' 查看日志。" -ForegroundColor Yellow
}

# 显示成功信息
Write-Host "`nAsterisk SIP服务器已成功启动！" -ForegroundColor Green
Write-Host "`n---------- 服务器配置信息 ----------" -ForegroundColor Cyan
Write-Host "本机IP地址: $env:EXTERNAL_IP（使用此IP注册SIP客户端）"
Write-Host "SIP端口: 5060 (UDP/TCP)"
Write-Host "TLS端口: 5061"
Write-Host "用户账号: "
Write-Host "  - user1/password1"
Write-Host "  - user2/password2"
Write-Host "  - support/support123 (视频支持)"
Write-Host "特殊功能:"
Write-Host "  - 999: 回音测试"
Write-Host "  - 1000: 语音演示"
Write-Host "  - 2000: 系统时间"
Write-Host "  - 3000: 会议室"
Write-Host "  - 8000: 语音邮箱"
Write-Host "------------------------------------`n"

Write-Host "常用命令:" -ForegroundColor Yellow
Write-Host "  - 查看日志: docker-compose -f asterisk-docker-compose.yml logs -f"
Write-Host "  - 停止服务: docker-compose -f asterisk-docker-compose.yml down"
Write-Host "  - 进入容器: docker exec -it asterisk bash"
Write-Host "  - Asterisk CLI: docker exec -it asterisk asterisk -rvvv"
Write-Host ""
Read-Host "按Enter键退出脚本" 