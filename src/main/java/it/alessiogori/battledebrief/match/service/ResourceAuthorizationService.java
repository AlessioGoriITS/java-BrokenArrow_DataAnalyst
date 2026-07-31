package it.alessiogori.battledebrief.match.service;

import it.alessiogori.battledebrief.user.entity.Role;

public interface ResourceAuthorizationService {

    boolean canAccessPlayer(Long playerProfileId, Long userId, Role role);

    boolean canAccessMatch(Long matchId, Long userId, Role role);
}
