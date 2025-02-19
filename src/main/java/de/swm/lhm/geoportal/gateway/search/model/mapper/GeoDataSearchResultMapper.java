package de.swm.lhm.geoportal.gateway.search.model.mapper;

import com.google.common.collect.Lists;
import de.swm.lhm.geoportal.gateway.search.model.elastic.GeoDataDocument;
import de.swm.lhm.geoportal.gateway.search.model.masterportal.SearchResultTo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;


/**
 * mapper to map {@link GeoDataDocument} and {@link SearchResultTo}
 */

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GeoDataSearchResultMapper extends ResultMapper<GeoDataDocument>{

    /**
     * map from {@link GeoDataDocument} to {@link SearchResultTo}
     *
     * @param geoDataDocument to map
     * @return the {@link SearchResultTo}
     */


    @Override
    @Mapping(target = "type", expression = "java(de.swm.lhm.geoportal.gateway.search.model.masterportal.SearchResultType.GEO_DATA.getSearchResultType())")
    @Mapping(target = "displayValue", expression = "java(mapDisplayValue(geoDataDocument))")
    @Mapping(target = "coordinate", expression = "java(mapCoordinates(geoDataDocument))")
    SearchResultTo map(GeoDataDocument geoDataDocument);

    default String mapDisplayValue(GeoDataDocument geoDataDocument) {
        return String.format("%s (%s)", geoDataDocument.getGeoDataValue(), geoDataDocument.getLayerTitle());
    }

    default List<Double> mapCoordinates(GeoDataDocument geoDataDocument) {
        List<Double> coordinates = Lists.newArrayList();
        coordinates.add(geoDataDocument.getRightValue());
        coordinates.add(geoDataDocument.getTopValue());
        return coordinates;
    }
}