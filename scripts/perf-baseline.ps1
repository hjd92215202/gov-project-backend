param(
    [string]$LogPath = "logs/perf.log",
    [string]$OutputPath = "docs/perf-baseline-latest.md"
)

function Get-PercentileValue {
    param(
        [int[]]$Values,
        [double]$Percentile
    )
    if (-not $Values -or $Values.Count -eq 0) {
        return 0
    }
    $sorted = $Values | Sort-Object
    $index = [Math]::Ceiling(($Percentile / 100.0) * $sorted.Count) - 1
    if ($index -lt 0) { $index = 0 }
    if ($index -ge $sorted.Count) { $index = $sorted.Count - 1 }
    return [int]$sorted[$index]
}

if (-not (Test-Path -LiteralPath $LogPath)) {
    throw "perf log not found: $LogPath"
}

$records = New-Object System.Collections.Generic.List[object]
Get-Content -LiteralPath $LogPath | ForEach-Object {
    $line = $_
    $durationMatch = [regex]::Match($line, "(?:totalMs|durationMs)=([0-9]+)")
    if (-not $durationMatch.Success) {
        return
    }

    $durationMs = [int]$durationMatch.Groups[1].Value
    $actionMatch = [regex]::Match($line, "action=([a-zA-Z0-9_:\\-]+)")
    $action = if ($actionMatch.Success) {
        $actionMatch.Groups[1].Value
    } else {
        "legacy"
    }

    $records.Add([pscustomobject]@{
        Action = $action
        DurationMs = $durationMs
    })
}

if ($records.Count -eq 0) {
    throw "no duration markers found in log: $LogPath"
}

$groups = $records | Group-Object -Property Action
$stats = $groups | ForEach-Object {
    $durations = @($_.Group | ForEach-Object { [int]$_.DurationMs })
    $sum = ($durations | Measure-Object -Sum).Sum
    [pscustomobject]@{
        Action = $_.Name
        Count = $durations.Count
        AvgMs = [int][Math]::Round($sum / [Math]::Max(1, $durations.Count))
        P50Ms = Get-PercentileValue -Values $durations -Percentile 50
        P95Ms = Get-PercentileValue -Values $durations -Percentile 95
        P99Ms = Get-PercentileValue -Values $durations -Percentile 99
        MaxMs = ($durations | Measure-Object -Maximum).Maximum
    }
} | Sort-Object -Property P95Ms -Descending

$tableHeader = "| Action | Count | Avg(ms) | P50(ms) | P95(ms) | P99(ms) | Max(ms) |"
$tableSplit = "| --- | ---: | ---: | ---: | ---: | ---: | ---: |"
$tableRows = $stats | ForEach-Object {
    "| $($_.Action) | $($_.Count) | $($_.AvgMs) | $($_.P50Ms) | $($_.P95Ms) | $($_.P99Ms) | $($_.MaxMs) |"
}

$reportLines = @(
    "# Performance Baseline",
    "",
    "- GeneratedAt: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')",
    "- SourceLog: ``$LogPath``",
    "",
    $tableHeader,
    $tableSplit
) + $tableRows

$outputDir = Split-Path -Path $OutputPath -Parent
if ($outputDir -and -not (Test-Path -LiteralPath $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

Set-Content -LiteralPath $OutputPath -Value $reportLines -Encoding UTF8
Write-Output "Baseline report generated: $OutputPath"
