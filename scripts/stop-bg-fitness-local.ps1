$ErrorActionPreference = 'Stop'
$repoPath = Split-Path $PSScriptRoot -Parent
$envPath = Join-Path $repoPath '.env.bg-fitness.local'
if (!(Test-Path -LiteralPath $envPath)) { throw 'Local environment has not been prepared.' }
& docker compose --env-file $envPath -p phoenix-bg-fitness-local -f (Join-Path $repoPath 'docker-compose.bg-fitness.local.yml') stop
if ($LASTEXITCODE -ne 0) { throw 'Stop failed. No volumes were deleted.' }
