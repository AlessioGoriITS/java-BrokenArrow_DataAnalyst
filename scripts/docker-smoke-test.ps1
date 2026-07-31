[CmdletBinding()]
param(
    [switch]$KeepRunning,
    [switch]$ResetData
)

$ErrorActionPreference = 'Stop'
$composeArguments = @(
    'compose',
    '--env-file',
    '.env.example',
    '--file',
    'docker-compose.yml'
)
$composeTouched = $false

function Invoke-DockerCompose {
    param([Parameter(Mandatory)][string[]]$Command)

    & docker @composeArguments @Command
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose failed: $($Command -join ' ')"
    }
}

function Wait-ForApplication {
    $deadline = (Get-Date).AddMinutes(3)
    do {
        try {
            $health = Invoke-RestMethod `
                -Uri 'http://localhost:8080/actuator/health' `
                -TimeoutSec 3
            if ($health.status -eq 'UP') {
                return
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    } while ((Get-Date) -lt $deadline)

    throw 'Application did not become healthy within three minutes'
}

docker info *> $null
if ($LASTEXITCODE -ne 0) {
    throw 'Docker Engine is not running'
}

try {
    if ($ResetData) {
        Invoke-DockerCompose -Command @(
            'down',
            '--volumes',
            '--remove-orphans'
        )
    }

    $composeTouched = $true
    Invoke-DockerCompose -Command @('up', '--build', '--detach')
    Wait-ForApplication

    $loginBody = @{
        username = 'admin'
        password = 'Admin123!'
    } | ConvertTo-Json
    $login = Invoke-RestMethod `
        -Method Post `
        -Uri 'http://localhost:8080/api/auth/login' `
        -ContentType 'application/json' `
        -Body $loginBody
    if ([string]::IsNullOrWhiteSpace($login.accessToken)) {
        throw 'Admin login did not return an access token'
    }

    $catalog = Invoke-RestMethod `
        -Uri 'http://localhost:8080/api/units?page=0&size=1'
    if ($catalog.totalElements -lt 1) {
        throw 'Unit catalog is empty'
    }

    Write-Host 'Docker smoke test passed:'
    Write-Host '  application health: UP'
    Write-Host '  admin login: OK'
    Write-Host "  catalog units: $($catalog.totalElements)"
} finally {
    if ($composeTouched -and -not $KeepRunning) {
        Invoke-DockerCompose -Command @('down', '--remove-orphans')
    }
}
