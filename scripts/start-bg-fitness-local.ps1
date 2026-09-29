param([switch]$PrepareOnly, [switch]$NoBrowser)
$ErrorActionPreference = 'Stop'
$repoPath = Split-Path $PSScriptRoot -Parent
$envPath = Join-Path $repoPath '.env.bg-fitness.local'
$composePath = Join-Path $repoPath 'docker-compose.bg-fitness.local.yml'
function New-LocalSecret {
    $bytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return 'Aa1' + ([BitConverter]::ToString($bytes).Replace('-','').ToLowerInvariant())
}
if (!(Test-Path -LiteralPath $envPath)) {
    @(
        '# LOCAL ONLY. Do not commit or reuse in production.'
        "BG_DB_PASSWORD=$(New-LocalSecret)"
        "BG_DB_ROOT_PASSWORD=$(New-LocalSecret)"
        "BG_JWT_SECRET=$(New-LocalSecret)"
        "BG_OWNER_PASSWORD=$(New-LocalSecret)"
    ) | Set-Content -LiteralPath $envPath -Encoding ascii
}
Write-Host "Local OWNER: bg_owner. Temporary password is BG_OWNER_PASSWORD in $envPath (view privately)."
Write-Host 'The first login requires a new password. The env value is not updated after changing it.'
if ($PrepareOnly) { return }
if (!(Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Install/start Docker Desktop with Linux containers, then rerun this script.' }
& docker info --format '{{.OSType}}' | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Docker engine is unavailable. Start Docker Desktop with Linux containers.' }
$composeArgs = @('compose','--env-file',$envPath,'-p','phoenix-bg-fitness-local','-f',$composePath)
& docker @composeArgs config --quiet
if ($LASTEXITCODE -ne 0) { throw 'Local Compose validation failed.' }
& docker @composeArgs up -d --build --wait --wait-timeout 300
if ($LASTEXITCODE -ne 0) { throw 'Startup failed. Run the documented ps/logs commands; no volumes were deleted.' }
$brand = Invoke-RestMethod 'http://localhost:8083/api/public/branding'
if ($brand.gymName -ne 'B.G. Fitness') { throw 'Unexpected branding. Inspect this dedicated database; do not reset any volume.' }
Write-Host 'B.G. Fitness: http://localhost:5183 | Health: http://localhost:8083/actuator/health'
if (!$NoBrowser) { Start-Process 'http://localhost:5183' -WindowStyle Hidden }
