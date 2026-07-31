export class ApiError extends Error {
    constructor(message, status, payload) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.payload = payload;
    }
}

class ApiClient {
    async request(path, options = {}) {
        const headers = new Headers(options.headers || {});
        if (options.body && !headers.has("Content-Type")) {
            headers.set("Content-Type", "application/json");
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

    specializations() {
        return this.request("/api/specializations", { auth: false });
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

    steamPlayer(steamId, weeks = 8, limit = 20) {
        return this.request(
            `/api/steam/players/${encodeURIComponent(steamId)}?weeks=${weeks}&limit=${limit}`,
            { auth: false }
        );
    }
}

export const api = new ApiClient();
