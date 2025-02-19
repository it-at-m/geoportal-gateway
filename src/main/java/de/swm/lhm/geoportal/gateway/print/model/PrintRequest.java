package de.swm.lhm.geoportal.gateway.print.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Sets;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import de.swm.lhm.geoportal.gateway.util.UuidUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrintRequest {
    private List<String> visibleLayerIds;

    private Attributes attributes;

   @JsonIgnore
    public Set<QualifiedLayerName> getLayerNamesForAuthorization(){
        return getAllLayerNames()
                .stream()
                // generic layers have a UUID as name. These shall bypass authorization and are always allowed
                .filter(s -> !UuidUtils.isUuid(s))
                // masterportal allows the user to draw on the map. These geometries are then
                // stored in a layer named "importDrawLayer". This layer shall bypass authorization
                // to be printable
                .filter(s -> !"importDrawLayer".equals(s))
                .map(QualifiedLayerName::fromString)
                .collect(Collectors.toSet());
    }

    @JsonIgnore
    public Set<String> getAllLayerNames(){
        Set<String> layerNames = Sets.newHashSet();
        layerNames.addAll(getVisibleLayerIds());

        if (getAttributes() != null) {
            Gfi gfi = getAttributes().getGfi();
            if (gfi != null) {
                layerNames.addAll(getLayerNames(gfi.getLayers()));
            }
            Legend legend = getAttributes().getLegend();
            if (legend != null) {
                layerNames.addAll(getLayerNames(legend.getLayers()));
            }
            Map map = getAttributes().getMap();
            if (map != null) {
                layerNames.addAll(getLayerNames(map.getLayers()));
            }
        }
        return layerNames;
    }

    private Set<String> getLayerNames(List<LayersItem> layersItems) {
        Set<String> layerNames = Sets.newHashSet();
        if (layersItems != null) {
            layersItems.stream().filter(layerItem -> layerItem != null && layerItem.getLayers() != null)
                    .forEach(layersItem -> layerNames.addAll(layersItem.getLayers()));
        }
        return layerNames;
    }
}