param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MavenArgs
)

. (Join-Path $PSScriptRoot 'use-jdk8-env.ps1')

if (-not $MavenArgs -or $MavenArgs.Count -eq 0) {
    $MavenArgs = @('-q', '-DskipTests', 'compile')
}

& mvn @MavenArgs
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
