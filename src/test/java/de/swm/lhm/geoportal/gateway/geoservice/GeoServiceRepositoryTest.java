package de.swm.lhm.geoportal.gateway.geoservice;

import de.swm.lhm.geoportal.gateway.authorization.model.AuthorizationGroup;
import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class GeoServiceRepositoryTest extends AbstractGeoServiceTest {

    @Autowired
    private GeoServiceRepository geoServiceRepository;

    @Test
    void testFindNonHiddenGeoServiceLayersWithRestrictedGrantedProducts() {
        Set<String> layers = geoServiceRepository.findNonHiddenGeoServiceLayers(
                        Stage.CONFIGURATION,
                        AuthorizationGroup.builder()
                                .productRoles(Set.of(PROTECTED_PRODUCT, RESTRICTED_PRODUCT))
                                .authLevelHigh(false)
                                .build()
                ).collectList()
                .map(Set::copyOf)
                .block();

        assertThat(layers, is(not(empty())));
        assertThat(layers, allOf(hasItem(PUBLIC_LAYER), hasItem(PROTECTED_LAYER), hasItem(PROTECTED_LAYER2), hasItem(RESTRICTED_LAYER)));
    }

    @Test
    void testFindNonHiddenGeoServiceLayersWithoutRestrictedGrantedProducts() {
        Set<String> layers = geoServiceRepository.findNonHiddenGeoServiceLayers(
                        Stage.CONFIGURATION,
                        AuthorizationGroup.builder()
                                .productRoles(Set.of(PROTECTED_PRODUCT))
                                .authLevelHigh(false)
                                .build()
                ).collectList()
                .map(Set::copyOf)
                .block();

        assertThat(layers, is(not(empty())));
        assertThat(layers, allOf(hasItem(PUBLIC_LAYER), hasItem(PROTECTED_LAYER), hasItem(PROTECTED_LAYER2), not(hasItem(RESTRICTED_LAYER))));

    }
}
