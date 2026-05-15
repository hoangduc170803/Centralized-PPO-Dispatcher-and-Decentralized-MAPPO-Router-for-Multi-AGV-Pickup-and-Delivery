param(
    [int[]]$Seeds = @(0, 1, 2),
    [string]$SeedCsv = "",
    [int]$Steps = 1000000,
    [int]$Agents = 5,
    [int]$EpisodeLength = 128,
    [string]$ExperimentName = "sprint3_5agv_1m",
    [string]$ResultsDir = "results/sprint3/onpolicy_smoke",
    [int]$LogInterval = 50,
    [int]$SaveInterval = 200
)

$ErrorActionPreference = "Stop"

if ($SeedCsv.Trim()) {
    $Seeds = $SeedCsv.Split(",") | ForEach-Object { [int]$_.Trim() }
}

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$Python = Join-Path $RepoRoot ".venv/Scripts/python.exe"
if (-not (Test-Path $Python)) {
    $Python = "python"
}

Push-Location $RepoRoot
try {
    foreach ($Seed in $Seeds) {
        Write-Host "[sprint3] training seed=$Seed steps=$Steps agents=$Agents"
        & $Python -m src.rl.mappo_onpolicy.train `
            --num_agents_target $Agents `
            --episode_length $EpisodeLength `
            --num_env_steps $Steps `
            --hidden_size 64 `
            --layer_N 2 `
            --experiment_name $ExperimentName `
            --seed $Seed `
            --results_dir $ResultsDir `
            --log_interval $LogInterval `
            --save_interval $SaveInterval
        if ($LASTEXITCODE -ne 0) {
            exit $LASTEXITCODE
        }

        $LogDir = Join-Path $ResultsDir "$($ExperimentName)_seed$Seed/logs"
        Write-Host "[sprint3] extracting metrics seed=$Seed from $LogDir"
        & $Python -m src.rl.mappo_onpolicy.read_metrics --log-dir $LogDir
        if ($LASTEXITCODE -ne 0) {
            exit $LASTEXITCODE
        }
    }
}
finally {
    Pop-Location
}
