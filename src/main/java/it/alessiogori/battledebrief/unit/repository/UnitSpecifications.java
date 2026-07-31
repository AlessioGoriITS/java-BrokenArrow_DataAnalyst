package it.alessiogori.battledebrief.unit.repository;

import it.alessiogori.battledebrief.unit.dto.UnitSearchCriteria;
import it.alessiogori.battledebrief.unit.entity.Specialization;
import it.alessiogori.battledebrief.unit.entity.Unit;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class UnitSpecifications {

    private UnitSpecifications() {
    }

    public static Specification<Unit> matching(UnitSearchCriteria criteria) {
        Specification<Unit> specification = (root, query, builder) ->
                builder.conjunction();

        if (hasText(criteria.name())) {
            String pattern = "%" + escapeLike(
                    criteria.name().trim().toLowerCase(Locale.ROOT)
            ) + "%";
            specification = specification.and((root, query, builder) ->
                    builder.like(
                            builder.lower(root.get("name")),
                            pattern,
                            '\\'
                    )
            );
        }
        if (hasText(criteria.faction())) {
            String faction = criteria.faction().trim().toLowerCase(Locale.ROOT);
            specification = specification.and((root, query, builder) ->
                    builder.equal(builder.lower(root.get("faction")), faction)
            );
        }
        if (hasText(criteria.category())) {
            String category = criteria.category().trim().toLowerCase(Locale.ROOT);
            specification = specification.and((root, query, builder) ->
                    builder.equal(builder.lower(root.get("category")), category)
            );
        }
        if (criteria.specializationId() != null) {
            specification = specification.and((root, query, builder) -> {
                Join<Unit, Specialization> specialization = root.join(
                        "specializations",
                        JoinType.INNER
                );
                query.distinct(true);
                return builder.equal(
                        specialization.get("id"),
                        criteria.specializationId()
                );
            });
        }
        if (criteria.minCost() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(
                            root.get("baseCost"),
                            criteria.minCost()
                    )
            );
        }
        if (criteria.maxCost() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(
                            root.get("baseCost"),
                            criteria.maxCost()
                    )
            );
        }

        return specification;
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
