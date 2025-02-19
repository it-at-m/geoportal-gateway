package de.swm.lhm.geoportal.gateway.loadbalancer;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;


@ExtendWith({SpringExtension.class, OutputCaptureExtension.class})
@EnableConfigurationProperties(value = LoadBalancerProperties.class)
@TestPropertySource("classpath:loadbalancer/load-balancer-properties-test.properties")
class LoadBalancerPropertiesTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private LoadBalancerProperties loadBalancerProperties;

    @Test
    void propertiesTest(CapturedOutput output) throws URISyntaxException {

        Set<String> expectedServiceIds = new HashSet<>(
                Arrays.asList("test__1__", "test__2__", "test__3__", "test__4__", "test__5__", "test__6__")
        );

        assertThat(loadBalancerProperties.getServiceIds(), is(expectedServiceIds));

        for (String serviceId : expectedServiceIds) {
            List<LoadBalancerServiceInstance> urls = loadBalancerProperties.getServicesMap().get(serviceId);
            List<LoadBalancerServiceInstance> expectedUrls = new ArrayList<>(
                    Arrays.asList(
                            new LoadBalancerServiceInstance(serviceId, "http://localhost:8083"),
                            new LoadBalancerServiceInstance(serviceId, "http://localhost:8084")
                    )
            );

            assertThat(expectedUrls, is(urls));
        }

        Set<String> expectedStickyIds = new HashSet<>(
                Arrays.asList("test__1__", "test__2__")
        );

        assertThat(loadBalancerProperties.getStickyMap().keySet(), is(expectedStickyIds));
        assertThat(
                output.getAll(),
                allOf(
                        containsString("test__8__.sticky=abc is not a boolean"),
                        containsString("LoadBalancerProperties")
                )
        );
    }

}
