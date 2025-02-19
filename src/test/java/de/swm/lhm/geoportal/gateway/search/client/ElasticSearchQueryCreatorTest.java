package de.swm.lhm.geoportal.gateway.search.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ElasticSearchQueryCreatorTest {
    private final ElasticSearchQueryCreator queryCreator = new ElasticSearchQueryCreator(new ObjectMapper());

    @Test
    void testCreateGeoDataQuery(){
        String searchString = "Searched Geo Data";
        String expectedQuery =  String.format(
                "{\"query\":{\"match\":{\"geoDataValue\":\"%s\"}}}",
                searchString);
        String actualQuery = queryCreator.createGeoDataQuery(searchString);
        Assertions.assertEquals(expectedQuery, actualQuery);
    }

    @Test
    void testCreateAddressQueryWithStreetWithHouseNumber(){
        String expectedQuery = "{\"size\":10,\"query\":{\"bool\":{\"must\":[{\"match\":{\"streetName\":{\"query\":\"Klenzestraße\",\"operator\":\"and\",\"fuzziness\":\"auto\"}}},{\"prefix\":{\"houseNumber\":\"3\"}}]}}}";
        String actualQuery = queryCreator.createAddressQuery("Klenzestraße 3", 10);
        Assertions.assertEquals(expectedQuery, actualQuery);
    }

    @Test
    void testCreateAddressQueryWithStreetWithoutHouseNumber() {
        String expectedQuery = "{\"size\":10,\"query\":{\"bool\":{\"must\":[{\"match\":{\"streetName\":{\"query\":\"Klenzestraße\",\"operator\":\"and\",\"fuzziness\":\"auto\"}}}]}}}";
        String actualQuery = queryCreator.createAddressQuery("Klenzestraße", 10);
        Assertions.assertEquals(expectedQuery, actualQuery);
    }
}