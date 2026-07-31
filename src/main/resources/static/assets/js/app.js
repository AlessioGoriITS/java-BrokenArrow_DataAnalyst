import { api } from "./api.js";
import { drawLineChart } from "./charts.js";

const state = {
    route: "dashboard",
    dashboardLoaded: false,
    analyticsTab: "units",
    unitPage: 0,
    unitLayout: "grid",
    account: null,
    accountLoading: false,
    playerProfile: null,
    playerLoading: false,
    playerRequest: null,
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
    if (state.route === "player") openPersonalDebrief();
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
    if (!api.isAuthenticated()) return renderPersonalGate("Accedi per caricare il tuo Command Center.");
    if (!state.account) return;
    const steamId = state.account.playerProfile?.steamId;
    if (!steamId) return renderPersonalGate("Collega lo Steam ID in My Debrief.");
    try {
        const profile = await ensurePlayerProfile(steamId);
        state.dashboardLoaded = true;
        renderPersonalDashboard(profile);
    } catch (error) {
        renderDashboardFailure(error);
    }
}

function renderPersonalGate(message) {
    $("#dataset-version").textContent = "LOGIN REQUIRED";
    $("#dataset-matches").textContent = "— MATCH";
    ["#kpi-units", "#kpi-maps", "#kpi-winrate", "#kpi-kd"].forEach(selector => {
        $(selector).textContent = "—";
    });
    $("#kpi-winrate-label").textContent = message;
    $("#kpi-kd-label").textContent = "Steam personale";
    ["#top-units-chart", "#map-popularity", "#specialization-overview"].forEach(selector => {
        const container = $(selector);
        container.classList.remove("loading-block");
        container.innerHTML = emptyMarkup("Profilo personale richiesto", message);
    });
}

function personalMaps(matches) {
    const totals = new Map();
    matches.forEach(match => {
        const item = totals.get(match.mapName) || { name: match.mapName, matches: 0, wins: 0, ratingDelta: 0 };
        item.matches += 1;
        item.wins += match.won ? 1 : 0;
        if (match.oldRating != null && match.newRating != null) item.ratingDelta += Number(match.newRating) - Number(match.oldRating);
        totals.set(match.mapName, item);
    });
    return [...totals.values()].sort((a, b) => b.matches - a.matches);
}

function personalSpecializations(matches) {
    const totals = new Map();
    matches.forEach(match => (match.specializations || []).forEach(name => {
        const item = totals.get(name) || { name, matches: 0, wins: 0 };
        item.matches += 1;
        item.wins += match.won ? 1 : 0;
        totals.set(name, item);
    }));
    return [...totals.values()].sort((a, b) => b.matches - a.matches);
}

function renderPersonalDashboard(profile) {
    const maps = personalMaps(profile.recentMatches);
    const specs = personalSpecializations(profile.recentMatches);
    $("#dataset-version").textContent = profile.displayName;
    $("#dataset-matches").textContent = `${formatNumber(profile.recentMatches.length)} RECENTI`;
    $("#kpi-units").textContent = formatNumber(profile.career.matches);
    $("#kpi-maps").textContent = formatNumber(maps.length);
    $("#kpi-winrate").textContent = profile.career.winRate == null ? "N/D" : `${formatDecimal(profile.career.winRate)}%`;
    $("#kpi-winrate-label").textContent = `${formatNumber(profile.career.wins)} vittorie`;
    $("#kpi-kd").textContent = formatDecimal(profile.currentRating, 0);
    $("#kpi-kd-label").textContent = `rank #${formatOptionalNumber(profile.leaderboardRank)} · ${profile.source}`;
    renderTopUnits(profile.mostUsedUnits, profile.source);
    renderMapPopularity(maps);
    renderSpecializationOverview(specs);
}

function renderTopUnits(items, source) {
    const container = $("#top-units-chart");
    container.classList.remove("loading-block");
    const ranked = [...items]
        .sort((a, b) => b.deployed - a.deployed)
        .slice(0, 6);
    if (!ranked.length) {
        container.innerHTML = emptyMarkup(
            "Dettaglio unità non pubblicato",
            `${source} fornisce match, mappe e brigate per questo profilo, ma non le singole unità schierate.`
        );
        return;
    }
    const max = Math.max(...ranked.map(item => item.deployed), 1);
    container.innerHTML = ranked.map((item, index) => `
        <div class="rank-row">
            <span class="rank-index">${String(index + 1).padStart(2, "0")}</span>
            <span class="rank-name"><strong>${escapeHtml(item.unitName)}</strong><small>${formatNumber(item.kills)} kill · ${formatNumber(item.refunded)} rimborsi</small></span>
            <span class="bar-track"><i class="bar-fill" style="width:${Math.max(4, item.deployed / max * 100)}%"></i></span>
            <span class="rank-value">${formatNumber(item.deployed)}×</span>
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
        <div class="compact-row"><span><strong>${escapeHtml(item.name)}</strong><small>${formatNumber(item.matches)} match · ${formatNumber(item.wins)} vittorie</small></span><b>${item.matches ? formatDecimal(item.wins * 100 / item.matches) : 0}%</b></div>
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
        <div class="spec-row"><span class="faction-chip">BRG</span><span><strong>${escapeHtml(item.name)}</strong><small>${formatNumber(item.matches)} match personali</small></span><b>${item.matches ? formatDecimal(item.wins * 100 / item.matches) : 0}%</b></div>
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
            ? `<img src="${escapeHtml(unit.imageUrl)}" alt="">`
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
    if (!api.isAuthenticated()) return renderAnalyticsGate("Accedi per analizzare il tuo Steam ID.");
    if (!state.account) return;
    const steamId = state.account.playerProfile?.steamId;
    if (!steamId) return renderAnalyticsGate("Collega lo Steam ID in My Debrief.");
    try {
        await ensurePlayerProfile(steamId);
        renderAnalytics();
    } catch (error) {
        renderAnalyticsGate(error.message);
    }
}

const analyticsDefinitions = {
    units: {
        code: "PERSONAL UNIT USAGE",
        heading: "Unità impiegate dal comandante",
        columns: ["Asset", "Schierate", "Rimborsate", "Kill", "Danno inflitto", "Danno ricevuto"],
        name: item => item.unitName,
        row: item => `<tr><td>${escapeHtml(item.unitName)}<span class="table-sub">provider unit #${formatNumber(item.unitId)}</span></td><td>${formatNumber(item.deployed)}</td><td>${formatNumber(item.refunded)}</td><td>${formatNumber(item.kills)}</td><td>${formatNumber(item.damageDealt)}</td><td>${formatNumber(item.damageReceived)}</td></tr>`
    },
    maps: {
        code: "PERSONAL THEATRE PERFORMANCE",
        heading: "Rendimento personale per mappa",
        columns: ["Mappa", "Match", "Vittorie", "Sconfitte", "Win rate", "Variazione ELO"],
        name: item => item.name,
        row: item => `<tr><td>${escapeHtml(item.name)}</td><td>${formatNumber(item.matches)}</td><td>${formatNumber(item.wins)}</td><td>${formatNumber(item.matches - item.wins)}</td><td>${formatDecimal(item.wins * 100 / item.matches)}%</td><td>${item.ratingDelta >= 0 ? "+" : ""}${formatDecimal(item.ratingDelta, 1)}</td></tr>`
    },
    specializations: {
        code: "PERSONAL FORMATION PERFORMANCE",
        heading: "Brigate usate dal comandante",
        columns: ["Brigata", "Match", "Vittorie", "Sconfitte", "Win rate"],
        name: item => item.name,
        row: item => `<tr><td>${escapeHtml(item.name)}</td><td>${formatNumber(item.matches)}</td><td>${formatNumber(item.wins)}</td><td>${formatNumber(item.matches - item.wins)}</td><td>${formatDecimal(item.wins * 100 / item.matches)}%</td></tr>`
    }
};

function personalAnalytics() {
    const profile = state.playerProfile;
    return {
        units: profile?.mostUsedUnits || [],
        maps: personalMaps(profile?.recentMatches || []),
        specializations: personalSpecializations(profile?.recentMatches || [])
    };
}

function renderAnalytics() {
    if (!state.playerProfile) return renderAnalyticsGate("Profilo personale non caricato.");
    const definition = analyticsDefinitions[state.analyticsTab];
    const search = $("#analytics-search").value.trim().toLowerCase();
    const items = personalAnalytics()[state.analyticsTab]
        .filter(item => definition.name(item).toLowerCase().includes(search));
    $("#analytics-code").textContent = definition.code;
    $("#analytics-heading").textContent = definition.heading;
    $("#analytics-scope").textContent = `${state.playerProfile.displayName} / ${state.playerProfile.source}`;
    $("#analytics-table-head").innerHTML = `<tr>${definition.columns.map(column => `<th>${escapeHtml(column)}</th>`).join("")}</tr>`;
    $("#analytics-table-body").innerHTML = items.length
        ? items.map(definition.row).join("")
        : `<tr><td colspan="${definition.columns.length}">${emptyMarkup(
            state.analyticsTab === "units" ? "Dettaglio unità non pubblicato" : "Nessun dato personale disponibile",
            state.analyticsTab === "units"
                ? `${state.playerProfile.source} non espone le unità schierate per questo profilo; mappe e brigate sono disponibili nelle altre schede.`
                : "Il provider non ha restituito elementi per i match recenti."
        )}</td></tr>`;
    renderAnalyticsSummary(state.playerProfile);
}

function renderAnalyticsSummary(profile) {
    const recent = profile.recentMatches;
    const recentWins = recent.filter(match => match.won).length;
    const ratingDelta = recent.reduce((sum, match) => sum + (
        match.oldRating != null && match.newRating != null
            ? Number(match.newRating) - Number(match.oldRating)
            : 0
    ), 0);
    $("#analytics-summary").innerHTML = [
        ["MATCH CARRIERA", formatNumber(profile.career.matches)],
        ["MATCH RECENTI", formatNumber(recent.length)],
        ["WIN RATE RECENTE", recent.length ? `${formatDecimal(recentWins * 100 / recent.length)}%` : "N/D"],
        ["VARIAZIONE ELO", `${ratingDelta >= 0 ? "+" : ""}${formatDecimal(ratingDelta, 1)}`]
    ].map(([label, value]) => `<article class="summary-card"><span>${label}</span><strong>${value}</strong></article>`).join("");
}

function renderAnalyticsGate(message) {
    $("#analytics-scope").textContent = "ACCOUNT REQUIRED";
    $("#analytics-summary").innerHTML = "";
    $("#analytics-table-head").innerHTML = "";
    $("#analytics-table-body").innerHTML = `<tr><td>${emptyMarkup("Profilo personale richiesto", message)}</td></tr>`;
}

function updateAccountButton() {
    $("#auth-button").textContent = state.account
        ? `Account · ${state.account.username}`
        : "Accedi";
    $("#global-logout").hidden = !state.account;
}

function showAccountStage(stage) {
    $("#player-guest").hidden = stage !== "guest";
    $("#steam-link-stage").hidden = stage !== "link";
    $("#player-console").hidden = stage !== "console";
}

function showGuest() {
    showAccountStage("guest");
}

function showSteamLink(error = "") {
    showAccountStage("link");
    $("#link-account-name").textContent = state.account
        ? `Account ${state.account.username}`
        : "Collega comandante";
    $("#linked-steam-id").value = state.account?.playerProfile?.steamId || "";
    $("#steam-link-error").textContent = error;
}

function switchAuthMode(mode) {
    $$('[data-auth-mode]').forEach(button => button.setAttribute(
        "aria-selected", String(button.dataset.authMode === mode)
    ));
    $("#login-form").hidden = mode !== "login";
    $("#register-form").hidden = mode !== "register";
    $(`#${mode}-form input`)?.focus();
}

async function loadAccount() {
    if (!api.isAuthenticated() || state.accountLoading) {
        if (!api.isAuthenticated()) showGuest();
        return;
    }
    state.accountLoading = true;
    try {
        const current = await api.currentUser();
        state.account = await api.user(current.id);
        updateAccountButton();
        const steamId = state.account.playerProfile?.steamId;
        if (state.route === "player") {
            if (steamId) await loadPlayerConsole(steamId);
            else showSteamLink();
        } else if (state.route === "dashboard") {
            await loadDashboard();
        } else if (state.route === "analytics") {
            await loadAnalytics();
        }
    } catch (error) {
        if (error.status === 401 || !api.isAuthenticated()) logout(false);
        else {
            showGuest();
            showToast(error.message, "error");
        }
    } finally {
        state.accountLoading = false;
    }
}

function openPersonalDebrief() {
    if (!api.isAuthenticated()) return showGuest();
    if (!state.account) return loadAccount();
    const steamId = state.account.playerProfile?.steamId;
    if (steamId && !state.playerLoading) loadPlayerConsole(steamId);
    else if (!steamId) showSteamLink();
}

async function submitLogin(event) {
    event.preventDefault();
    const button = $("button[type='submit']", event.currentTarget);
    const errorNode = $("#login-error");
    errorNode.textContent = "";
    button.disabled = true;
    button.textContent = "Accesso…";
    try {
        const response = await api.login(
            $("#login-username").value.trim(),
            $("#login-password").value
        );
        api.setToken(response.accessToken);
        state.account = null;
        state.playerProfile = null;
        state.dashboardLoaded = false;
        await loadAccount();
        showToast("Accesso effettuato", "success");
    } catch (error) {
        errorNode.textContent = error.message;
    } finally {
        button.disabled = false;
        button.textContent = "Accedi";
    }
}

async function submitRegistration(event) {
    event.preventDefault();
    const button = $("button[type='submit']", event.currentTarget);
    const errorNode = $("#register-error");
    errorNode.textContent = "";
    button.disabled = true;
    button.textContent = "Creazione…";
    const username = $("#register-username").value.trim();
    const password = $("#register-password").value;
    try {
        await api.register(username, $("#register-email").value.trim(), password);
        const response = await api.login(username, password);
        api.setToken(response.accessToken);
        state.account = null;
        state.playerProfile = null;
        state.dashboardLoaded = false;
        await loadAccount();
        showToast("Account creato: ora collega Steam", "success");
    } catch (error) {
        errorNode.textContent = error.message;
    } finally {
        button.disabled = false;
        button.textContent = "Crea account";
    }
}

async function submitSteamLink(event) {
    event.preventDefault();
    const button = $("button[type='submit']", event.currentTarget);
    const errorNode = $("#steam-link-error");
    const steamId = $("#linked-steam-id").value.trim();
    errorNode.textContent = "";
    button.disabled = true;
    button.textContent = "Verifica profilo…";
    try {
        state.account = await api.linkSteam(state.account.id, steamId);
        state.playerProfile = null;
        state.playerRequest = null;
        state.dashboardLoaded = false;
        updateAccountButton();
        await loadPlayerConsole(steamId);
        showToast("Steam ID collegato all'account", "success");
    } catch (error) {
        errorNode.textContent = error.message;
    } finally {
        button.disabled = false;
        button.textContent = "Collega e carica";
    }
}

function logout(notify = true) {
    api.setToken(null);
    state.account = null;
    state.playerProfile = null;
    state.playerRequest = null;
    state.dashboardLoaded = false;
    state.trend = [];
    updateAccountButton();
    showGuest();
    if (state.route === "dashboard") renderPersonalGate("Accedi per caricare il tuo Command Center.");
    if (state.route === "analytics") renderAnalyticsGate("Accedi per analizzare il tuo Steam ID.");
    if (notify) showToast("Sessione terminata", "success");
}

function changeSteamPlayer() {
    showSteamLink();
    location.hash = "player";
}

async function loadPlayerConsole(steamId) {
    try {
        showAccountStage("console");
        $("#player-display-name").textContent = "Recupero dati…";
        $("#player-matches").innerHTML = '<tr><td colspan="7">Sincronizzazione della cronologia in corso…</td></tr>';
        const profile = await ensurePlayerProfile(steamId);
        const trend = [...profile.recentMatches]
            .filter(match => Number(match.newRating) > 0)
            .reverse()
            .map(match => ({ ...match, startedAt: match.endedAt }));
        state.trend = trend;
        renderPlayerHeader(profile);
        renderPlayerMetrics(profile);
        renderPlayerTrend(trend);
        renderPlayerUnits(profile.mostUsedUnits, profile.source);
        renderPlayerMatches(profile.recentMatches, profile.source);
    } catch (error) {
        showSteamLink(`Profilo collegato, ma le statistiche non sono disponibili: ${error.message}`);
        throw error;
    }
}

async function ensurePlayerProfile(steamId) {
    if (state.playerProfile?.steamId === steamId) return state.playerProfile;
    if (state.playerRequest) return state.playerRequest;
    state.playerLoading = true;
    state.playerRequest = api.steamPlayer(steamId)
        .then(profile => {
            state.playerProfile = profile;
            return profile;
        })
        .finally(() => {
            state.playerLoading = false;
            state.playerRequest = null;
        });
    return state.playerRequest;
}

function renderPlayerHeader(profile) {
    $("#player-avatar").textContent = initials(profile.displayName);
    $("#player-display-name").textContent = profile.displayName;
    $("#player-handle").textContent = `STEAM ${profile.steamId} · LVL ${profile.level} · RANK #${profile.leaderboardRank || "—"} · ${profile.source}`;
    $("#player-current-elo").textContent = formatDecimal(profile.currentRating, 0);
    const matches = profile.recentMatches.filter(match => Number(match.newRating) > 0);
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

function renderPlayerUnits(units, source) {
    const container = $("#player-units");
    container.classList.remove("loading-block");
    if (!units.length) {
        container.innerHTML = emptyMarkup(
            "Dettaglio unità non pubblicato",
            `${source} non restituisce le singole unità schierate per questo profilo. Non significa che non siano state usate.`
        );
        return;
    }
    container.innerHTML = [...units]
        .sort((a, b) => b.deployed - a.deployed)
        .slice(0, 6)
        .map(item => `<div class="compact-row"><span><strong>${escapeHtml(item.unitName)}</strong><small>${formatNumber(item.deployed)} schierate · ${formatNumber(item.refunded)} rimborsate</small></span><b>${formatNumber(item.kills)} KILL</b></div>`)
        .join("");
}

function renderPlayerMatches(matches, source) {
    const detailed = source !== "BATTLEGROUP";
    $("#player-matches").innerHTML = matches.length
        ? matches.map(match => `<tr><td><span class="result-badge ${match.won ? "win" : "loss"}">${match.won ? "W" : "L"}</span></td><td>${escapeHtml(match.mapName)}</td><td>${detailed ? `${formatNumber(Math.round(match.durationSeconds / 60))} min` : "N/D"}</td><td>${Number(match.newRating) > 0 ? `${formatDecimal(match.oldRating, 0)} → ${formatDecimal(match.newRating, 0)}` : "N/D"}</td><td>${detailed ? formatNumber(match.destructionScore) : "N/D"}</td><td>${detailed ? formatNumber(match.lossesScore) : "N/D"}</td><td>${formatDate(match.endedAt)}</td></tr>`).join("")
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
    $("#global-logout").addEventListener("click", () => logout());
    $$('[data-auth-mode]').forEach(button => button.addEventListener(
        "click", () => switchAuthMode(button.dataset.authMode)
    ));
    $("#login-form").addEventListener("submit", submitLogin);
    $("#register-form").addEventListener("submit", submitRegistration);
    $("#steam-link-form").addEventListener("submit", submitSteamLink);
    $("#change-player").addEventListener("click", changeSteamPlayer);
    $("#logout-button").addEventListener("click", () => logout());
    $("#link-logout").addEventListener("click", () => logout());
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
    if (api.isAuthenticated()) await loadAccount();
    await Promise.allSettled([
        checkHealth(),
        loadDashboard(),
        loadUnitFilterOptions()
    ]);
}

boot();
