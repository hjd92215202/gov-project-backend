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
