# ==================================
# Asterisk SIP服务器停止脚本 (PowerShell)
# ==================================

Write-Host "正在停止Asterisk SIP服务器..." -ForegroundColor Cyan

# 检查是否已存在运行中的容器
$runningContainer = docker ps --filter "name=asterisk" --format "{{.Names}}"
if (-not $runningContainer) {
    Write-Host "未检测到运行中的Asterisk容器，无需停止。" -ForegroundColor Yellow
    
    # 检查是否有已停止的容器
    $stoppedContainer = docker ps -a --filter "name=asterisk" --format "{{.Names}}"
    if ($stoppedContainer) {
        Write-Host "检测到已停止的Asterisk容器。" -ForegroundColor Cyan
        $removeContainer = Read-Host "是否要移除已停止的容器? (y/n) [默认: n]"
        if ($removeContainer.ToLower() -eq "y") {
            docker rm $stoppedContainer
            if ($LASTEXITCODE -eq 0) {
                Write-Host "已成功移除容器 $stoppedContainer" -ForegroundColor Green
            } else {
                Write-Host "移除容器 $stoppedContainer 时发生错误" -ForegroundColor Red
            }
        }
    }
} else {
    # 停止服务
    docker-compose -f asterisk-docker-compose.yml down

    if ($LASTEXITCODE -eq 0) {
        Write-Host "Asterisk SIP服务器已成功停止！" -ForegroundColor Green
    } else {
        Write-Host "错误：停止服务器时出现问题，尝试强制停止..." -ForegroundColor Yellow
        
        # 尝试强制停止容器
        docker stop asterisk
        if ($LASTEXITCODE -eq 0) {
            Write-Host "已强制停止Asterisk容器" -ForegroundColor Green
        } else {
            Write-Host "错误：无法停止Asterisk容器" -ForegroundColor Red
        }
    }
    
    # 列出仍在运行的容器
    Write-Host "`n检查运行中的容器..." -ForegroundColor Yellow
    $stillRunning = docker ps --filter "name=asterisk"
    if ($stillRunning) {
        Write-Host "警告：以下Asterisk相关容器仍在运行:" -ForegroundColor Red
        docker ps --filter "name=asterisk"
    } else {
        Write-Host "已确认所有Asterisk容器均已停止" -ForegroundColor Green
    }
}

# 询问是否要清理卷
$askVolumes = Read-Host "`n是否要查看或清理Asterisk数据卷? (y/n) [默认: n]"
if ($askVolumes.ToLower() -eq "y") {
    $volumes = docker volume ls --filter "name=asterisk_" --format "{{.Name}}"
    if ($volumes) {
        Write-Host "`nAsterisk数据卷:" -ForegroundColor Cyan
        docker volume ls --filter "name=asterisk_"
        
        $cleanVolumes = Read-Host "`n是否要清理这些数据卷? 这将删除所有语音邮件和自定义音频文件! (y/n) [默认: n]"
        if ($cleanVolumes.ToLower() -eq "y") {
            foreach ($volume in $volumes) {
                docker volume rm $volume
                if ($LASTEXITCODE -eq 0) {
                    Write-Host "已删除卷 $volume" -ForegroundColor Green
                } else {
                    Write-Host "删除卷 $volume 时发生错误" -ForegroundColor Red
                }
            }
        }
    } else {
        Write-Host "未检测到Asterisk数据卷" -ForegroundColor Yellow
    }
}

Write-Host "`n操作完成！" -ForegroundColor Green
Read-Host "按Enter键退出脚本" 