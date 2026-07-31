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

$specializationResponse = Invoke-RestMethod -Uri "$apiBase/spec/all" `
    -Headers $headers
$sourceSpecializations = [System.Collections.Generic.List[object]]::new()
foreach ($specialization in $specializationResponse) {
    if ($specialization.ShowInHangar -and
        $factions.ContainsKey([int]$specialization.CountryId)) {
        $sourceSpecializations.Add($specialization)
    }
}
$roster = Invoke-RestMethod -Uri "$apiBase/unit/all" -Headers $headers

$unitsById = @{}
$specializationsByUnitId = @{}
foreach ($specialization in $sourceSpecializations) {
    $members = Invoke-RestMethod `
        -Uri "$apiBase/spec/$($specialization.Id)/$($specialization.Id)/units" `
        -Headers $headers
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
        $unit = Invoke-RestMethod -Uri "$apiBase/unit/$id" -Headers $headers
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
        $unit = Invoke-RestMethod -Uri "$apiBase/unit/$id" -Headers $headers
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
    $outputUnits.Add([ordered]@{
        externalUnitId = $externalUnitId
        name = $name
        faction = $faction
        category = $category
        baseCost = [int]$unit.Cost
        description = if ($curated.description) { $curated.description } else {
            "Unità $category del roster $faction. Dati identificativi e costo sincronizzati dal catalogo pubblico BArmory."
        }
        hitPoints = if ($null -ne $curated) { $curated.hitPoints } else { $null }
        speed = if ($null -ne $curated) { $curated.speed } else { $null }
        armor = if ($null -ne $curated) { $curated.armor } else { $null }
        mainWeapon = if ($null -ne $curated) { $curated.mainWeapon } else { $null }
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
