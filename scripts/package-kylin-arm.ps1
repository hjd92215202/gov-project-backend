param(
    [string]$FrontendRepo,
    [string]$BackendRepo,
    [string]$OutputDir
)

$script:CurrentRepo = Split-Path -Path $PSScriptRoot -Parent
if (-not $BackendRepo) {
    $BackendRepo = $script:CurrentRepo
}
if (-not $FrontendRepo) {
    $FrontendRepo = Join-Path (Split-Path -Path $BackendRepo -Parent) 'gov-web'
}
if (-not $OutputDir) {
    $OutputDir = Join-Path $BackendRepo 'deploy-output\gov4'
}

if (-not (Test-Path -LiteralPath $FrontendRepo)) {
    throw "Frontend repo not found: $FrontendRepo"
}

Write-Output "Building frontend ..."
Push-Location $FrontendRepo
try {
    & npm run build
    if ($LASTEXITCODE -ne 0) {
        throw "Frontend build failed."
    }
} finally {
    Pop-Location
}

Write-Output "Building backend jar ..."
Push-Location $BackendRepo
try {
    & powershell -ExecutionPolicy Bypass -File (Join-Path $BackendRepo 'scripts\mvn-jdk8.ps1') -q -DskipTests clean package
    if ($LASTEXITCODE -ne 0) {
        throw "Backend package failed."
    }
} finally {
    Pop-Location
}

$jarFile = Get-ChildItem -Path (Join-Path $BackendRepo 'target') -Filter *.jar -File |
    Where-Object { $_.Name -notlike 'original-*' } |
    Sort-Object Length -Descending |
    Select-Object -First 1

if (-not $jarFile) {
    throw "Backend jar not found in target directory."
}

if (Test-Path -LiteralPath $OutputDir) {
    Remove-Item -LiteralPath $OutputDir -Recurse -Force
}

New-Item -ItemType Directory -Path $OutputDir | Out-Null
New-Item -ItemType Directory -Path (Join-Path $OutputDir 'backend') | Out-Null
New-Item -ItemType Directory -Path (Join-Path $OutputDir 'frontend') | Out-Null
New-Item -ItemType Directory -Path (Join-Path $OutputDir 'frontend\runtime') -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $OutputDir 'mariadb\init') -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $OutputDir 'scripts') | Out-Null

Copy-Item -LiteralPath $jarFile.FullName -Destination (Join-Path $OutputDir 'backend\app.jar')
Copy-Item -LiteralPath (Join-Path $BackendRepo 'deploy\kylin-arm\backend.env.example') -Destination (Join-Path $OutputDir 'backend\backend.env')
Copy-Item -LiteralPath (Join-Path $BackendRepo 'RBAC.sql') -Destination (Join-Path $OutputDir 'mariadb\init\RBAC.sql')
Copy-Item -LiteralPath (Join-Path $FrontendRepo 'deploy\kylin-arm\nginx.conf') -Destination (Join-Path $OutputDir 'frontend\nginx.conf')
Copy-Item -LiteralPath (Join-Path $FrontendRepo 'deploy\kylin-arm\frontend.env.example') -Destination (Join-Path $OutputDir 'frontend\frontend.env')
Copy-Item -LiteralPath (Join-Path $FrontendRepo 'dist') -Destination (Join-Path $OutputDir 'frontend\dist') -Recurse

$deployScripts = @(
    'prepare-directories.sh',
    'run-mariadb.sh',
    'wait-mariadb-and-init.sh',
    'run-minio.sh',
    'run-backend.sh',
    'run-frontend.sh',
    'deploy-all.sh',
    'README.md'
)

foreach ($item in $deployScripts) {
    Copy-Item -LiteralPath (Join-Path $BackendRepo "deploy\kylin-arm\$item") -Destination (Join-Path $OutputDir "scripts\$item")
}

Write-Output "Package ready: $OutputDir"
