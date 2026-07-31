package it.alessiogori.battledebrief.match.repository;

import it.alessiogori.battledebrief.match.dto.MatchSearchCriteria;
import it.alessiogori.battledebrief.match.entity.GameMatch;
import it.alessiogori.battledebrief.match.entity.MatchPerformance;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GameMatchSpecifications {

    private GameMatchSpecifications() {
    }

    public static Specification<GameMatch> forPlayer(
            Long playerProfileId,
            MatchSearchCriteria criteria
    ) {
        return (root, query, builder) -> {
            Join<GameMatch, MatchPerformance> performance =
                    root.join("performances");
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(
                    performance.get("playerProfile").get("id"),
                    playerProfileId
            ));

            if (criteria.from() != null) {
                predicates.add(builder.greaterThanOrEqualTo(
                        root.get("startedAt"),
                        criteria.from()
                ));
            }
            if (criteria.to() != null) {
                predicates.add(builder.lessThanOrEqualTo(
                        root.get("startedAt"),
                        criteria.to()
                ));
            }
            if (criteria.won() != null) {
                predicates.add(builder.equal(
                        performance.get("won"),
                        criteria.won()
                ));
            }
            if (hasText(criteria.mapName())) {
                String pattern = "%" + escapeLike(
                        criteria.mapName().trim().toLowerCase(Locale.ROOT)
                ) + "%";
                predicates.add(builder.like(
                        builder.lower(root.get("mapName")),
                        pattern,
                        '\\'
                ));
            }
            if (criteria.minElo() != null) {
                predicates.add(builder.greaterThanOrEqualTo(
                        performance.get("newRating"),
                        criteria.minElo()
                ));
            }

            query.distinct(true);
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
