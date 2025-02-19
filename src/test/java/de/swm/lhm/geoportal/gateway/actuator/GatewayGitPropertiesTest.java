package de.swm.lhm.geoportal.gateway.actuator;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


class GatewayGitPropertiesTest {

    @Test
    void asMap() {

        GatewayGitProperties properties = new GatewayGitProperties();
        Map<String, String> result = properties.asMap();

        testMap(result);

        properties.setCommit(new GatewayGitProperties.GitCommit());
        result = properties.asMap();

        testMap(result);

        properties.setCommit(new GatewayGitProperties.GitCommit(new GatewayGitProperties.GitId()));
        result = properties.asMap();

        testMap(result);

    }

    private void testMap(Map<String, String> theMap){
        assertThat(theMap, allOf(aMapWithSize(3), hasKey("version"), hasKey("id"), hasKey("time")));
    }
}