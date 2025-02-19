package de.swm.lhm.geoportal.gateway.search.model.masterportal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "Anfrageobjekt für Portalsuche")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PortalSearchRequest {

    @Schema(
            description = "ID der Suchanfrage"
    )
    private String id;

    @Schema(
            description = "Maximale Anzahl an Ergebnissen die zurück gegeben werden sollen"
    )
    private Integer maxResultAmount;

    @Schema(
            description = "ID des Portals"
    )
    private Integer portalId;

    @Schema(
            description = "Versetzung der Suche (offset)"
    )
    private Integer searchOffset;

    @Schema(
            description = "Suchbegriff"
    )
    private String searchString;
}
