package de.swm.lhm.geoportal.gateway.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

class ExtendedURIBuilderTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "  ",
            "/"
    })
    void addPathShouldLeaveUriBuilderUnchanged(String path) throws URISyntaxException {
        ExtendedURIBuilder uriBuilder = new ExtendedURIBuilder("http://localhost:123");
        assertThat(uriBuilder.addPath(path)).isEqualTo(uriBuilder);
    }

    @Test
    void shouldAddPathCorrectly() throws URISyntaxException {
        ExtendedURIBuilder uriBuilder = new ExtendedURIBuilder("http://localhost:123");
        ExtendedURIBuilder updated = uriBuilder.addPath("subpath");
        assertThat(updated.getPath()).isEqualTo("/subpath");
    }
}