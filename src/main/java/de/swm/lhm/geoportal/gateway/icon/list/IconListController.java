package de.swm.lhm.geoportal.gateway.icon.list;

import de.swm.lhm.geoportal.gateway.shared.IsStageConfigurationCondition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Icons")
@RequiredArgsConstructor
@RestController
@Conditional(IsStageConfigurationCondition.class)
public class IconListController {
    private final IconListService iconListService;
    @Operation(
            description = "Pfade für Icons anfragen",
            responses = {@ApiResponse(description = "Liste mit Pfaden für die Icons")}
    )
    @GetMapping("/api/${geoportal.gateway.api.version}/icon")
    public List<String> getPathNames() {
        // here a blocking implementation is used for simplicity
        // this should not be a problem as
        // 1. this endpoint is only used in stage configuration
        // 2. the result is cached with the caffeine cache which is very fast
        return iconListService.getFiles();
    }

}
