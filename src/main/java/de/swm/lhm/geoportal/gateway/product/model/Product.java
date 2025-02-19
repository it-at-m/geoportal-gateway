package de.swm.lhm.geoportal.gateway.product.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import de.swm.lhm.geoportal.gateway.authorization.model.AccessLevel;
import de.swm.lhm.geoportal.gateway.portal.model.Portal;
import de.swm.lhm.geoportal.gateway.resource.model.FileResource;
import de.swm.lhm.geoportal.gateway.shared.model.ServiceType;
import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Schema(name = "Produkt")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "t_product")
public class Product {

    @Schema(hidden = true)
    @Id
    @Column("id")
    @JsonIgnore
    Integer id;

    @Schema(description = "Name")
    @Column("name")
    String name;

    @Schema(description = "Titel")
    @Column("title")
    String title;

    @Schema(description = "Beschreibung")
    @Column("description")
    String description;

    @Schema(description = "Lizenz")
    @Column("license")
    License license;

    @Schema(hidden = true)
    @Column("stage")
    @JsonIgnore
    Stage stage;

    @Schema(hidden = true)
    @Column("metadata_id")
    @JsonIgnore
    Integer metadataId;

    @Schema(hidden = true)
    @Transient
    @JsonIgnore
    UUID metadataUuid;

    @Schema(description = "Metadaten URL")
    @Transient
    String metadataUrl;

    @Schema(hidden = true)
    @Column("unit_id")
    @JsonIgnore
    Integer unitId;

    @Schema(description = "Organisationseinheit")
    @Transient
    String unit;

    @Schema(description = "Dateiname für Header Bild")
    @Column("header_image_file_name")
    String headerImageFileName;

    @Schema(hidden = true)
    @Column("header_image")
    @JsonIgnore
    byte[] headerImageBytes;

    @Schema(description = "Url von Logo")
    @Transient
    String logoUrl;

    @Schema(description = "Zugriffsebene")
    @Column("access_level")
    AccessLevel accessLevel;

    @Schema(description = "Ist es eine Zugriffsebene?")
    @Column("auth_level_high")
    Boolean authLevelHigh;

    @Schema(hidden = true)
    @Column("role_name")
    @JsonIgnore
    String roleName;

    @Schema(description = "Liste der Portale")
    @Transient
    @Builder.Default
    List<Portal> portals = new ArrayList<>();

    @Schema(description = "Liste der Geoservices")
    @Transient
    @Builder.Default
    List<Service> geoServices = new ArrayList<>();

    @Schema(description = "Liste der Dateiresourcen")
    @Transient
    @Builder.Default
    List<FileResource> fileResources = new ArrayList<>();

    public void addGeoService(Service service) {
        geoServices.add(service);
    }

    public void addFileResource(FileResource fileResource) {
        fileResources.add(fileResource);
    }

    public void addPortal(Portal portal) {
        portals.add(portal);
    }

    @Schema(description = "Service")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Service {

        @Schema(description = "Name")
        String name;

        @Schema(description = "Arbeitsplatz")
        String workspace;

        @Schema(description = "Zugriffsebene")
        AccessLevel accessLevel;

        @Schema(description = "Ist es eine hohe Zugriffsebene?")
        Boolean authLevelHigh;

        @Schema(description = "URLs")
        List<ServiceUrl> urls;

        @Schema(description = "Service URL")
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ServiceUrl {

            @Schema(description = "Typ")
            ServiceType serviceType;

            @Schema(description = "URL")
            String url;
        }

    }


}
