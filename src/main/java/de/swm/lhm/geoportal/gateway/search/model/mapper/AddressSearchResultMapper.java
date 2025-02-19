package de.swm.lhm.geoportal.gateway.search.model.mapper;

import de.swm.lhm.geoportal.gateway.search.model.elastic.AddressDocument;
import de.swm.lhm.geoportal.gateway.search.model.masterportal.SearchResultTo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * mapper for {@link AddressDocument} to {@link SearchResultTo}
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AddressSearchResultMapper extends ResultMapper<AddressDocument>{

    /**
     * mapping from {@link AddressDocument} to {@link SearchResultTo}
     *
     * @param addressDocument to map
     * @return the AddressSearchResultTo
     */

    @Override
    @Mapping(target = "displayValue", expression = "java(java.lang.String.format(\"%s, %s %s\", addressDocument.getStreetNameComplete(), addressDocument.getZipCode(), addressDocument.getCity()))")
    @Mapping(target = "coordinate", expression = "java(com.google.common.collect.Lists.newArrayList(addressDocument.getRightValue(), addressDocument.getTopValue()))")
    @Mapping(target = "type", expression = "java(de.swm.lhm.geoportal.gateway.search.model.masterportal.SearchResultType.ADDRESS.getSearchResultType())")
    SearchResultTo map(AddressDocument addressDocument);
}