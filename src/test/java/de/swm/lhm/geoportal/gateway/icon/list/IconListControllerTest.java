package de.swm.lhm.geoportal.gateway.icon.list;

import com.github.benmanes.caffeine.cache.Cache;
import de.swm.lhm.geoportal.gateway.base_classes.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IconListControllerTest extends BaseIntegrationTest {
    public static String ICON_LIST_ENDPOINT = "/api/v1/icon";
    private static final int EXPECTED_LIST_SIZE = 145;
    private static final String EXAMPLE_LIST_ENTRY_EXPECTED = "icon://./plan/icons/3W1D2P/RYTFJQMLF/icon_W346Q58VSR.jpg";

    @Autowired
    IconListService iconService;

    @Test
    void testIconEndpointAndCaching() {
        shouldReturnIconsCorrectly();
        Cache<String, List<String>> cache = iconService.getCache();
        List<String> cachedResult = cache.getIfPresent(IconListService.CACHE_KEY);
        assertEquals(EXPECTED_LIST_SIZE, cachedResult.size());
        assertThat(cachedResult).contains(EXAMPLE_LIST_ENTRY_EXPECTED);
        shouldReturnIconsCorrectly(); // using cache

    }

    private void shouldReturnIconsCorrectly() {
        webTestClient
                .get()
                .uri(ICON_LIST_ENDPOINT)
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
