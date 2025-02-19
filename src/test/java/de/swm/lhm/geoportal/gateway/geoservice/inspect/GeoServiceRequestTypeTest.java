package de.swm.lhm.geoportal.gateway.geoservice.inspect;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class GeoServiceRequestTypeTest {

    @Test
    void parseSuccessfully() {
        assertThat(GeoServiceRequestType.getFromString("GetCapabilities"), is(Optional.of(GeoServiceRequestType.GET_CAPABILITIES)));
    }

    @Test
    void parseFailure() {
        assertThat(GeoServiceRequestType.getFromString("does-not-exist"), is(Optional.empty()));
    }
}
