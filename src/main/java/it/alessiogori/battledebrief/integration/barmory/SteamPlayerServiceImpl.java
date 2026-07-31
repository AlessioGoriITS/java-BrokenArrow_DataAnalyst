package it.alessiogori.battledebrief.integration.barmory;

import com.fasterxml.jackson.databind.JsonNode;
import it.alessiogori.battledebrief.common.exception.ResourceNotFoundException;
import it.alessiogori.battledebrief.integration.barmory.dto.SteamCareerResponse;
import it.alessiogori.battledebrief.integration.barmory.dto.SteamMatchResponse;
import it.alessiogori.battledebrief.integration.barmory.dto.SteamPlayerResponse;
import it.alessiogori.battledebrief.integration.barmory.dto.SteamUnitPerformanceResponse;
import it.alessiogori.battledebrief.unit.entity.Unit;
import it.alessiogori.battledebrief.unit.repository.UnitRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
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

    private final BarmoryGateway gateway;
    private final UnitRepository unitRepository;
    private final Clock clock;

    public SteamPlayerServiceImpl(
            BarmoryGateway gateway,
            UnitRepository unitRepository,
            Clock clock
    ) {
        this.gateway = gateway;
        this.unitRepository = unitRepository;
        this.clock = clock;
    }

    @Override
    public SteamPlayerResponse findBySteamId(
            String steamId,
            int weeks,
            int limit
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
        List<RawMatch> rawMatches = matchIds.stream()
                .map(id -> new RawMatch(id, gateway.findMatch(id)))
                .toList();
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
                aggregateUnits(rawMatches, commanderId),
                instant(stats, "updateDate")
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
                integer(player, "TotalExp", 0)
        );
    }

    private List<SteamUnitPerformanceResponse> aggregateUnits(
            List<RawMatch> matches,
            long commanderId
    ) {
        Map<Long, MutableUnitPerformance> totals = new HashMap<>();
        for (RawMatch match : matches) {
            JsonNode unitData = playerData(match.data(), commanderId)
                    .path("UnitData");
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
                } catch (NumberFormatException ignored) {
                    // Curated legacy identifiers do not carry the provider ID.
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

    private Instant instant(JsonNode node, String field) {
        String value = node.path(field).asText("");
        try {
            return value.isBlank() ? null : Instant.parse(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static final class MutableUnitPerformance {
        private int deployed;
        private int refunded;
        private int kills;
        private int damageDealt;
        private int damageReceived;
    }

    private record RawMatch(long id, JsonNode data) {
    }
}
