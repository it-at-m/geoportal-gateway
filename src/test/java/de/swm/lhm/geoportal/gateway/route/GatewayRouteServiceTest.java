package de.swm.lhm.geoportal.gateway.route;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
class GatewayRouteServiceTest {

    @InjectMocks
    private GatewayRouteService gatewayRouteService;

    @Mock
    private GatewayRoutesCollector collector1;

    @Mock
    private GatewayRoutesCollector collector2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        gatewayRouteService = new GatewayRouteService(List.of(collector1, collector2));
    }

    @Test
    void testGetAllSuccess() {
        GatewayRoute route1 = GatewayRoute.builder().routeId("route1").build();
        GatewayRoute route2 = GatewayRoute.builder().routeId("route2").build();

        when(collector1.getAllGatewayRoutes()).thenReturn(Flux.just(route1));
        when(collector2.getAllGatewayRoutes()).thenReturn(Flux.just(route2));

        Flux<GatewayRoute> allRoutes = gatewayRouteService.getAll();

        StepVerifier.create(allRoutes)
                    .expectNext(route1)
                    .expectNext(route2)
                    .verifyComplete();

        verify(collector1, times(1)).getAllGatewayRoutes();
        verify(collector2, times(1)).getAllGatewayRoutes();
    }

    @Test
    void testGetAllWithException(CapturedOutput output) {
        GatewayRoute route1 = GatewayRoute.builder().routeId("route1").build();

        when(collector1.getAllGatewayRoutes()).thenReturn(Flux.just(route1));
        when(collector2.getAllGatewayRoutes()).thenThrow(new RuntimeException("Test exception"));

        Flux<GatewayRoute> allRoutes = gatewayRouteService.getAll();

        StepVerifier.create(allRoutes)
                    .expectNext(route1)
                    .verifyComplete();

        assertThat(output.toString(), containsString("Failed to call GatewayRoutesCollector"));

        verify(collector1, times(1)).getAllGatewayRoutes();
        verify(collector2, times(1)).getAllGatewayRoutes();
    }
}