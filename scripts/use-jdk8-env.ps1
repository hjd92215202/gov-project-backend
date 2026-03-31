param(
    [string]$JdkHome
)

$script:RepoRoot = Split-Path -Path $PSScriptRoot -Parent

function Get-DotEnvValue {
    param(
        [string]$FilePath,
        [string]$Key
    )

    if (-not (Test-Path -LiteralPath $FilePath)) {
        return $null
    }

    foreach ($line in Get-Content -LiteralPath $FilePath -Encoding UTF8) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith('#')) {
            continue
        }
        if ($trimmed -notmatch '^[^=]+=') {
            continue
        }
        $parts = $trimmed.Split('=', 2)
        if ($parts.Count -ne 2) {
            continue
        }
        if ($parts[0].Trim() -ne $Key) {
            continue
        }
        return $parts[1].Trim().Trim('"')
    }

    return $null
}

if (-not $JdkHome) {
    $JdkHome = $env:GOV_JAVA_HOME
}

if (-not $JdkHome) {
    $JdkHome = Get-DotEnvValue -FilePath (Join-Path $script:RepoRoot '.env') -Key 'GOV_JAVA_HOME'
}

if (-not $JdkHome) {
    $JdkHome = 'C:\Program Files\Java\jdk1.8.0_112'
}

$javaExe = Join-Path $JdkHome 'bin\java.exe'
if (-not (Test-Path -LiteralPath $javaExe)) {
    throw "JDK 8 not found: $JdkHome. Please check GOV_JAVA_HOME."
}

$env:JAVA_HOME = $JdkHome
$env:Path = (Join-Path $JdkHome 'bin') + ';' + $env:Path

Write-Output "Using JDK: $JdkHome"
& $javaExe -version
