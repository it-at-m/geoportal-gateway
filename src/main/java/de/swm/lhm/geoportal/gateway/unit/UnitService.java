package de.swm.lhm.geoportal.gateway.unit;


import de.swm.lhm.geoportal.gateway.unit.model.Unit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Service
@RequiredArgsConstructor
public class UnitService {

    private final UnitRepository unitRepository;

    public Mono<String> getUnitNameById(Integer unitId) {
        return unitRepository.findUnitById(unitId)
                .map(Unit::getName);
    }

}
