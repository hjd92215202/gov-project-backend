# Perf Baseline Guide

## Scope
- Backend perf log baseline for key actions in `logs/perf.log`
- Focus on `action=projectMapList` and `action=projectMapSummary`

## Generate Baseline
```powershell
pwsh -File scripts/perf-baseline.ps1
```

Optional custom paths:
```powershell
pwsh -File scripts/perf-baseline.ps1 -LogPath logs/perf.log -OutputPath docs/perf-baseline-latest.md
```

## Output
- Markdown report: `docs/perf-baseline-latest.md`
- Metrics: `count / avg / p50 / p95 / p99 / max`

## Suggested Run Timing
1. Before optimization rollout (baseline)
2. After each batch (Batch 1, Batch 2)
3. Before merge to main branch
