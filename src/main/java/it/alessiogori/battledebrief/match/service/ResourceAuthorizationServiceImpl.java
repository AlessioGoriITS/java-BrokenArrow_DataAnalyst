package it.alessiogori.battledebrief.match.service;

import it.alessiogori.battledebrief.match.repository.MatchPerformanceRepository;
import it.alessiogori.battledebrief.player.repository.PlayerProfileRepository;
import it.alessiogori.battledebrief.user.entity.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("resourceAuthorization")
public class ResourceAuthorizationServiceImpl
        implements ResourceAuthorizationService {

    private final PlayerProfileRepository playerProfileRepository;
    private final MatchPerformanceRepository matchPerformanceRepository;

    public ResourceAuthorizationServiceImpl(
            PlayerProfileRepository playerProfileRepository,
            MatchPerformanceRepository matchPerformanceRepository
    ) {
        this.playerProfileRepository = playerProfileRepository;
        this.matchPerformanceRepository = matchPerformanceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAccessPlayer(
            Long playerProfileId,
            Long userId,
            Role role
    ) {
        return role == Role.ADMIN || playerProfileRepository
                .existsByIdAndUserId(playerProfileId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAccessMatch(Long matchId, Long userId, Role role) {
        return role == Role.ADMIN || matchPerformanceRepository
                .existsByGameMatchIdAndPlayerProfileUserId(matchId, userId);
    }
}
