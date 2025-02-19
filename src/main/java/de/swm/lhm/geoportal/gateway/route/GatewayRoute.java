package de.swm.lhm.geoportal.gateway.route;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayRoute {

    private String routeId;
    private String url;
    private HttpMethod method;
    private String path;
    private Integer stripPrefix;
    @Builder.Default
    private List<GatewayFilter> gatewayFilters = new ArrayList<>();

}
