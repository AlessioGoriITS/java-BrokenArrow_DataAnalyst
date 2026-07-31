package it.alessiogori.battledebrief.match.service;

import it.alessiogori.battledebrief.common.dto.PageResponse;
import it.alessiogori.battledebrief.common.exception.InvalidRequestException;
import it.alessiogori.battledebrief.common.exception.ResourceNotFoundException;
import it.alessiogori.battledebrief.match.dto.MatchDetailResponse;
import it.alessiogori.battledebrief.match.dto.MatchSearchCriteria;
import it.alessiogori.battledebrief.match.dto.PlayerMatchSummaryResponse;
import it.alessiogori.battledebrief.match.mapper.MatchMapper;
import it.alessiogori.battledebrief.match.repository.GameMatchRepository;
import it.alessiogori.battledebrief.match.repository.GameMatchSpecifications;
import it.alessiogori.battledebrief.player.repository.PlayerProfileRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchQueryServiceImpl implements MatchQueryService {

    private final GameMatchRepository gameMatchRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final MatchMapper matchMapper;

    public MatchQueryServiceImpl(
            GameMatchRepository gameMatchRepository,
            PlayerProfileRepository playerProfileRepository,
            MatchMapper matchMapper
    ) {
        this.gameMatchRepository = gameMatchRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.matchMapper = matchMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PlayerMatchSummaryResponse> findPlayerMatches(
            Long playerProfileId,
            MatchSearchCriteria criteria,
            Pageable pageable
    ) {
        if (!playerProfileRepository.existsById(playerProfileId)) {
            throw new ResourceNotFoundException("Player profile not found");
        }
        validateCriteria(criteria);
        return PageResponse.from(gameMatchRepository.findAll(
                GameMatchSpecifications.forPlayer(playerProfileId, criteria),
                pageable
        ).map(match -> matchMapper.toSummary(match, playerProfileId)));
    }

    @Override
    @Transactional(readOnly = true)
    public MatchDetailResponse getById(Long matchId) {
        return gameMatchRepository.findById(matchId)
                .map(matchMapper::toDetail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Match not found"
                ));
    }

    private void validateCriteria(MatchSearchCriteria criteria) {
        if (criteria.from() != null
                && criteria.to() != null
                && criteria.from().isAfter(criteria.to())) {
            throw new InvalidRequestException(
                    "from cannot be later than to"
            );
        }
        if (criteria.minElo() != null && criteria.minElo() < 0) {
            throw new InvalidRequestException("minElo cannot be negative");
        }
    }
}
