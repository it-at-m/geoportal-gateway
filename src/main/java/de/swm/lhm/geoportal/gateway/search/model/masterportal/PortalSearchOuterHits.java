package de.swm.lhm.geoportal.gateway.search.model.masterportal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PortalSearchOuterHits {

    @Schema(
            description = "Liste aller Suchergebnisse"
    )
    private List<SearchResultTo> hits;
}
