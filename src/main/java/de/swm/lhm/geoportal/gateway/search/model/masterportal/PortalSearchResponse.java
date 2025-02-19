package de.swm.lhm.geoportal.gateway.search.model.masterportal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "Rückgabe einer Portalsuche")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PortalSearchResponse {

    @Schema(
            description = "Suchergebnisse"
    )
    private PortalSearchOuterHits hits;

}
