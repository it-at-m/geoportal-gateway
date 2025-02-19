package de.swm.lhm.geoportal.gateway.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


class EndpointUtilsTest {

    @Test
    void prepareEndpointEmpty() {

        EndpointUtils.Endpoints ep = EndpointUtils.prepareEndpoint("");

        assertThat(ep.getEndpointWithoutSlashes(), is(""));
        assertThat(ep.getEndpointWithSlashes(), is("/"));
        assertThat(ep.getEndpointWithSlashesLength(), is(1));

    }

    @ParameterizedTest
    @ValueSource(strings = {"/test", "test/", "/test/", "test"})
    void prepareEndpoint(String endpoint) {

        EndpointUtils.Endpoints ep = EndpointUtils.prepareEndpoint(endpoint);

        assertThat(ep.getEndpointWithoutSlashes(), is("test"));
        assertThat(ep.getEndpointWithSlashes(), is("/test/"));
        assertThat(ep.getEndpointWithSlashesLength(), is(6));

    }

    @Test
    void prepareEndpoint2() {

        EndpointUtils.Endpoints ep = EndpointUtils.prepareEndpoint("/test/");

        assertThat(ep.getEndpointWithoutSlashes(), is("test"));
        assertThat(ep.getEndpointWithSlashes(), is("/test/"));
        assertThat(ep.getEndpointWithSlashesLength(), is(6));

    }
}