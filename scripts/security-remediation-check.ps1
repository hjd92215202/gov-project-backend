param(
    [string]$RepoRoot = (Split-Path -Path $PSScriptRoot -Parent)
)

$ErrorActionPreference = 'Stop'

Set-Location $RepoRoot

$checks = @(
    @{
        Name = 'Forbidden sensitive defaults'
        Paths = @(
            'src/main/resources/application.yml',
            'deploy/kylin-arm/backend.env.example',
            'deploy/kylin-arm/run-mariadb.sh',
            'deploy/kylin-arm/run-minio.sh',
            'deploy/kylin-arm/wait-mariadb-and-init.sh',
            'scripts/db_backup.py',
            'src/main/java/com/gov/config/properties/SmCryptoProperties.java',
            'src/main/java/com/gov/crypto/SmTypeHandler.java'
        )
        Patterns = @(
            'GOV_DB_PASSWORD=Egov@123',
            'MYSQL_ROOT_PASSWORD=123',
            'GOV_MINIO_ACCESS_KEY=govadmin',
            'GOV_MINIO_SECRET_KEY=govadminpassword',
            'GOV_SM4_KEY=1234567812345678',
            'read_env_value "MYSQL_ROOT_PASSWORD" "123"',
            'read_env_value "GOV_MINIO_ACCESS_KEY" "govadmin"',
            'read_env_value "GOV_MINIO_SECRET_KEY" "govadminpassword"',
            'read_env_value "GOV_DB_PASSWORD" "Egov@123"',
            'os.environ.get("GOV_DB_PASSWORD", "Egov@123")',
            '"1234567812345678"'
        )
    },
    @{
        Name = 'RuntimeException usage'
        Paths = @('src/main/java')
        Patterns = @('throw new RuntimeException')
    },
    @{
        Name = 'Ignored exception handlers'
        Paths = @('src/main/java')
        Patterns = @('catch (Exception ignored)')
    },
    @{
        Name = 'Forbidden public upload endpoint'
        Paths = @('src/main/java')
        Patterns = @('/common/upload')
    },
    @{
        Name = 'Browser token persistence'
        Paths = @('../gov-web/src')
        Patterns = @(
            "localStorage.setItem('token'",
            'localStorage.setItem("token"',
            "sessionStorage.setItem('token'",
            'sessionStorage.setItem("token"',
            'Authorization = token',
            'headers.Authorization = token'
        )
    },
    @{
        Name = 'Public MinIO business URL fallback'
        Paths = @(
            'src/main/java/com/gov/module/file/service',
            'src/main/java/com/gov/module/project/controller'
        )
        Patterns = @('buildPublicDownloadUrl(')
    },
    @{
        Name = 'Direct 401 login message literals'
        Paths = @('src/main/java/com/gov/module/system/service/impl/SysUserServiceImpl.java')
        Patterns = @('throw new BizException(401, "')
    }
)

$violations = New-Object System.Collections.Generic.List[string]

foreach ($check in $checks) {
    foreach ($path in $check.Paths) {
        $absolutePath = Join-Path $RepoRoot $path
        if (-not (Test-Path -LiteralPath $absolutePath)) {
            continue
        }
        $scanTargets = @($absolutePath)
        if ((Get-Item -LiteralPath $absolutePath).PSIsContainer) {
            $scanTargets = Get-ChildItem -Path $absolutePath -Recurse -File | Select-Object -ExpandProperty FullName
        }
        foreach ($pattern in $check.Patterns) {
            $matches = Select-String -Path $scanTargets -Pattern $pattern -SimpleMatch
            foreach ($match in $matches) {
                $violations.Add(("{0}: {1}:{2} => {3}" -f $check.Name, $match.Path, $match.LineNumber, $pattern))
            }
        }
    }
}

if ($violations.Count -gt 0) {
    Write-Output 'Security remediation check failed:'
    $violations | ForEach-Object { Write-Output $_ }
    exit 1
}

Write-Output 'Security remediation check passed.'
