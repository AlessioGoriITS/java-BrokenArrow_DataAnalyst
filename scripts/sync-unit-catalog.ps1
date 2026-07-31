param(
    [string]$OutputPath = "src/main/resources/catalog/units.json"
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$apiBase = "https://barmory.net"
$apiVersion = "8.4"
$datasetVersion = "barmory-$apiVersion-2026.07"
$clientId = [guid]::NewGuid().ToString()

$pulseHeaders = @{
    "Accept" = "application/json"
    "Content-Type" = "application/json"
    "X-Barmory-ID" = $clientId
    "X-Type" = "NONE"
    "X-Barmory-Version" = $apiVersion
}
$pulse = Invoke-RestMethod -Method Post -Uri "$apiBase/pulse" `
    -Headers $pulseHeaders -Body "{}"

$headers = @{
    "Accept" = "application/json"
    "Content-Type" = "application/json"
    "X-Barmory-ID" = $clientId
    "X-Type" = "data"
    "X-Barmory-Version" = $apiVersion
    "X-Barmory-Attest" = $pulse.token
}

function Invoke-CatalogRequest {
    param([Parameter(Mandatory)][string]$Path)

    if ([DateTimeOffset]::UtcNow.ToUnixTimeSeconds() -ge
        ([long]$script:pulse.expiresAt - 20)) {
        $script:pulse = Invoke-RestMethod -Method Post -Uri "$script:apiBase/pulse" `
            -Headers $script:pulseHeaders -Body "{}"
        $script:headers["X-Barmory-Attest"] = $script:pulse.token
    }

    $payload = $null
    for ($attempt = 1; $attempt -le 8; $attempt++) {
        try {
            Start-Sleep -Milliseconds 120
            $payload = Invoke-RestMethod -Uri "$script:apiBase/$Path" `
                -Headers $script:headers
            break
        } catch {
            $statusCode = $_.Exception.Response.StatusCode.value__
            if ($statusCode -ne 429 -or $attempt -eq 8) { throw }
            $retryAfter = $_.Exception.Response.Headers["Retry-After"]
            $delay = if ($retryAfter) {
                [math]::Max(1, [int]$retryAfter)
            } else {
                [math]::Min(30, [math]::Pow(2, $attempt))
            }
            Write-Warning "BArmory rate limit su $Path; nuovo tentativo tra $delay s."
            Start-Sleep -Seconds $delay
        }
    }
    if (($payload -is [string]) -and
        ($payload.TrimStart().StartsWith("[") -or
         $payload.TrimStart().StartsWith("{"))) {
        $normalizedPayload = $payload `
            -replace ',"flyPresetId":[^,}\]]*', '' `
            -replace ',"isUnderbarrel":[^,}\]]*', ''
        return $normalizedPayload | ConvertFrom-Json
    }
    return $payload
}

$existing = Get-Content $OutputPath -Raw | ConvertFrom-Json
$curatedByName = @{}
foreach ($unit in $existing.units) {
    if (-not $unit.externalUnitId.StartsWith("ba_")) {
        $curatedByName[$unit.name.Trim().ToLowerInvariant()] = $unit
    }
}

$specializationAliases = @{
    "US Armored Brigade" = "Armored Brigade"
    "US Airborne Brigade" = "Airborne infantry"
    "RU Guards Tank Brigade" = "Guard Tank Brigade"
    "RU Guards Air Assault Brigade" = "VDV Brigade"
}
$factions = @{ 1 = "RUS"; 2 = "USA" }
$categories = @{
    0 = "RECON"
    1 = "INFANTRY"
    2 = "VEHICLE"
    3 = "SUPPORT"
    4 = "LOGISTICS"
    5 = "HELICOPTER"
    6 = "AIRCRAFT"
}

$specializationResponse = Invoke-CatalogRequest -Path "spec/all"
$sourceSpecializations = [System.Collections.Generic.List[object]]::new()
foreach ($specialization in $specializationResponse) {
    if ($specialization.ShowInHangar -and
        $factions.ContainsKey([int]$specialization.CountryId)) {
        $sourceSpecializations.Add($specialization)
    }
}
$roster = Invoke-CatalogRequest -Path "unit/all"

$unitsById = @{}
$specializationsByUnitId = @{}
foreach ($specialization in $sourceSpecializations) {
    $members = Invoke-CatalogRequest `
        -Path "spec/$($specialization.Id)/$($specialization.Id)/units"
    foreach ($unit in $members) {
        if (-not $unit.DisplayInArmory) {
            continue
        }
        $id = [int]$unit.Id
        $unitsById[$id] = $unit
        if (-not $specializationsByUnitId.ContainsKey($id)) {
            $specializationsByUnitId[$id] = [System.Collections.Generic.List[string]]::new()
        }
        $specializationsByUnitId[$id].Add($specialization.Name.Trim())
    }
}

foreach ($summary in $roster) {
    $id = [int]$summary.Id
    if (-not $unitsById.ContainsKey($id)) {
        if ([int]$summary.SpecId -lt 0) {
            continue
        }
        $unit = Invoke-CatalogRequest -Path "unit/$id"
        if ($unit.DisplayInArmory) {
            $fallbackSpecialization = $sourceSpecializations |
                Where-Object Id -eq $summary.SpecId |
                Select-Object -First 1
            if ($null -eq $fallbackSpecialization) {
                throw "Specializzazione $($summary.SpecId) non trovata per l'unità $id."
            }
            $unitsById[$id] = $unit
            $specializationsByUnitId[$id] = [System.Collections.Generic.List[string]]::new()
            $specializationsByUnitId[$id].Add($fallbackSpecialization.Name.Trim())
        }
    }
}

# BA Data lists visible loadout variants that BArmory intentionally omits from
# the base roster. Include every USA/RUS card that still resolves through the
# BArmory unit endpoint; inaccessible pilot placeholders are ignored.
$hangarHtml = (Invoke-WebRequest -Uri "https://ba.puliaev.com/hangar").Content
$hangarPattern = '<div class="unit-card[^>]*data-name="[^"]*"[^>]*data-country="(?<country>[^"]*)"\s*data-type="(?<type>[^"]*)"[\s\S]*?<img src="[^"]*" alt="(?<name>[^"]*)"[\s\S]*?<a href="/unit/(?<id>\d+)"'
$hangarCards = [regex]::Matches($hangarHtml, $hangarPattern)
foreach ($card in $hangarCards) {
    $countryId = [int]$card.Groups["country"].Value
    if (-not $factions.ContainsKey($countryId)) {
        continue
    }
    $id = [int]$card.Groups["id"].Value
    if ($unitsById.ContainsKey($id)) {
        continue
    }
    try {
        $unit = Invoke-CatalogRequest -Path "unit/$id"
        if ($unit.DisplayInArmory) {
            $unitsById[$id] = $unit
            $specializationsByUnitId[$id] = [System.Collections.Generic.List[string]]::new()
        }
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 404) {
            throw
        }
    }
}

$usedExternalIds = [System.Collections.Generic.HashSet[string]]::new()
$sourceNames = [System.Collections.Generic.HashSet[string]]::new()
$outputUnits = [System.Collections.Generic.List[object]]::new()

foreach ($unit in ($unitsById.Values | Sort-Object Name, Id)) {
    $name = $unit.Name.Trim()
    $nameKey = $name.ToLowerInvariant()
    $null = $sourceNames.Add($nameKey)
    $curated = $curatedByName[$nameKey]
    $externalUnitId = "ba_$($unit.Id)"
    if ($null -ne $curated -and $usedExternalIds.Add($curated.externalUnitId)) {
        $externalUnitId = $curated.externalUnitId
    } else {
        $null = $usedExternalIds.Add($externalUnitId)
    }

    $thumbnail = $unit.ThumbnailFileName
    $imageUrl = $null
    if (-not [string]::IsNullOrWhiteSpace($thumbnail)) {
        $thumbnail = $thumbnail.ToUpperInvariant() `
            -replace "-LABEL", "" `
            -replace "-OPTICON", "" `
            -replace "-WEAPONICON", ""
        $thumbnail = ($thumbnail -replace "\.PNG$", "") + ".png"
        $imageUrl = "https://ik.imagekit.io/ywfw1k0dn/units/$thumbnail"
    }

    $category = $categories[[int]$unit.CategoryType]
    $faction = $factions[[int]$unit.CountryId]

    $armorData = $null
    $mobilityData = $null
    $weapons = [System.Collections.Generic.List[object]]::new()
    try {
        $armorData = Invoke-CatalogRequest -Path "unit/$($unit.Id)/armor"
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 404) { throw }
    }
    try {
        $mobilityId = Invoke-CatalogRequest `
            -Path "unit/$($unit.Id)/default/mobility"
        if ($null -ne $mobilityId -and [int]$mobilityId -gt 0) {
            $mobilityData = Invoke-CatalogRequest -Path "mobility/$mobilityId"
            if ($null -ne $mobilityData.FlyPresetId -and
                [int]$mobilityData.FlyPresetId -gt 0) {
                $mobilityData.FlyPreset = Invoke-CatalogRequest `
                    -Path "flypreset/$($mobilityData.FlyPresetId)"
            }
        }
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 404) { throw }
    }
    try {
        $directWeapons = Invoke-CatalogRequest -Path "unit/$($unit.Id)/weapons"
        foreach ($weapon in @($directWeapons)) {
            if ($null -ne $weapon) { $weapons.Add($weapon) }
        }
        $turrets = Invoke-CatalogRequest `
            -Path "unit/$($unit.Id)/default/turrets"
        $turretIds = @($turrets | ForEach-Object Id | Where-Object { $_ }) -join ","
        if (-not [string]::IsNullOrWhiteSpace($turretIds)) {
            $turretWeapons = Invoke-CatalogRequest `
                -Path "turrets/$turretIds/weapons"
            foreach ($weapon in @($turretWeapons)) {
                if ($null -ne $weapon) { $weapons.Add($weapon) }
            }
        }
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -notin @(400, 404)) { throw }
    }

    $speed = $null
    if ($null -ne $mobilityData) {
        if ($null -ne $mobilityData.FlyPreset -and
            $null -ne $mobilityData.FlyPreset.MaxSpeed) {
            $speed = [math]::Round(
                [double]$mobilityData.FlyPreset.MaxSpeed * 3.6,
                2
            )
        } elseif ($null -ne $mobilityData.MaxSpeedRoad) {
            $speed = [math]::Round([double]$mobilityData.MaxSpeedRoad, 2)
        }
    }
    $armorLabel = "UNARMORED"
    if ($null -ne $armorData) {
        $armorLabel = "KIN $($armorData.KinArmorFront)/$($armorData.KinArmorSides)/$($armorData.KinArmorRear) | HEAT $($armorData.HeatArmorFront)/$($armorData.HeatArmorSides)/$($armorData.HeatArmorRear)"
    }
    $mainWeapon = $weapons |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_.HUDName) } |
        Select-Object -First 1 -ExpandProperty HUDName
    if ([string]::IsNullOrWhiteSpace($mainWeapon)) {
        $mainWeapon = "UNARMED"
    }
    $dimensions = "$($unit.Length) x $($unit.Width) x $($unit.Height) m"
    $weightTonnes = [math]::Round([double]$unit.Weight / 1000, 1)
    $outputUnits.Add([ordered]@{
        externalUnitId = $externalUnitId
        name = $name
        faction = $faction
        category = $category
        baseCost = [int]$unit.Cost
        description = if ($curated.description) { $curated.description } else {
            "Unità $category del roster $faction. Dimensioni $dimensions; peso $weightTonnes t. Dati tecnici sincronizzati dal catalogo pubblico BArmory."
        }
        hitPoints = if ($null -ne $armorData) {
            [int]$armorData.MaxHealthPoints
        } elseif ($null -ne $curated) { $curated.hitPoints } else { 0 }
        speed = if ($null -ne $speed) {
            $speed
        } elseif ($null -ne $curated) { $curated.speed } else { 0 }
        armor = $armorLabel
        mainWeapon = $mainWeapon.Trim()
        imageUrl = $imageUrl
        specializations = @($specializationsByUnitId[[int]$unit.Id] | Sort-Object -Unique)
    })
}

foreach ($curated in $curatedByName.Values | Sort-Object name) {
    $nameKey = $curated.name.Trim().ToLowerInvariant()
    if ($sourceNames.Contains($nameKey)) {
        continue
    }
    $mappedSpecializations = @($curated.specializations | ForEach-Object {
        if ($specializationAliases.ContainsKey($_)) { $specializationAliases[$_] } else { $_ }
    } | Sort-Object -Unique)
    $outputUnits.Add([ordered]@{
        externalUnitId = $curated.externalUnitId
        name = $curated.name
        faction = $curated.faction
        category = $curated.category
        baseCost = $curated.baseCost
        description = $curated.description
        hitPoints = $curated.hitPoints
        speed = $curated.speed
        armor = $curated.armor
        mainWeapon = $curated.mainWeapon
        imageUrl = $curated.imageUrl
        specializations = $mappedSpecializations
    })
}

$outputSpecializations = @($sourceSpecializations | Sort-Object CountryId, Name | ForEach-Object {
    $faction = $factions[[int]$_.CountryId]
    [ordered]@{
        name = $_.Name.Trim()
        faction = $faction
        description = "Specializzazione $faction del roster pubblico BArmory (ID $($_.Id))."
    }
})

$dataset = [ordered]@{
    version = $datasetVersion
    specializations = $outputSpecializations
    units = @($outputUnits | Sort-Object name, externalUnitId)
}

$dataset | ConvertTo-Json -Depth 8 | Set-Content $OutputPath -Encoding utf8
Write-Output "Catalogo aggiornato: $($dataset.units.Count) unità, $($dataset.specializations.Count) specializzazioni."
