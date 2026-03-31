param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MavenArgs
)

$resolvedArgs = @('spring-boot:run')
if ($MavenArgs -and $MavenArgs.Count -gt 0) {
    $resolvedArgs += $MavenArgs
}

& (Join-Path $PSScriptRoot 'mvn-jdk8.ps1') @resolvedArgs
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
