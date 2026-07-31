package it.alessiogori.battledebrief.integration.barmory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.alessiogori.battledebrief.common.exception.ExternalProviderException;
import it.alessiogori.battledebrief.integration.barmory.dto.SteamPlayerResponse;
import it.alessiogori.battledebrief.unit.entity.Unit;
import it.alessiogori.battledebrief.unit.repository.UnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SteamPlayerServiceImplTests {

    private static final String STEAM_ID = "76561198157609957";
    private final ObjectMapper mapper = new ObjectMapper();
    private BarmoryGateway gateway;
    private BattleGroupGateway battleGroupGateway;
    private UnitRepository unitRepository;
    private SteamPlayerService service;

    @BeforeEach
    void setUp() {
        gateway = mock(BarmoryGateway.class);
        battleGroupGateway = mock(BattleGroupGateway.class);
        unitRepository = mock(UnitRepository.class);
        service = new SteamPlayerServiceImpl(
                gateway,
                battleGroupGateway,
                unitRepository,
                Clock.fixed(Instant.parse("2026-07-31T12:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void buildsPublicPlayerDebriefFromSteamId() throws Exception {
        when(gateway.findCommander(STEAM_ID)).thenReturn(json("""
                {"id":27516,"name":"HotWinter","lvl":88,"rt":3500.4,"rk":1}
                """));
        when(gateway.findCommanderStats(STEAM_ID)).thenReturn(json("""
                {
                  "name":"HotWinter","level":88,
                  "updateDate":"2026-07-31T10:00:00Z",
                  "capturedZonesCount":51,"killsFriendlyFireCount":2,
                  "supplyPointsConsumed":18000,
                  "statisticByLobbyType":{"Rating":{
                    "fightsCount":10,"winsCount":6,"losesCount":4,
                    "leavesCount":0,"killsCount":120,"deathsCount":80,
                    "kdRatio":1.5,"totalMatchTimeSec":7200
                  }}
                }
                """));
        when(gateway.findMatchIds(27516, "2026-W31"))
                .thenReturn(List.of(7448867L));
        when(gateway.findMatch(7448867L)).thenReturn(json("""
                {
                  "MapId":22,"EndTime":1785500000,"TotalPlayTimeInSec":1820,
                  "WinnerTeam":1,
                  "Data":{"27516":{
                    "Id":27516,"TeamId":1,"OldRating":3490.7,"NewRating":3491.9,
                    "DestructionScore":8200,"LossesScore":6900,
                    "DamageDealt":1160,"DamageReceived":1481,
                    "ObjectivesCaptured":2,"TotalExp":3105,
                    "UnitData":{"100":{"Id":42,"KilledCount":3,
                      "TotalDamageDealt":700,"TotalDamageReceived":200}}
                  }}
                }
                """));
        Unit unit = new Unit(
                "ba_42", "M1A1 Abrams", "USA", "TANK", 240, "Main battle tank"
        );
        when(unitRepository.findAll()).thenReturn(List.of(unit));

        SteamPlayerResponse result = service.findBySteamId(STEAM_ID, 1, 20);

        assertThat(result.displayName()).isEqualTo("HotWinter");
        assertThat(result.source()).isEqualTo("BARMORY");
        assertThat(result.career().winRate()).isEqualByComparingTo("60.00");
        assertThat(result.recentMatches()).singleElement().satisfies(match -> {
            assertThat(match.matchId()).isEqualTo(7448867L);
            assertThat(match.won()).isTrue();
            assertThat(match.mapName()).isEqualTo("Map #22");
        });
        assertThat(result.mostUsedUnits()).singleElement().satisfies(unitStats -> {
            assertThat(unitStats.unitName()).isEqualTo("M1A1 Abrams");
            assertThat(unitStats.kills()).isEqualTo(3);
        });
    }

    @Test
    void fallsBackToBattleGroupWhenBarmoryIsUnavailable() throws Exception {
        when(gateway.findCommander(STEAM_ID))
                .thenThrow(new ExternalProviderException("blocked"));
        when(battleGroupGateway.findPlayerStats(STEAM_ID)).thenReturn(json("""
                {
                  "found":true,
                  "steam_id":"76561198157609957",
                  "steam":{"personaName":"HotWinter"},
                  "user":{"id":27516,"name":"HotWinter","level":88,
                    "rating":3500.85,"rank":1},
                  "stats":{"kdRatio":1.75,"fightsCount":973,
                    "winsCount":815,"losesCount":149,"killsCount":37889,
                    "deathsCount":21559,"totalMatchTimeSec":1885444,
                    "capturedZonesCount":1833},
                  "recent":[{"fightId":"7469463","mapName":"Baltiisk",
                    "endTime":1783274193,"result":"win",
                    "oldRating":3499.3,"ratingChange":1.55}]
                }
                """));

        SteamPlayerResponse result = service.findBySteamId(STEAM_ID, 8, 20);

        assertThat(result.source()).isEqualTo("BATTLEGROUP");
        assertThat(result.career().matches()).isEqualTo(973);
        assertThat(result.recentMatches()).singleElement().satisfies(match -> {
            assertThat(match.mapName()).isEqualTo("Baltiisk");
            assertThat(match.newRating()).isEqualByComparingTo("3500.85");
        });
    }

    private JsonNode json(String value) throws Exception {
        return mapper.readTree(value);
    }
}
