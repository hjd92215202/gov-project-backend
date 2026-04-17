#!/usr/bin/env pwsh

# Government Project Backend - 本地开发启动脚本
# 用法: .\dev.ps1 [命令]
# 命令: compile | run | help

param(
    [string]$command = "help"
)

# ==================== 环境变量配置 ====================
$env:GOV_SM4_KEY = "1234567812345678"
#密码加盐值 aB3$xK9@mL2#pQ7&vN5!wR8%
$env:GOV_PASSWORD_PEPPER = ""
$env:GOV_DB_URL = "jdbc:mariadb://localhost:13306/gov_db?useUnicode=true&characterEncoding=utf8mb4&connectionCollation=utf8mb4_unicode_ci&serverTimezone=GMT%2B8&useSSL=false&allowPublicKeyRetrieval=true&useInformationSchema=true"
$env:GOV_DB_USERNAME = "db_user"
$env:GOV_DB_PASSWORD = "Egov@123"
$env:GOV_MINIO_ENDPOINT = "http://127.0.0.1:9000"
$env:GOV_MINIO_PUBLIC_ENDPOINT = "http://127.0.0.1:9000"
$env:GOV_MINIO_ACCESS_KEY = "govadmin"
$env:GOV_MINIO_SECRET_KEY = "govadminpassword"
$env:JAVA_OPTS = "-Xms2048m -Xmx2048m -Dfile.encoding=UTF-8"

# ==================== 函数定义 ====================

function Compile-Backend {
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "编译后端" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan

    Write-Host "执行: mvn clean compile" -ForegroundColor Yellow
    mvn clean compile

    if ($LASTEXITCODE -eq 0) {
        Write-Host "`n✓ 后端编译成功" -ForegroundColor Green
    } else {
        Write-Host "`n❌ 后端编译失败" -ForegroundColor Red
    }
}

function Run-Backend {
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "运行后端" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan

    Write-Host "环境变量:" -ForegroundColor Yellow
    Write-Host "  GOV_SM4_KEY: $($env:GOV_SM4_KEY)" -ForegroundColor Gray
    Write-Host "  GOV_DB_URL: $($env:GOV_DB_URL)" -ForegroundColor Gray
    Write-Host "  GOV_MINIO_ENDPOINT: $($env:GOV_MINIO_ENDPOINT)" -ForegroundColor Gray
    Write-Host "  JAVA_OPTS: $($env:JAVA_OPTS)" -ForegroundColor Gray

    Write-Host "`n执行: mvn spring-boot:run" -ForegroundColor Yellow
    Write-Host "后端启动地址: http://localhost:8080/api" -ForegroundColor Green
    Write-Host "监控地址: http://localhost:8081/actuator/health" -ForegroundColor Green

    mvn spring-boot:run
}

function Show-Help {
    Write-Host @"
========================================
Government Project Backend - 启动脚本
========================================

用法: .\dev.ps1 [命令]

命令:
  compile   编译后端
  run       运行后端
  help      显示此帮助信息

环境变量:
  GOV_SM4_KEY=$($env:GOV_SM4_KEY)
  GOV_PASSWORD_PEPPER=$($env:GOV_PASSWORD_PEPPER)
  GOV_DB_URL=$($env:GOV_DB_URL)
  GOV_DB_USERNAME=$($env:GOV_DB_USERNAME)
  GOV_DB_PASSWORD=$($env:GOV_DB_PASSWORD)
  GOV_MINIO_ENDPOINT=$($env:GOV_MINIO_ENDPOINT)
  GOV_MINIO_PUBLIC_ENDPOINT=$($env:GOV_MINIO_PUBLIC_ENDPOINT)
  JAVA_OPTS=$($env:JAVA_OPTS)

示例:
  .\dev.ps1 compile    # 编译后端
  .\dev.ps1 run        # 运行后端

快速开始:
  .\dev.ps1 run

  访问:
    后端API: http://localhost:8080/api
    监控: http://localhost:8081/actuator/health

========================================
"@
}

# ==================== 主程序 ====================

switch ($command.ToLower()) {
    "compile" {
        Compile-Backend
    }
    "run" {
        Run-Backend
    }
    "help" {
        Show-Help
    }
    default {
        Show-Help
    }
}
