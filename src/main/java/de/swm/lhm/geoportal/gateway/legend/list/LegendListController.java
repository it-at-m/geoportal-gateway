package de.swm.lhm.geoportal.gateway.legend.list;

import de.swm.lhm.geoportal.gateway.shared.IsStageConfigurationCondition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "Legenden")
@RequiredArgsConstructor
@RestController
@Conditional(IsStageConfigurationCondition.class)
public class LegendListController {
    private final LegendListService legendListService;
    @Operation(
            description = "Pfade für Legenden anfragen",
            responses = {@ApiResponse(description = "Liste mit Pfaden für die Legenden")}
    )
    @GetMapping("/api/${geoportal.gateway.api.version}/legend")
    public List<String> getPathNames() {
        // here a blocking implementation is used for simplicity
        // this should not be a problem as
        // 1. this endpoint is only used in stage configuration
        // 2. the result is cached with the caffeine cache which is very fast
        return legendListService.getFiles();
    }

}
