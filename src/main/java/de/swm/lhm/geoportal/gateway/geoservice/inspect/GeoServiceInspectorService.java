package de.swm.lhm.geoportal.gateway.geoservice.inspect;

import static de.swm.lhm.geoportal.gateway.util.ReactiveUtils.optionalToMono;

import de.swm.lhm.geoportal.gateway.geoservice.GeoServiceProperties;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.xml.GeoServiceXmlRequestDocumentParser;
import de.swm.lhm.geoportal.gateway.shared.model.ServiceType;
import de.swm.lhm.geoportal.gateway.util.DataBufferUtils;
import de.swm.lhm.geoportal.gateway.util.HttpHeaderUtils;
import de.swm.lhm.geoportal.gateway.util.ReactiveUtils;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;


@Service
@RequiredArgsConstructor
@Slf4j
public class GeoServiceInspectorService {

    private static final Pattern RE_GLOBAL_WMS_WFS_PATTERN = Pattern.compile(
            "^/?(?<endpoint>(wms|wfs|ows))(?<queryparams>\\?.*)?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern RE_QUALIFIED_WMS_WFS_PATTERN = Pattern.compile(
            "^/?(?<workspace>[^/]+)(/(?<layer>[^/]+))?/(?<endpoint>(wms|wfs|ows))(?<queryparams>\\?.*)?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern RE_WMTS_PATTERN = Pattern.compile(
            "^/?gwc/service/wmts(?<queryparams>\\?.*)?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final String CACHED_GEOSERVICE_REQUEST_ATTR = "geoServiceRequest";
    private final GeoServiceXmlRequestDocumentParser geoServiceXmlRequestDocumentParser = new GeoServiceXmlRequestDocumentParser();

    @Getter
    private final GeoServiceProperties geoServiceProperties;

    public String getGeoServicePathPrefix() {
        return geoServiceProperties.getEndpoint();
    }

    public Mono<Boolean> isSupportedGeoServiceRequest(ServerWebExchange serverWebExchange) {
        // only proxy requests the inspector understands, as those are parts of the public API.
        // This behavior goes hand in hand with the Authorization process implemented
        // in the GeoServiceRequestMatcher class.
        return inspectRequestAndCache(serverWebExchange)
                .map(geoServiceRequest -> true)
                .switchIfEmpty(Mono.just(false));
    }

    public Mono<GeoServiceRequest> inspectRequestAndCache(ServerWebExchange serverWebExchange) {
        Optional<GeoServiceRequest> geoServiceRequestOptional = serverWebExchange.getAttribute(CACHED_GEOSERVICE_REQUEST_ATTR);

        if (geoServiceRequestOptional != null) {
            log.trace("Reusing cached previous GeoService request");
            return ReactiveUtils.optionalToMono(geoServiceRequestOptional);
        }

        return inspectRequest(serverWebExchange)
                .doOnSuccess(nullableGeoServiceRequest -> serverWebExchange.getAttributes()
                        .put(CACHED_GEOSERVICE_REQUEST_ATTR, Optional.ofNullable(nullableGeoServiceRequest))
                );
    }

    public Mono<GeoServiceRequest> inspectRequest(ServerWebExchange serverWebExchange) {
        ServerHttpRequest request = serverWebExchange.getRequest();

        if (Objects.equals(request.getMethod(), HttpMethod.GET) || Objects.equals(request.getMethod(), HttpMethod.HEAD)) {
            return optionalToMono(inspectQueryParametersOfRequest(
                    request.getMethod(),
                    request.getPath().value(),
                    request.getQueryParams()
            ));
        } else if (Objects.equals(request.getMethod(), HttpMethod.POST)) {
            return inspectPostRequest(serverWebExchange);
        } else {
            return Mono.empty(); // not supported
        }
    }

    /**
     * inspect a get request with attached query parameters
     */
    public Optional<GeoServiceRequest> inspectGetRequestWithQueryParams(String requestPathWithQueryString) {
        UriComponents uriComponents = UriComponentsBuilder
                .fromUriString(requestPathWithQueryString)
                .build();
        return inspectQueryParametersOfRequest(HttpMethod.GET, uriComponents.getPath(), uriComponents.getQueryParams());
    }

    private Mono<GeoServiceRequest> inspectPostRequest(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();

        // POSTs require an XML body
        if (!HttpHeaderUtils.isContentTypeXml(request.getHeaders())) {
            return Mono.empty();
        }

        GeoServiceRequest geoServiceRequest = new GeoServiceRequest();
        geoServiceRequest.setHttpMethod(request.getMethod());

        return optionalToMono(setPathBasedProperties(geoServiceRequest, request.getPath().value()))
                .flatMap(geoServiceRequest1 -> DataBufferUtils.copyAsString(request.getBody())
                        .flatMap(body ->
                                ReactiveUtils.runInBackgroundEmptyAfterTimeout(
                                                () -> geoServiceXmlRequestDocumentParser.parseXmlString(body),
                                                Duration.ofMillis(geoServiceProperties.getSanitizedMaxXmlParsingDurationMs()),
                                                () -> log.atWarn()
                                                        .setMessage(() -> String.format(
                                                                "Could not parse body of %s request to %s with the required time (%d ms) -> discarded",
                                                                request.getMethod(),
                                                                request.getPath().value(),
                                                                geoServiceProperties.getSanitizedMaxXmlParsingDurationMs()))
                                                        .log()
                                        )
                                        // Requests which can not be parsed will result in an empty Mono
                                        .flatMap(ReactiveUtils::optionalToMono)
                                        .map(parameters -> {
                                            geoServiceRequest1.setLayers(
                                                    // on workspace or even layer-scoped endpoints geoserver supports
                                                    // the omission of the workspace-part of the layer name as it is already
                                                    // present in the request path.
                                                    parameters.getRequestedLayers()
                                                            .stream()
                                                            .map(layerName -> QualifiedLayerName.fromStringWithWorkspaceFallback(geoServiceRequest1.getWorkspaceName(), layerName))
                                                            .filter(Optional::isPresent)
                                                            .map(Optional::get)
                                                            .collect(Collectors.toSet())
                                            );
                                            geoServiceRequest1.setServiceType(parameters.getGeoServiceType());
                                            geoServiceRequest1.setRequestType(Optional.of(parameters.getGeoServiceRequestType().getGetParameterValue()));
                                            return geoServiceRequest1;
                                        })
                        ));
    }

    public Optional<GeoServiceRequest> inspectQueryParametersOfRequest(HttpMethod requestHttpMethod, String requestPath, MultiValueMap<String, String> queryParams) {
        GeoServiceRequest geoServiceRequest = new GeoServiceRequest();
        geoServiceRequest.setHttpMethod(requestHttpMethod);

        return setPathBasedProperties(geoServiceRequest, requestPath)
                .flatMap(baseGeoServiceRequest -> setQueryParamBasedProperties(baseGeoServiceRequest, queryParams));
    }

    private Optional<GeoServiceRequest> setPathBasedProperties(GeoServiceRequest geoServiceRequest, String requestPath) {
        if (!StringUtils.startsWithIgnoreCase(requestPath, getGeoServicePathPrefix())) {
            return Optional.empty();
        }

        String trimmedRequestPath = requestPath.substring(getGeoServicePathPrefix().length());

        Matcher globalWmsWfsMatcher = RE_GLOBAL_WMS_WFS_PATTERN.matcher(trimmedRequestPath);
        if (globalWmsWfsMatcher.find()) {
            String endpointFragment = globalWmsWfsMatcher.group("endpoint");
            return setEndpointFromPathFragment(geoServiceRequest, endpointFragment);
        }


        Matcher qualifiedWmsWfsMatcher = RE_QUALIFIED_WMS_WFS_PATTERN.matcher(trimmedRequestPath);
        if (qualifiedWmsWfsMatcher.find()) {
            String endpointFragment = qualifiedWmsWfsMatcher.group("endpoint");
            String workspaceFragment = qualifiedWmsWfsMatcher.group("workspace");

            geoServiceRequest.setWorkspaceName(Optional.of(workspaceFragment));

            String layerFragment = qualifiedWmsWfsMatcher.group("layer");
            if (layerFragment != null) {
                geoServiceRequest.setLayers(
                        QualifiedLayerName.fromStringWithWorkspaceFallback(geoServiceRequest.getWorkspaceName(), layerFragment)
                                .map(Set::of)
                                .orElse(Collections.emptySet())
                );
            }
            return setEndpointFromPathFragment(geoServiceRequest, endpointFragment);
        }

        Matcher wmtsMatcher = RE_WMTS_PATTERN.matcher(trimmedRequestPath);
        if (wmtsMatcher.find()) {
            geoServiceRequest.setEndpoint(GeoServiceEndpoint.WMTS);
            geoServiceRequest.setServiceType(ServiceType.WMTS);
            geoServiceRequest.setWorkspaceName(Optional.empty());
            return Optional.of(geoServiceRequest);
        }
        return Optional.empty();
    }

    private Optional<GeoServiceRequest> setEndpointFromPathFragment(GeoServiceRequest geoServiceRequest, String endpointFragment) {
        if (StringUtils.equalsIgnoreCase(endpointFragment, "wms")) {
            geoServiceRequest.setEndpoint(GeoServiceEndpoint.WMS);
        } else if (StringUtils.equalsIgnoreCase(endpointFragment, "wfs")) {
            geoServiceRequest.setEndpoint(GeoServiceEndpoint.WFS);
        } else if (StringUtils.equalsIgnoreCase(endpointFragment, "ows")) {
            geoServiceRequest.setEndpoint(GeoServiceEndpoint.OWS);
        } else {
            // should not be reachable by regex
            return Optional.empty();
        }
        return Optional.of(geoServiceRequest);
    }

    private Optional<GeoServiceRequest> setQueryParamBasedProperties(GeoServiceRequest geoServiceRequest, MultiValueMap<String, String> queryParams) {
        geoServiceRequest.setParamsNormalized(queryParams);

        String serviceString = geoServiceRequest.getParamsNormalized().get("service");
        if (StringUtils.equalsIgnoreCase(serviceString, "wms")) {
            geoServiceRequest.setServiceType(ServiceType.WMS);
        } else if (StringUtils.equalsIgnoreCase(serviceString, "wfs")) {
            geoServiceRequest.setServiceType(ServiceType.WFS);
        } else if (StringUtils.equalsIgnoreCase(serviceString, "wmts")) {
            geoServiceRequest.setServiceType(ServiceType.WMTS);
        }

        geoServiceRequest.setRequestType(
                Optional.ofNullable(geoServiceRequest.getParamsNormalized().get("request"))
                        .map(String::toLowerCase)
        );

        if (geoServiceRequest.getServiceType() == ServiceType.WMS) {
            addGeoServiceRequestLayersFromQueryParams(geoServiceRequest, queryParams, "layers");

            // query_layers is an additional parameter used by WMS GetFeatureInfo requests
            addGeoServiceRequestLayersFromQueryParams(geoServiceRequest, queryParams, "query_layers");
        } else if (geoServiceRequest.getServiceType() == ServiceType.WFS) {
            addGeoServiceRequestLayersFromQueryParams(geoServiceRequest, queryParams, "typenames");
            addGeoServiceRequestLayersFromQueryParams(geoServiceRequest, queryParams, "typename"); // DescribeFeatureType
        } else if (geoServiceRequest.getServiceType() == ServiceType.WMTS) {
            addGeoServiceRequestLayersFromQueryParams(geoServiceRequest, queryParams, "layer");
        }

        return Optional.of(geoServiceRequest);
    }

    /**
     * fill the `request` property of the layer taking all duplicated occurrences of a query parameter into account
     */
    private void addGeoServiceRequestLayersFromQueryParams(GeoServiceRequest geoServiceRequest, MultiValueMap<String, String> queryParams, String paramName) {
        geoServiceRequest.addLayers(
                queryParams.keySet()
                        .stream()
                        .filter(keyName -> StringUtils.equalsIgnoreCase(keyName, paramName))
                        .flatMap(keyName ->
                                queryParams
                                        .get(keyName)
                                        .stream()
                                        .filter(Objects::nonNull)
                                        .map(paramValue -> URLDecoder.decode(paramValue, StandardCharsets.UTF_8))
                                        .flatMap(paramValue -> splitCommaSeparated(paramValue).stream())
                                        .map(layerName -> QualifiedLayerName.fromStringWithWorkspaceFallback(
                                                geoServiceRequest.getWorkspaceName(),
                                                layerName
                                        ))
                                        .filter(Optional::isPresent)
                                        .map(Optional::get)

                        )
                        .collect(Collectors.toSet())
        );
    }

    private List<String> splitCommaSeparated(String input) {
        if (StringUtils.isBlank(input)) {
            return Collections.emptyList();
        }
        return Arrays.stream(input.split("\\s*,\\s*")).toList();
    }

    public boolean isBlockedRequestType(final String requestType) {
        if (requestType == null)
            return false;
        String newRequestType = requestType.toLowerCase(Locale.ROOT).strip();
        if (newRequestType.isEmpty())
            return false;
        return geoServiceProperties.getBlockedRequestTypes().contains(newRequestType);
    }
}
