package it.alessiogori.battledebrief.unit.mapper;

import it.alessiogori.battledebrief.unit.dto.SpecializationResponse;
import it.alessiogori.battledebrief.unit.dto.UnitResponse;
import it.alessiogori.battledebrief.unit.entity.Specialization;
import it.alessiogori.battledebrief.unit.entity.Unit;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class UnitMapper {

    public UnitResponse toResponse(Unit unit) {
        List<SpecializationResponse> specializations = unit
                .getSpecializations()
                .stream()
                .sorted(Comparator
                        .comparing(Specialization::getFaction)
                        .thenComparing(Specialization::getName))
                .map(this::toResponse)
                .toList();

        return new UnitResponse(
                unit.getId(),
                unit.getExternalUnitId(),
                unit.getName(),
                unit.getFaction(),
                unit.getCategory(),
                unit.getBaseCost(),
                unit.getDescription(),
                unit.getHitPoints(),
                unit.getSpeed(),
                unit.getArmor(),
                unit.getMainWeapon(),
                unit.getImageUrl(),
                unit.getDatasetVersion(),
                specializations
        );
    }

    public SpecializationResponse toResponse(Specialization specialization) {
        return new SpecializationResponse(
                specialization.getId(),
                specialization.getName(),
                specialization.getFaction(),
                specialization.getDescription()
        );
    }
}
