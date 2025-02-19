package de.swm.lhm.geoportal.gateway.legend.list;

import com.github.benmanes.caffeine.cache.Cache;
import de.swm.lhm.geoportal.gateway.base_classes.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
class LegendListControllerTest extends BaseIntegrationTest {
    private static final String LEGEND_LIST_ENDPOINT = "/api/v1/legend";
    private static final int EXPECTED_LIST_SIZE = 106;
    private static final String EXAMPLE_LIST_ENTRY_EXPECTED = "legend://./plan/legends/TB2AOJMIPG/KZO7571T9E/legend_G75K1RD9M.png";

    @Autowired
    LegendListService legendService;

    @Test
    void testLegendEndpointAndCaching() {
        shouldReturnLegendsCorrectly();
        Cache<String, List<String>> cache = legendService.getCache();
        List<String> cachedResult = cache.getIfPresent(LegendListService.CACHE_KEY);
        assertEquals(EXPECTED_LIST_SIZE, cachedResult.size());
        assertThat(cachedResult).contains(EXAMPLE_LIST_ENTRY_EXPECTED);
        shouldReturnLegendsCorrectly(); // using cache
    }

    private void shouldReturnLegendsCorrectly() {
        webTestClient
                    .get()
                    .uri(LEGEND_LIST_ENDPOINT)
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody(new ParameterizedTypeReference<List<String>>() {
                    })
                    .consumeWith(listEntityExchangeResult -> {
                        List<String> body = listEntityExchangeResult.getResponseBody();
                        assertEquals(EXPECTED_LIST_SIZE, body.size());
                        assertThat(body).contains(EXAMPLE_LIST_ENTRY_EXPECTED);
                    });
    }

}
