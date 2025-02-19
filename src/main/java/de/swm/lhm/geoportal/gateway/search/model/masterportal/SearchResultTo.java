package de.swm.lhm.geoportal.gateway.search.model.masterportal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResultTo {

    @Schema(
            description = "ID des Ergebnisses"
    )
    @JsonProperty(required = true)
    private String id;

    @Schema(
            description = "Typ"
    )
    @JsonProperty(required = true)
    private String type;

    @Schema(
            description = "Koordinaten"
    )
    @JsonProperty(required = true)
    private List<Double> coordinate;

    @Schema(
            description = "Anzeigename"
    )
    @JsonProperty(required = true)
    private String displayValue;
    // GeoData Parameters
    @Schema(
            description = "ID der Ebene"
    )
    private String layerId;

    @Schema(
            description = "Name der Ebene"
    )
    private String layerTitle;

    @Schema(
            description = "Wert der Geodaten"
    )
    private String geoDataValue;
    //Address Parameters
    @Schema(
            description = "Straße"
    )
    private String streetName;

    @Schema(
            description = "Stadt"
    )
    private String city;

    @Schema(
            description = "Postleitzahl"
    )
    private String zipCode;

    @Schema(
            description = "Straßenname mit Postleitzahl"
    )
    private String streetNameComplete;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SearchResultTo)) return false;
        SearchResultTo that = (SearchResultTo) o;
        return Objects.equals(id, that.id) && Objects.equals(type, that.type)
                && coordinate.size() == that.coordinate.size()
                && coordinate.containsAll(that.coordinate)
                && Objects.equals(displayValue, that.displayValue)
                && Objects.equals(layerId, that.layerId) && Objects.equals(layerTitle, that.layerTitle)
                && Objects.equals(geoDataValue, that.geoDataValue)
                && Objects.equals(streetName, that.streetName)
                && Objects.equals(city, that.city)
                && Objects.equals(zipCode, that.zipCode)
                && Objects.equals(streetNameComplete, that.streetNameComplete);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, coordinate, displayValue, layerId, layerTitle, geoDataValue, streetName, city, zipCode, streetNameComplete);
    }
}
