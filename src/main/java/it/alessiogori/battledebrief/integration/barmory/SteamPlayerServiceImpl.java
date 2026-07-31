package it.alessiogori.battledebrief.integration.barmory;

import com.fasterxml.jackson.databind.JsonNode;
import it.alessiogori.battledebrief.common.exception.ExternalProviderException;
import it.alessiogori.battledebrief.common.exception.ResourceNotFoundException;
import it.alessiogori.battledebrief.integration.barmory.dto.SteamCareerResponse;
import it.alessiogori.battledebrief.integration.barmory.dto.SteamMatchResponse;
import it.alessiogori.battledebrief.integration.barmory.dto.SteamPlayerResponse;
import it.alessiogori.battledebrief.integration.barmory.dto.SteamLookupDiagnosticsResponse;
import it.alessiogori.battledebrief.integration.barmory.dto.SteamUnitPerformanceResponse;
import it.alessiogori.battledebrief.unit.entity.Unit;
import it.alessiogori.battledebrief.unit.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class SteamPlayerServiceImpl implements SteamPlayerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            SteamPlayerServiceImpl.class
    );

    private final BarmoryGateway gateway;
    private final BattleGroupGateway battleGroupGateway;
    private final UnitRepository unitRepository;
    private final Clock clock;
    private final SteamProviderMetrics providerMetrics;

    public SteamPlayerServiceImpl(
            BarmoryGateway gateway,
            BattleGroupGateway battleGroupGateway,
            UnitRepository unitRepository,
            Clock clock,
            SteamProviderMetrics providerMetrics
    ) {
        this.gateway = gateway;
        this.battleGroupGateway = battleGroupGateway;
        this.unitRepository = unitRepository;
        this.clock = clock;
        this.providerMetrics = providerMetrics;
    }

    @Override
    public SteamPlayerResponse findBySteamId(
            String steamId,
            int weeks,
            int limit
    ) {
        long lookupStarted = System.nanoTime();
        long providerStarted = lookupStarted;
        LookupDiagnostics diagnostics = new LookupDiagnostics();
        try {
            SteamPlayerResponse response = findOnBarmory(
                    steamId,
                    weeks,
                    limit,
                    diagnostics
            );
            providerMetrics.recordLookup(
                    "BARMORY",
                    elapsed(providerStarted),
                    true
            );
            return response.withDiagnostics(
                    diagnostics.toResponse(elapsedMillis(lookupStarted))
            );
        } catch (ExternalProviderException exception) {
            providerMetrics.recordLookup(
                    "BARMORY",
                    elapsed(providerStarted),
                    false
            );
            LOGGER.warn(
                    "BArmory lookup failed; using BattleGroup fallback: {}",
                    exception.getMessage()
            );
            LookupDiagnostics fallbackDiagnostics = new LookupDiagnostics();
            fallbackDiagnostics.warn(
                    "BArmory unavailable: " + exception.getMessage()
            );
            providerStarted = System.nanoTime();
            try {
                SteamPlayerResponse response = findOnBattleGroup(
                        steamId,
                        limit,
                        fallbackDiagnostics
                );
                providerMetrics.recordLookup(
                        "BATTLEGROUP",
                        elapsed(providerStarted),
                        true
                );
                return response.withDiagnostics(
                        fallbackDiagnostics.toResponse(
                                elapsedMillis(lookupStarted)
                        )
                );
            } catch (RuntimeException fallbackException) {
                providerMetrics.recordLookup(
                        "BATTLEGROUP",
                        elapsed(providerStarted),
                        false
                );
                throw fallbackException;
            }
        } catch (RuntimeException exception) {
            providerMetrics.recordLookup(
                    "BARMORY",
                    elapsed(providerStarted),
                    false
            );
            throw exception;
        }
    }

    private SteamPlayerResponse findOnBarmory(
            String steamId,
            int weeks,
            int limit,
            LookupDiagnostics diagnostics
    ) {
        JsonNode commander = gateway.findCommander(steamId);
        long commanderId = commander.path("id").asLong();
        if (commanderId == 0) {
            throw new ResourceNotFoundException(
                    "No Broken Arrow commander found for this Steam ID"
            );
        }

        JsonNode stats = gateway.findCommanderStats(steamId);
        Set<Long> matchIds = recentMatchIds(commanderId, weeks, limit);
        List<RawMatch> rawMatches = availableMatches(matchIds, diagnostics);
        List<SteamMatchResponse> matches = rawMatches.stream()
                .map(match -> toMatch(match, commanderId))
                .sorted(Comparator.comparing(SteamMatchResponse::endedAt).reversed())
                .toList();

        return new SteamPlayerResponse(
                steamId,
                commanderId,
                text(commander, "name", text(stats, "name", "Commander")),
                integer(commander, "lvl", integer(stats, "level", 0)),
                decimal(commander, "rt"),
                integer(commander, "rk", 0),
                career(stats),
                matches,
                aggregateUnits(rawMatches, commanderId, "BARMORY", diagnostics),
                "BARMORY",
                instant(stats, "updateDate", "BARMORY", diagnostics)
        );
    }

    private SteamPlayerResponse findOnBattleGroup(
            String steamId,
            int limit,
            LookupDiagnostics diagnostics
    ) {
        JsonNode response = battleGroupGateway.findPlayerStats(steamId);
        if (!response.path("found").asBoolean(false)) {
            throw new ResourceNotFoundException(
                    "No Broken Arrow commander found for this Steam ID"
            );
        }
        JsonNode user = response.path("user");
        JsonNode identity = battleGroupGateway.findPlayer(steamId);
        long commanderId = identity.path("user").path("id").asLong();
        if (commanderId == 0) {
            throw new ResourceNotFoundException(
                    "No Broken Arrow commander found for this Steam ID"
            );
        }
        JsonNode stats = response.path("stats");
        List<SteamMatchResponse> matches = new ArrayList<>();
        response.path("recent").forEach(match -> {
            BigDecimal oldRating = decimal(match, "oldRating");
            BigDecimal change = decimal(match, "ratingChange");
            BigDecimal newRating = oldRating == null || change == null
                    ? null
                    : oldRating.add(change);
            matches.add(new SteamMatchResponse(
                    match.path("fightId").asLong(),
                    0,
                    text(match, "mapName", "Unknown map"),
                    "win".equalsIgnoreCase(match.path("result").asText()),
                    0,
                    0,
                    Instant.ofEpochSecond(match.path("endTime").asLong()),
                    oldRating,
                    newRating,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    text(match, "countryName", null),
                    stringList(match.path("specNames")),
                    decimal(match, "enemyAvgRating"),
                    match.path("isRanked").asBoolean(false)
            ));
        });
        List<SteamMatchResponse> recentMatches = matches.stream()
                .sorted(Comparator.comparing(SteamMatchResponse::endedAt).reversed())
                .limit(limit)
                .toList();
        List<RawMatch> rawMatches = new ArrayList<>();
        diagnostics.requestedMatches(recentMatches.size());
        recentMatches.forEach(match -> {
            try {
                rawMatches.add(new RawMatch(
                        match.matchId(),
                        battleGroupGateway.findMatch(match.matchId())
                ));
                diagnostics.loadedMatch();
            } catch (ExternalProviderException exception) {
                // The summary remains useful if an archived match has expired.
                diagnostics.discardMatch(
                        match.matchId(),
                        "Match telemetry " + match.matchId()
                                + " unavailable: " + exception.getMessage()
                );
                providerMetrics.recordDiscardedMatch(
                        "BATTLEGROUP",
                        "telemetry_unavailable"
                );
                LOGGER.warn(
                        "Match telemetry {} could not be loaded: {}",
                        match.matchId(),
                        exception.getMessage()
                );
            }
        });
        int fights = integer(stats, "fightsCount", 0);
        int wins = integer(stats, "winsCount", 0);
        SteamCareerResponse career = new SteamCareerResponse(
                fights,
                wins,
                integer(stats, "losesCount", Math.max(0, fights - wins)),
                0,
                integer(stats, "killsCount", 0),
                integer(stats, "deathsCount", 0),
                decimal(stats, "kdRatio"),
                fights == 0 ? null : BigDecimal.valueOf(wins * 100L)
                        .divide(BigDecimal.valueOf(fights), 2, RoundingMode.HALF_UP),
                stats.path("totalMatchTimeSec").asLong(0),
                integer(stats, "capturedZonesCount", 0),
                0,
                0
        );
        return new SteamPlayerResponse(
                steamId,
                commanderId,
                text(response.path("steam"), "personaName",
                        text(user, "name", "Commander")),
                integer(user, "level", 0),
                decimal(user, "rating"),
                integer(user, "rank", 0),
                career,
                recentMatches,
                aggregateUnits(
                        rawMatches,
                        commanderId,
                        "BATTLEGROUP",
                        diagnostics
                ),
                "BATTLEGROUP",
                clock.instant()
        );
    }

    private Set<Long> recentMatchIds(long commanderId, int weeks, int limit) {
        Set<Long> ids = new LinkedHashSet<>();
        LocalDate cursor = LocalDate.now(clock);
        for (int offset = 0; offset < weeks && ids.size() < limit; offset++) {
            ids.addAll(gateway.findMatchIds(commanderId, isoWeek(cursor)));
            cursor = cursor.minusWeeks(1);
        }
        return ids.stream().limit(limit)
                .collect(java.util.stream.Collectors.toCollection(
                        LinkedHashSet::new
                ));
    }

    private List<RawMatch> availableMatches(
            Set<Long> matchIds,
            LookupDiagnostics diagnostics
    ) {
        List<RawMatch> matches = new ArrayList<>();
        diagnostics.requestedMatches(matchIds.size());
        for (Long matchId : matchIds) {
            try {
                matches.add(new RawMatch(matchId, gateway.findMatch(matchId)));
                diagnostics.loadedMatch();
            } catch (ExternalProviderException exception) {
                // Providers can retain an ID after its match payload expires.
                // One missing match must not discard the remaining telemetry.
                diagnostics.discardMatch(
                        matchId,
                        "Match telemetry " + matchId
                                + " unavailable: " + exception.getMessage()
                );
                providerMetrics.recordDiscardedMatch(
                        "BARMORY",
                        "telemetry_unavailable"
                );
                LOGGER.warn(
                        "BArmory match telemetry {} was discarded: {}",
                        matchId,
                        exception.getMessage()
                );
            }
        }
        if (!matchIds.isEmpty() && matches.isEmpty()) {
            throw new ExternalProviderException(
                    "BArmory match telemetry is temporarily unavailable"
            );
        }
        return List.copyOf(matches);
    }

    private String isoWeek(LocalDate date) {
        WeekFields fields = WeekFields.ISO;
        return "%d-W%02d".formatted(
                date.get(fields.weekBasedYear()),
                date.get(fields.weekOfWeekBasedYear())
        );
    }

    private SteamCareerResponse career(JsonNode stats) {
        JsonNode rating = stats.path("statisticByLobbyType").path("Rating");
        int matches = integer(rating, "fightsCount", 0);
        int wins = integer(rating, "winsCount", 0);
        return new SteamCareerResponse(
                matches,
                wins,
                integer(rating, "losesCount", Math.max(0, matches - wins)),
                integer(rating, "leavesCount", 0),
                integer(rating, "killsCount", 0),
                integer(rating, "deathsCount", 0),
                decimal(rating, "kdRatio"),
                matches == 0 ? null : BigDecimal.valueOf(wins * 100L)
                        .divide(BigDecimal.valueOf(matches), 2, RoundingMode.HALF_UP),
                rating.path("totalMatchTimeSec").asLong(0),
                integer(stats, "capturedZonesCount", 0),
                integer(stats, "killsFriendlyFireCount", 0),
                stats.path("supplyPointsConsumed").asLong(0)
        );
    }

    private SteamMatchResponse toMatch(RawMatch rawMatch, long commanderId) {
        JsonNode match = rawMatch.data();
        JsonNode player = playerData(match, commanderId);
        int mapId = integer(match, "MapId", 0);
        return new SteamMatchResponse(
                rawMatch.id(),
                mapId,
                "Map #" + mapId,
                integer(player, "TeamId", -1)
                        == integer(match, "WinnerTeam", -2),
                integer(player, "TeamId", 0),
                integer(match, "TotalPlayTimeInSec", 0),
                Instant.ofEpochSecond(match.path("EndTime").asLong(0)),
                decimal(player, "OldRating"),
                decimal(player, "NewRating"),
                integer(player, "DestructionScore", 0),
                integer(player, "LossesScore", 0),
                integer(player, "DamageDealt", 0),
                integer(player, "DamageReceived", 0),
                integer(player, "ObjectivesCaptured", 0),
                integer(player, "TotalExp", 0),
                text(player, "CountryName", null),
                stringList(player.path("SpecializationNames")),
                decimal(player, "EnemyAvgRating"),
                true
        );
    }

    private List<SteamUnitPerformanceResponse> aggregateUnits(
            List<RawMatch> matches,
            long commanderId,
            String provider,
            LookupDiagnostics diagnostics
    ) {
        Map<Long, MutableUnitPerformance> totals = new HashMap<>();
        for (RawMatch match : matches) {
            JsonNode unitData;
            try {
                unitData = playerData(match.data(), commanderId).path("UnitData");
            } catch (ResourceNotFoundException exception) {
                diagnostics.invalidField(
                        "Match " + match.id()
                                + " has no telemetry for commander "
                                + commanderId
                );
                providerMetrics.recordInvalidField(
                        provider,
                        "match.commander_data"
                );
                LOGGER.warn(
                        "Unit telemetry from match {} was discarded: {}",
                        match.id(),
                        exception.getMessage()
                );
                continue;
            }
            unitData.forEach(unit -> {
                long unitId = unit.path("Id").asLong();
                MutableUnitPerformance total = totals.computeIfAbsent(
                        unitId,
                        ignored -> new MutableUnitPerformance()
                );
                total.deployed++;
                if (unit.path("WasRefunded").asBoolean(false)) total.refunded++;
                total.kills += integer(unit, "KilledCount", 0);
                total.damageDealt += integer(unit, "TotalDamageDealt", 0);
                total.damageReceived += integer(unit, "TotalDamageReceived", 0);
            });
        }

        Map<Long, String> names = new HashMap<>();
        for (Unit unit : unitRepository.findAll()) {
            String externalId = unit.getExternalUnitId();
            if (externalId != null && externalId.startsWith("ba_")) {
                try {
                    names.put(Long.parseLong(externalId.substring(3)), unit.getName());
                } catch (NumberFormatException exception) {
                    // Curated legacy identifiers do not carry the provider ID.
                    diagnostics.invalidField(
                            "Catalog unit has an invalid provider ID: "
                                    + externalId
                    );
                    providerMetrics.recordInvalidField(
                            provider,
                            "catalog.external_unit_id"
                    );
                    LOGGER.warn(
                            "Catalog unit ID {} cannot be mapped to provider data",
                            externalId
                    );
                }
            }
        }

        return totals.entrySet().stream()
                .map(entry -> new SteamUnitPerformanceResponse(
                        entry.getKey(),
                        names.getOrDefault(entry.getKey(), "Unit #" + entry.getKey()),
                        entry.getValue().deployed,
                        entry.getValue().refunded,
                        entry.getValue().kills,
                        entry.getValue().damageDealt,
                        entry.getValue().damageReceived
                ))
                .sorted(Comparator.comparingInt(
                        SteamUnitPerformanceResponse::deployed
                ).reversed())
                .limit(12)
                .toList();
    }

    private JsonNode playerData(JsonNode match, long commanderId) {
        JsonNode data = match.path("Data");
        JsonNode direct = data.path(Long.toString(commanderId));
        if (!direct.isMissingNode()) return direct;
        for (JsonNode player : data) {
            if (player.path("Id").asLong() == commanderId) return player;
        }
        throw new ResourceNotFoundException(
                "Commander data is missing from a retrieved match"
        );
    }

    private int integer(JsonNode node, String field, int fallback) {
        return node.path(field).isNumber() ? node.path(field).asInt() : fallback;
    }

    private BigDecimal decimal(JsonNode node, String field) {
        return node.path(field).isNumber() ? node.path(field).decimalValue() : null;
    }

    private String text(JsonNode node, String field, String fallback) {
        String value = node.path(field).asText("");
        return value.isBlank() ? fallback : value;
    }

    private Instant instant(
            JsonNode node,
            String field,
            String provider,
            LookupDiagnostics diagnostics
    ) {
        String value = node.path(field).asText("");
        try {
            return value.isBlank() ? null : Instant.parse(value);
        } catch (RuntimeException exception) {
            diagnostics.invalidField(
                    "Invalid timestamp in field " + field + ": " + value
            );
            providerMetrics.recordInvalidField(provider, field);
            LOGGER.warn(
                    "Provider {} returned an invalid {} timestamp: {}",
                    provider,
                    field,
                    value
            );
            return null;
        }
    }

    private Duration elapsed(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt);
    }

    private long elapsedMillis(long startedAt) {
        return elapsed(startedAt).toMillis();
    }

    private List<String> stringList(JsonNode node) {
        if (!node.isArray()) return List.of();
        List<String> values = new ArrayList<>();
        node.forEach(value -> {
            if (value.isTextual() && !value.asText().isBlank()) {
                values.add(value.asText());
            }
        });
        return List.copyOf(values);
    }

    private static final class MutableUnitPerformance {
        private int deployed;
        private int refunded;
        private int kills;
        private int damageDealt;
        private int damageReceived;
    }

    private static final class LookupDiagnostics {

        private static final int MAX_WARNINGS = 20;

        private int requestedMatches;
        private int loadedMatches;
        private int invalidFields;
        private final List<Long> discardedMatchIds = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        private void requestedMatches(int value) {
            requestedMatches = value;
        }

        private void loadedMatch() {
            loadedMatches++;
        }

        private void discardMatch(long matchId, String warning) {
            discardedMatchIds.add(matchId);
            warn(warning);
        }

        private void invalidField(String warning) {
            invalidFields++;
            warn(warning);
        }

        private void warn(String warning) {
            if (warnings.size() < MAX_WARNINGS) {
                warnings.add(warning);
            }
        }

        private SteamLookupDiagnosticsResponse toResponse(long durationMs) {
            return new SteamLookupDiagnosticsResponse(
                    durationMs,
                    requestedMatches,
                    loadedMatches,
                    discardedMatchIds.size(),
                    discardedMatchIds,
                    invalidFields,
                    warnings
            );
        }
    }

    private record RawMatch(long id, JsonNode data) {
    }
}
