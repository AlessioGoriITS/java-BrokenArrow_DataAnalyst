import { api } from "./api.js";
import { drawLineChart } from "./charts.js";

const state = {
    route: "dashboard",
    dashboardLoaded: false,
    analyticsLoaded: false,
    analyticsTab: "units",
    analytics: { units: [], maps: [], specializations: [] },
    unitPage: 0,
    unitLayout: "grid",
    playerProfile: null,
    playerLoading: false,
    trend: []
};

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

const escapeHtml = value => String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");

const formatNumber = value => new Intl.NumberFormat("it-IT").format(value ?? 0);
const formatOptionalNumber = value => value === null || value === undefined
    ? "—"
    : formatNumber(value);
const formatDecimal = (value, digits = 2) => value === null || value === undefined
    ? "—"
    : Number(value).toLocaleString("it-IT", { maximumFractionDigits: digits });
const formatDate = value => value
    ? new Date(value).toLocaleDateString("it-IT", { day: "2-digit", month: "short", year: "numeric" })
    : "—";
const initials = value => String(value || "Commander")
    .split(/\s+/)
    .slice(0, 2)
    .map(part => part[0])
    .join("")
    .toUpperCase();
const metricNumber = metric => metric?.value ?? null;
const metricText = (metric, suffix = "") => metricNumber(metric) === null
    ? "N/D"
    : `${formatDecimal(metric.value)}${suffix}`;
const metricClass = metric => metricNumber(metric) === null
    ? "muted"
    : Number(metric.value) >= 1 || Number(metric.value) >= 50 ? "good" : "warn";
const categoryGlyph = category => ({
    TANK: "TNK", IFV: "IFV", RECON: "RCN", ARTILLERY: "ART",
    AIR_DEFENSE: "ADA", INFANTRY: "INF", HELICOPTER: "HEL",
    AIRCRAFT: "AIR", SUPPORT: "SUP", VEHICLE: "VHC", LOGISTICS: "LOG"
}[category] || String(category || "UNIT").slice(0, 3));

function showToast(message, type = "info") {
    const toast = document.createElement("div");
    toast.className = `toast ${type}`;
    toast.textContent = message;
    $("#toast-region").append(toast);
    window.setTimeout(() => toast.remove(), 4200);
}

function emptyMarkup(title, detail) {
    return `<div class="empty-state"><div><strong>${escapeHtml(title)}</strong><p>${escapeHtml(detail)}</p></div></div>`;
}

function setSystemState(mode, label) {
    const node = $("#system-state");
    node.classList.remove("is-online", "is-offline");
    if (mode) node.classList.add(`is-${mode}`);
    $("span:last-child", node).textContent = label;
}

function currentRoute() {
    const route = location.hash.replace(/^#/, "").split("?")[0];
    return ["dashboard", "hangar", "analytics", "player"].includes(route)
        ? route
        : "dashboard";
}

function navigate() {
    state.route = currentRoute();
    $$("[data-view]").forEach(view => view.classList.toggle(
        "is-active",
        view.dataset.view === state.route
    ));
    $$('[data-route]').forEach(link => link.classList.toggle(
        "is-active",
        link.dataset.route === state.route
    ));
    $("#main-content").focus?.({ preventScroll: true });
    window.scrollTo({ top: 0, behavior: "auto" });
    $("#menu-button").setAttribute("aria-expanded", "false");
    $(".main-nav").classList.remove("is-open");

    if (state.route === "hangar") loadHangar(state.unitPage);
    if (state.route === "analytics") loadAnalytics();
    if (state.route === "player" && !state.playerProfile && !state.playerLoading) {
        const steamId = localStorage.getItem("battle-debrief-steam-id");
        if (steamId) loadPlayerConsole(steamId);
    }
}

async function checkHealth() {
    try {
        const health = await api.health();
        setSystemState(health.status === "UP" ? "online" : "offline", health.status);
    } catch {
        setSystemState("offline", "OFFLINE");
    }
}

async function loadDashboard() {
    if (state.dashboardLoaded) return;
    try {
        const [unitPage, unitStats, mapStats, specializationStats] = await Promise.all([
            api.units({ page: 0, size: 1, sort: "name,asc" }),
            api.unitAnalytics(),
            api.mapAnalytics(),
            api.specializationAnalytics()
        ]);
        state.analytics = {
            units: unitStats,
            maps: mapStats,
            specializations: specializationStats
        };
        state.analyticsLoaded = true;
        state.dashboardLoaded = true;

        const firstUnit = unitPage.content?.[0];
        const datasetMatches = Math.max(
            unitStats[0]?.datasetMatches || 0,
            mapStats[0]?.datasetMatches || 0,
            specializationStats[0]?.datasetMatches || 0
        );
        $("#dataset-version").textContent = firstUnit?.datasetVersion || "NO ASSETS";
        $("#dataset-matches").textContent = `${formatNumber(datasetMatches)} MATCH`;
        $("#kpi-units").textContent = formatNumber(unitPage.totalElements);
        $("#kpi-maps").textContent = formatNumber(mapStats.length);
        $("#hangar-total").textContent = formatNumber(unitPage.totalElements);

        const bestWinRate = [...unitStats]
            .filter(item => metricNumber(item.winRate) !== null)
            .sort((a, b) => b.winRate.value - a.winRate.value)[0];
        const bestKd = [...unitStats]
            .filter(item => metricNumber(item.economicKd) !== null)
            .sort((a, b) => b.economicKd.value - a.economicKd.value)[0];
        $("#kpi-winrate").textContent = bestWinRate
            ? metricText(bestWinRate.winRate, "%")
            : "N/D";
        $("#kpi-winrate-label").textContent = bestWinRate?.unitName || "nessun campione";
        $("#kpi-kd").textContent = bestKd ? metricText(bestKd.economicKd) : "N/D";
        $("#kpi-kd-label").textContent = bestKd?.unitName || "nessun campione";

        renderTopUnits(unitStats);
        renderMapPopularity(mapStats);
        renderSpecializationOverview(specializationStats);
    } catch (error) {
        renderDashboardFailure(error);
    }
}

function renderTopUnits(items) {
    const container = $("#top-units-chart");
    container.classList.remove("loading-block");
    const ranked = [...items]
        .filter(item => metricNumber(item.deploymentEfficiency) !== null)
        .sort((a, b) => b.deploymentEfficiency.value - a.deploymentEfficiency.value)
        .slice(0, 6);
    if (!ranked.length) {
        container.innerHTML = emptyMarkup(
            "Nessuna telemetria unità",
            "Importa una partita per generare il ranking di efficienza."
        );
        return;
    }
    const max = Math.max(...ranked.map(item => item.deploymentEfficiency.value), 1);
    container.innerHTML = ranked.map((item, index) => `
        <div class="rank-row">
            <span class="rank-index">${String(index + 1).padStart(2, "0")}</span>
            <span class="rank-name"><strong>${escapeHtml(item.unitName)}</strong><small>${escapeHtml(item.category)} · ${escapeHtml(item.faction)}</small></span>
            <span class="bar-track"><i class="bar-fill" style="width:${Math.max(4, item.deploymentEfficiency.value / max * 100)}%"></i></span>
            <span class="rank-value">${formatDecimal(item.deploymentEfficiency.value)}×</span>
        </div>`).join("");
}

function renderMapPopularity(items) {
    const container = $("#map-popularity");
    container.classList.remove("loading-block");
    if (!items.length) {
        container.innerHTML = emptyMarkup("Nessuna mappa", "Il dataset non contiene ancora partite.");
        return;
    }
    container.innerHTML = items.slice(0, 5).map(item => `
        <div class="compact-row"><span><strong>${escapeHtml(item.mapName)}</strong><small>${formatNumber(item.sampleMatches)} match · ${formatNumber(item.samplePlayers)} player</small></span><b>${metricText(item.playRate, "%")}</b></div>
    `).join("");
}

function renderSpecializationOverview(items) {
    const container = $("#specialization-overview");
    container.classList.remove("loading-block");
    if (!items.length) {
        container.innerHTML = emptyMarkup("Nessuna formazione", "Servono unità schierate con specializzazione.");
        return;
    }
    container.innerHTML = items.slice(0, 5).map(item => `
        <div class="spec-row"><span class="faction-chip">${escapeHtml(item.faction.slice(0, 3))}</span><span><strong>${escapeHtml(item.specializationName)}</strong><small>${formatNumber(item.sampleUnits)} unità · ${formatNumber(item.sampleMatches)} match</small></span><b>${metricText(item.winRate, "%")}</b></div>
    `).join("");
}

function renderDashboardFailure(error) {
    ["#top-units-chart", "#map-popularity", "#specialization-overview"].forEach(selector => {
        const container = $(selector);
        container.classList.remove("loading-block");
        container.innerHTML = emptyMarkup("Link dati interrotto", error.message);
    });
}

function readUnitFilters() {
    return {
        name: $("#unit-search").value.trim(),
        faction: $("#unit-faction").value,
        category: $("#unit-category").value,
        specializationId: $("#unit-specialization").value,
        maxCost: $("#unit-max-cost").value,
        page: state.unitPage,
        size: 12,
        sort: "name,asc"
    };
}

async function loadHangar(page = 0) {
    state.unitPage = page;
    const grid = $("#unit-grid");
    grid.innerHTML = Array.from({ length: 8 }, () => '<div class="unit-card loading-block"></div>').join("");
    try {
        const response = await api.units(readUnitFilters());
        $("#hangar-total").textContent = formatNumber(response.totalElements);
        $("#unit-results-label").textContent = `${formatNumber(response.totalElements)} ASSET TROVATI / PAGINA ${response.page + 1}`;
        renderUnitCards(response.content);
        renderPagination(response);
        populateCategories(response.content);
    } catch (error) {
        grid.innerHTML = emptyMarkup("Hangar non disponibile", error.message);
        $("#unit-pagination").innerHTML = "";
    }
}

async function loadUnitFilterOptions() {
    try {
        const [response, specializations] = await Promise.all([
            api.units({ page: 0, size: 100, sort: "category,asc" }),
            api.specializations()
        ]);
        populateCategories(response.content);
        populateSpecializations(specializations);
    } catch {
        // The Hangar itself will expose a visible error if the API is offline.
    }
}

function populateSpecializations(specializations) {
    const select = $("#unit-specialization");
    const current = select.value;
    select.innerHTML = '<option value="">Tutte</option>';
    [...specializations]
        .sort((left, right) => left.name.localeCompare(right.name, "it"))
        .forEach(specialization => {
            const option = document.createElement("option");
            option.value = specialization.id;
            option.textContent = `${specialization.name} · ${specialization.faction}`;
            select.append(option);
        });
    select.value = current;
}

function populateCategories(units) {
    const select = $("#unit-category");
    const current = select.value;
    const known = new Set($$("option", select).map(option => option.value));
    units.map(unit => unit.category).filter(Boolean).sort().forEach(category => {
        if (known.has(category)) return;
        const option = document.createElement("option");
        option.value = category;
        option.textContent = category.replaceAll("_", " ");
        select.append(option);
        known.add(category);
    });
    select.value = current;
}

function unitCard(unit) {
    const image = unit.imageUrl
        ? `<img src="${escapeHtml(unit.imageUrl)}" alt="" loading="lazy">`
        : `<span class="unit-glyph">${escapeHtml(categoryGlyph(unit.category))}</span>`;
    return `
        <article class="unit-card" tabindex="0" data-unit-id="${unit.id}" aria-label="Apri ${escapeHtml(unit.name)}">
            <div class="unit-visual">${image}<span class="unit-category">${escapeHtml(unit.category.replaceAll("_", " "))}</span><span class="unit-faction">${escapeHtml(unit.faction)}</span></div>
            <div class="unit-card-body">
                <h3>${escapeHtml(unit.name)}</h3><span class="unit-code">${escapeHtml(unit.externalUnitId)}</span>
                <div class="unit-stats"><div><span>HP</span><strong>${formatOptionalNumber(unit.hitPoints)}</strong></div><div><span>SPEED</span><strong>${formatDecimal(unit.speed, 1)}</strong></div><div><span>ARMOR</span><strong>${escapeHtml(unit.armor || "—")}</strong></div></div>
                <div class="unit-card-foot"><span>${escapeHtml(unit.mainWeapon || "Sistema non censito")}</span><strong>${formatNumber(unit.baseCost)}<small> PT</small></strong></div>
            </div>
        </article>`;
}

function renderUnitCards(units) {
    const grid = $("#unit-grid");
    grid.classList.toggle("is-list", state.unitLayout === "list");
    grid.innerHTML = units.length
        ? units.map(unitCard).join("")
        : emptyMarkup("Nessun asset trovato", "Modifica i filtri e riprova.");
}

function renderPagination(page) {
    const container = $("#unit-pagination");
    if (page.totalPages <= 1) {
        container.innerHTML = "";
        return;
    }
    const start = Math.max(0, page.page - 2);
    const end = Math.min(page.totalPages, start + 5);
    const buttons = [];
    buttons.push(`<button type="button" data-page="${page.page - 1}" ${page.first ? "disabled" : ""}>←</button>`);
    for (let index = start; index < end; index += 1) {
        buttons.push(`<button type="button" data-page="${index}" class="${index === page.page ? "is-active" : ""}">${index + 1}</button>`);
    }
    buttons.push(`<button type="button" data-page="${page.page + 1}" ${page.last ? "disabled" : ""}>→</button>`);
    container.innerHTML = buttons.join("");
}

async function openUnitDrawer(unitId) {
    const drawer = $("#unit-drawer");
    const backdrop = $("#drawer-backdrop");
    $("#unit-drawer-content").innerHTML = '<div class="loading-block" style="height:70vh"></div>';
    backdrop.hidden = false;
    drawer.classList.add("is-open");
    drawer.setAttribute("aria-hidden", "false");
    document.body.classList.add("drawer-open");
    try {
        const unit = await api.unit(unitId);
        const image = unit.imageUrl
            ? `<img src="${escapeHtml(unit.imageUrl)}" alt="" style="width:100%;height:100%;object-fit:cover">`
            : `<span class="unit-glyph">${escapeHtml(categoryGlyph(unit.category))}</span>`;
        $("#unit-drawer-content").innerHTML = `
            <div class="drawer-visual">${image}</div>
            <p class="drawer-meta">${escapeHtml(unit.faction)} / ${escapeHtml(unit.category)} / ${escapeHtml(unit.externalUnitId)}</p>
            <h2>${escapeHtml(unit.name)}</h2>
            <p class="drawer-description">${escapeHtml(unit.description || "Asset del catalogo operativo Battle Debrief.")}</p>
            <div class="drawer-stats">
                ${drawerStat("COSTO", `${formatNumber(unit.baseCost)} PT`)}
                ${drawerStat("HIT POINTS", formatOptionalNumber(unit.hitPoints))}
                ${drawerStat("VELOCITÀ", formatDecimal(unit.speed, 1))}
                ${drawerStat("CORAZZATURA", unit.armor || "—")}
                ${drawerStat("ARMA PRINCIPALE", unit.mainWeapon || "—")}
                ${drawerStat("DATASET", unit.datasetVersion)}
            </div>
            <div class="drawer-section"><h3>SPECIALIZZAZIONI COMPATIBILI</h3><div class="tag-list">${unit.specializations.length ? unit.specializations.map(spec => `<span class="tag">${escapeHtml(spec.name)} · ${escapeHtml(spec.faction)}</span>`).join("") : '<span class="tag">Nessuna associazione</span>'}</div></div>`;
    } catch (error) {
        $("#unit-drawer-content").innerHTML = emptyMarkup("Dettaglio non disponibile", error.message);
    }
}

const drawerStat = (label, value) => `<div class="drawer-stat"><span>${escapeHtml(label)}</span><strong>${escapeHtml(value ?? "—")}</strong></div>`;

function closeUnitDrawer() {
    $("#unit-drawer").classList.remove("is-open");
    $("#unit-drawer").setAttribute("aria-hidden", "true");
    $("#drawer-backdrop").hidden = true;
    document.body.classList.remove("drawer-open");
}

async function loadAnalytics() {
    if (!state.analyticsLoaded) {
        try {
            const [units, maps, specializations] = await Promise.all([
                api.unitAnalytics(), api.mapAnalytics(), api.specializationAnalytics()
            ]);
            state.analytics = { units, maps, specializations };
            state.analyticsLoaded = true;
        } catch (error) {
            $("#analytics-table-body").innerHTML = `<tr><td>${escapeHtml(error.message)}</td></tr>`;
            return;
        }
    }
    renderAnalytics();
}

const analyticsDefinitions = {
    units: {
        code: "UNIT PERFORMANCE",
        heading: "Rendimento delle unità",
        columns: ["Asset", "Campione", "Play rate", "Win rate", "Economic K/D", "Efficienza", "Survival"],
        name: item => `${item.unitName} ${item.externalUnitId} ${item.faction} ${item.category}`,
        row: item => `<tr><td>${escapeHtml(item.unitName)}<span class="table-sub">${escapeHtml(item.faction)} · ${escapeHtml(item.category)}</span></td><td>${formatNumber(item.sampleMatches)}<span class="table-sub">${formatNumber(item.samplePlayers)} player</span></td>${metricCell(item.playRate, "%")}${metricCell(item.winRate, "%")}${metricCell(item.economicKd)}${metricCell(item.deploymentEfficiency)}${metricCell(item.survivalRate, "%")}</tr>`
    },
    maps: {
        code: "THEATRE PERFORMANCE",
        heading: "Rendimento per mappa",
        columns: ["Mappa", "Campione", "Play rate", "Win rate", "Economic K/D", "Damage ratio", "Distrutto"],
        name: item => item.mapName,
        row: item => `<tr><td>${escapeHtml(item.mapName)}</td><td>${formatNumber(item.sampleMatches)}<span class="table-sub">${formatNumber(item.samplePlayers)} player</span></td>${metricCell(item.playRate, "%")}${metricCell(item.winRate, "%")}${metricCell(item.economicKd)}${metricCell(item.damageRatio)}<td>${formatNumber(item.destroyedValue)}</td></tr>`
    },
    specializations: {
        code: "FORMATION PERFORMANCE",
        heading: "Rendimento delle specializzazioni",
        columns: ["Specializzazione", "Campione", "Play rate", "Win rate", "Economic K/D", "Efficienza", "Survival"],
        name: item => `${item.specializationName} ${item.faction}`,
        row: item => `<tr><td>${escapeHtml(item.specializationName)}<span class="table-sub">${escapeHtml(item.faction)} · ${formatNumber(item.sampleUnits)} unità</span></td><td>${formatNumber(item.sampleMatches)}<span class="table-sub">${formatNumber(item.samplePlayers)} player</span></td>${metricCell(item.playRate, "%")}${metricCell(item.winRate, "%")}${metricCell(item.economicKd)}${metricCell(item.deploymentEfficiency)}${metricCell(item.survivalRate, "%")}</tr>`
    }
};

function metricCell(metric, suffix = "") {
    const title = metricNumber(metric) === null ? metric?.status || "NO_DATA" : "VALID";
    return `<td><span class="metric-value ${metricClass(metric)}" title="${escapeHtml(title)}">${metricText(metric, suffix)}</span></td>`;
}

function renderAnalytics() {
    const definition = analyticsDefinitions[state.analyticsTab];
    const search = $("#analytics-search").value.trim().toLowerCase();
    const items = state.analytics[state.analyticsTab]
        .filter(item => definition.name(item).toLowerCase().includes(search));
    $("#analytics-code").textContent = definition.code;
    $("#analytics-heading").textContent = definition.heading;
    $("#analytics-table-head").innerHTML = `<tr>${definition.columns.map(column => `<th>${escapeHtml(column)}</th>`).join("")}</tr>`;
    $("#analytics-table-body").innerHTML = items.length
        ? items.map(definition.row).join("")
        : `<tr><td colspan="${definition.columns.length}">${emptyMarkup("Nessun dato disponibile", "Importa partite o modifica il filtro.")}</td></tr>`;
    renderAnalyticsSummary(items);
}

function renderAnalyticsSummary(items) {
    const sampleMatches = Math.max(...items.map(item => item.datasetMatches || 0), 0);
    const validWinRates = items.map(item => metricNumber(item.winRate)).filter(value => value !== null);
    const validKds = items.map(item => metricNumber(item.economicKd)).filter(value => value !== null);
    const average = values => values.length
        ? values.reduce((sum, value) => sum + Number(value), 0) / values.length
        : null;
    $("#analytics-summary").innerHTML = [
        ["RIGHE ANALIZZATE", formatNumber(items.length)],
        ["MATCH NEL DATASET", formatNumber(sampleMatches)],
        ["WIN RATE MEDIO", average(validWinRates) === null ? "N/D" : `${formatDecimal(average(validWinRates))}%`],
        ["ECONOMIC K/D MEDIO", average(validKds) === null ? "N/D" : formatDecimal(average(validKds))]
    ].map(([label, value]) => `<article class="summary-card"><span>${label}</span><strong>${value}</strong></article>`).join("");
}

async function searchSteamPlayer(event) {
    event.preventDefault();
    const button = $("button[type='submit']", event.currentTarget);
    const errorNode = $("#login-error");
    errorNode.textContent = "";
    button.disabled = true;
    button.textContent = "Recupero dati…";
    const steamId = $("#steam-id").value.trim();
    try {
        await loadPlayerConsole(steamId);
        localStorage.setItem("battle-debrief-steam-id", steamId);
        showToast("Debrief Steam caricato", "success");
    } catch (error) {
        errorNode.textContent = error.message;
    } finally {
        button.disabled = false;
        button.textContent = "Carica debrief";
    }
}

function showPlayerConsole() {
    $("#player-guest").hidden = true;
    $("#player-console").hidden = false;
}

function showSteamSearch() {
    state.playerProfile = null;
    $("#player-guest").hidden = false;
    $("#player-console").hidden = true;
    $("#steam-id").focus();
}

function changeSteamPlayer() {
    localStorage.removeItem("battle-debrief-steam-id");
    showSteamSearch();
    location.hash = "player";
}

async function loadPlayerConsole(steamId) {
    state.playerLoading = true;
    try {
        showPlayerConsole();
        $("#player-display-name").textContent = "Recupero dati…";
        $("#player-matches").innerHTML = '<tr><td colspan="7">Sincronizzazione della cronologia in corso…</td></tr>';
        const profile = await api.steamPlayer(steamId);
        state.playerProfile = profile;
        const trend = [...profile.recentMatches]
            .reverse()
            .map(match => ({ ...match, startedAt: match.endedAt }));
        state.trend = trend;
        renderPlayerHeader(profile);
        renderPlayerMetrics(profile);
        renderPlayerTrend(trend);
        renderPlayerUnits(profile.mostUsedUnits);
        renderPlayerMatches(profile.recentMatches);
    } catch (error) {
        showSteamSearch();
        $("#steam-id").value = steamId;
        $("#login-error").textContent = error.message;
        localStorage.removeItem("battle-debrief-steam-id");
        throw error;
    } finally {
        state.playerLoading = false;
    }
}

function renderPlayerHeader(profile) {
    $("#player-avatar").textContent = initials(profile.displayName);
    $("#player-display-name").textContent = profile.displayName;
    $("#player-handle").textContent = `STEAM ${profile.steamId} · LVL ${profile.level} · RANK #${profile.leaderboardRank || "—"}`;
    $("#player-current-elo").textContent = formatDecimal(profile.currentRating, 0);
    const matches = profile.recentMatches;
    const latest = matches[0];
    const oldest = matches[matches.length - 1];
    const delta = latest?.newRating != null && oldest?.oldRating != null
        ? Number(latest.newRating) - Number(oldest.oldRating)
        : null;
    $("#player-elo-delta").textContent = delta === null
        ? "rating non disponibile"
        : `${delta >= 0 ? "+" : ""}${formatDecimal(delta, 1)} nei match caricati`;
}

function renderPlayerMetrics(profile) {
    const career = profile.career;
    const cards = [
        ["MATCH", formatNumber(career.matches), `${formatNumber(career.wins)} vittorie`],
        ["WIN RATE", career.winRate == null ? "N/D" : `${formatDecimal(career.winRate)}%`, `${formatNumber(career.losses)} sconfitte`],
        ["K/D", formatDecimal(career.kdRatio), `${formatNumber(career.kills)} / ${formatNumber(career.deaths)}`],
        ["ZONE CATTURATE", formatNumber(career.capturedZones), `${formatNumber(career.playTimeSeconds / 3600)} ore giocate`]
    ];
    $("#player-metrics").innerHTML = cards.map(([label, value, detail]) => `
        <article class="metric-card"><span>${label}</span><strong>${value}</strong><small>${detail}</small></article>
    `).join("");
}

function renderPlayerTrend(trend) {
    const canvas = $("#elo-chart");
    const empty = $("#elo-empty");
    if (!trend.length) {
        canvas.hidden = true;
        empty.hidden = false;
        empty.innerHTML = "<div><strong>Nessun trend disponibile</strong><p>BArmory non ha restituito partite recenti con valori ELO.</p></div>";
        return;
    }
    canvas.hidden = false;
    empty.hidden = true;
    window.requestAnimationFrame(() => drawLineChart(canvas, trend));
}

function renderPlayerUnits(units) {
    const container = $("#player-units");
    container.classList.remove("loading-block");
    if (!units.length) {
        container.innerHTML = emptyMarkup("Nessun asset impiegato", "Non risultano unità nei match recenti.");
        return;
    }
    container.innerHTML = [...units]
        .sort((a, b) => b.deployed - a.deployed)
        .slice(0, 6)
        .map(item => `<div class="compact-row"><span><strong>${escapeHtml(item.unitName)}</strong><small>${formatNumber(item.deployed)} schierate · ${formatNumber(item.refunded)} rimborsate</small></span><b>${formatNumber(item.kills)} KILL</b></div>`)
        .join("");
}

function renderPlayerMatches(matches) {
    $("#player-matches").innerHTML = matches.length
        ? matches.map(match => `<tr><td><span class="result-badge ${match.won ? "win" : "loss"}">${match.won ? "W" : "L"}</span></td><td>${escapeHtml(match.mapName)}</td><td>${formatNumber(Math.round(match.durationSeconds / 60))} min</td><td>${formatDecimal(match.oldRating, 0)} → ${formatDecimal(match.newRating, 0)}</td><td>${formatNumber(match.destructionScore)}</td><td>${formatNumber(match.lossesScore)}</td><td>${formatDate(match.endedAt)}</td></tr>`).join("")
        : `<tr><td colspan="7">${emptyMarkup("Nessun after action report", "BArmory non ha restituito partite nelle settimane analizzate.")}</td></tr>`;
}

function bindEvents() {
    window.addEventListener("hashchange", navigate);
    window.addEventListener("resize", () => {
        if (state.route === "player" && state.trend.length) {
            drawLineChart($("#elo-chart"), state.trend);
        }
    });
    $("#menu-button").addEventListener("click", event => {
        const open = event.currentTarget.getAttribute("aria-expanded") === "true";
        event.currentTarget.setAttribute("aria-expanded", String(!open));
        $(".main-nav").classList.toggle("is-open", !open);
    });
    $("#auth-button").addEventListener("click", () => {
        location.hash = "player";
    });
    $("#steam-search-form").addEventListener("submit", searchSteamPlayer);
    $("#change-player").addEventListener("click", changeSteamPlayer);
    $("#unit-filters").addEventListener("submit", event => {
        event.preventDefault();
        loadHangar(0);
    });
    $("#unit-grid").addEventListener("click", event => {
        const card = event.target.closest("[data-unit-id]");
        if (card) openUnitDrawer(card.dataset.unitId);
    });
    $("#unit-grid").addEventListener("keydown", event => {
        const card = event.target.closest("[data-unit-id]");
        if (card && ["Enter", " "].includes(event.key)) {
            event.preventDefault();
            openUnitDrawer(card.dataset.unitId);
        }
    });
    $("#unit-pagination").addEventListener("click", event => {
        const button = event.target.closest("[data-page]");
        if (button && !button.disabled) loadHangar(Number(button.dataset.page));
    });
    $$("[data-layout]").forEach(button => button.addEventListener("click", () => {
        state.unitLayout = button.dataset.layout;
        $$("[data-layout]").forEach(item => item.classList.toggle("is-active", item === button));
        $("#unit-grid").classList.toggle("is-list", state.unitLayout === "list");
    }));
    $$("[data-analytics-tab]").forEach(button => button.addEventListener("click", () => {
        state.analyticsTab = button.dataset.analyticsTab;
        $$("[data-analytics-tab]").forEach(item => item.setAttribute("aria-selected", String(item === button)));
        renderAnalytics();
    }));
    $("#analytics-search").addEventListener("input", renderAnalytics);
    $("#drawer-close").addEventListener("click", closeUnitDrawer);
    $("#drawer-backdrop").addEventListener("click", closeUnitDrawer);
    document.addEventListener("keydown", event => {
        if (event.key === "Escape") closeUnitDrawer();
    });
}

function startClock() {
    const update = () => {
        $("#sync-clock").textContent = `${new Date().toISOString().slice(11, 19)} Z`;
    };
    update();
    window.setInterval(update, 1000);
}

async function boot() {
    bindEvents();
    startClock();
    navigate();
    await Promise.allSettled([
        checkHealth(),
        loadDashboard(),
        loadUnitFilterOptions()
    ]);
}

boot();
