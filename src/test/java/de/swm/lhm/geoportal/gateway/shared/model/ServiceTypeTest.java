package de.swm.lhm.geoportal.gateway.shared.model;

import org.junit.jupiter.api.Test;

import static de.swm.lhm.geoportal.gateway.base_classes.hamcrest.HamcrestCompareJsonMatcher.equalToJSON;
import static org.hamcrest.MatcherAssert.assertThat;


class ServiceTypeTest {

    @Test
    void toJson(){
        assertThat(ServiceType.STA, equalToJSON("\"STA\""));
    }

}