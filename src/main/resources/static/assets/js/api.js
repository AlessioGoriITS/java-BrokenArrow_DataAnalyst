const TOKEN_KEY = "battle-debrief-token";

export class ApiError extends Error {
    constructor(message, status, payload) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.payload = payload;
    }
}

class ApiClient {
    constructor() {
        this.token = sessionStorage.getItem(TOKEN_KEY) || "";
    }

    isAuthenticated() {
        return Boolean(this.token);
    }

    setToken(token) {
        this.token = token || "";
        if (this.token) {
            sessionStorage.setItem(TOKEN_KEY, this.token);
        } else {
            sessionStorage.removeItem(TOKEN_KEY);
        }
    }

    async request(path, options = {}) {
        const headers = new Headers(options.headers || {});
        if (options.body && !headers.has("Content-Type")) {
            headers.set("Content-Type", "application/json");
        }
        if (this.token && options.auth !== false) {
            headers.set("Authorization", `Bearer ${this.token}`);
        }

        let response;
        try {
            response = await fetch(path, { ...options, headers });
        } catch (error) {
            throw new ApiError("Backend non raggiungibile", 0, null);
        }

        const contentType = response.headers.get("content-type") || "";
        const payload = contentType.includes("application/json")
            ? await response.json()
            : await response.text();

        if (!response.ok) {
            if (response.status === 401 && options.auth !== false) {
                this.setToken("");
            }
            throw new ApiError(
                payload?.message || `Richiesta fallita (${response.status})`,
                response.status,
                payload
            );
        }
        return payload;
    }

    health() {
        return this.request("/actuator/health", { auth: false });
    }

    login(username, password) {
        return this.request("/api/auth/login", {
            method: "POST",
            auth: false,
            body: JSON.stringify({ username, password })
        });
    }

    currentUser() {
        return this.request("/api/auth/me");
    }

    user(userId) {
        return this.request(`/api/users/${userId}`);
    }

    units(params = {}) {
        const query = new URLSearchParams();
        Object.entries(params).forEach(([key, value]) => {
            if (value !== "" && value !== null && value !== undefined) {
                query.set(key, value);
            }
        });
        return this.request(`/api/units?${query}`, { auth: false });
    }

    unit(unitId) {
        return this.request(`/api/units/${unitId}`, { auth: false });
    }

    unitAnalytics() {
        return this.request("/api/analytics/units", { auth: false });
    }

    mapAnalytics() {
        return this.request("/api/analytics/maps", { auth: false });
    }

    specializationAnalytics() {
        return this.request("/api/analytics/specializations", { auth: false });
    }

    playerAnalysis(playerId) {
        return this.request(`/api/players/${playerId}/analysis`);
    }

    playerTrend(playerId, limit = 20) {
        return this.request(
            `/api/players/${playerId}/analysis/trend?limit=${limit}`
        );
    }

    playerUnits(playerId) {
        return this.request(`/api/players/${playerId}/units`);
    }

    playerMatches(playerId, size = 10) {
        return this.request(
            `/api/players/${playerId}/matches?page=0&size=${size}`
        );
    }
}

export const api = new ApiClient();
