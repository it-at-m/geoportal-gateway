package de.swm.lhm.geoportal.gateway.geoservice.inspect;

import de.swm.lhm.geoportal.gateway.shared.model.ServiceType;
import de.swm.lhm.geoportal.gateway.geoservice.GeoServiceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


@ExtendWith(SpringExtension.class)
class GeoServiceInspectorServiceTest {

    private GeoServiceInspectorService geoServiceInspectorService;

    @BeforeEach
    void setUp() {
        GeoServiceProperties geoServiceProperties = new GeoServiceProperties();
        geoServiceProperties.setEndpoint("/geoserver");
        geoServiceProperties.setUrl("lb://geoserver");
        this.geoServiceInspectorService = new GeoServiceInspectorService(geoServiceProperties);
    }


    @Test
    void noGeoServiceRequest() {
        assertThat(geoServiceInspectorService.inspectGetRequestWithQueryParams("/geoserver/whatever").isEmpty(), is(true));
        assertThat(geoServiceInspectorService.inspectGetRequestWithQueryParams("/whatever").isEmpty(), is(true));
    }

    @Test
    void wmsGeoServiceRequest() {
        Optional<GeoServiceRequest> request = geoServiceInspectorService.inspectGetRequestWithQueryParams(
                "/geoserver/wms?REQUEST=GetMap&SERVICE=WMS&LAYERS=myws:mylayer,myws:myotherlayer"
        );
        assertThat(request.isPresent(), is(true));
        assertThat(request.get().getEndpoint(), is(GeoServiceEndpoint.WMS));
        assertThat(request.get().getServiceType(), is(ServiceType.WMS));
        assertThat(request.get().getWorkspaceName(), is(Optional.empty()));
        assertThat(request.get().getRequestType(), is(Optional.of("getmap")));
        assertThat(request.get().getLayers(), is(Set.of(
                new QualifiedLayerName("myws", "mylayer"),
                new QualifiedLayerName("myws", "myotherlayer"))));
    }

    @Test
    void wmsGeoServiceRequestWithWorkspacePath() {
        Optional<GeoServiceRequest> request = geoServiceInspectorService.inspectGetRequestWithQueryParams(
                "/geoserver/myworkspace/wms?REQUEST=GetMap&SERVICE=WMS&LAYERS=myws:mylayer,myws:myotherlayer"
        );
        assertThat(request.isPresent(), is(true));
        assertThat(request.get().getEndpoint(), is(GeoServiceEndpoint.WMS));
        assertThat(request.get().getServiceType(), is(ServiceType.WMS));
        assertThat(request.get().getWorkspaceName(), is(Optional.of("myworkspace")));
        assertThat(request.get().getRequestType(), is(Optional.of("getmap")));
        assertThat(request.get().getLayers(), is(Set.of(
                new QualifiedLayerName("myws", "mylayer"),
                new QualifiedLayerName("myws", "myotherlayer"))));
    }

    @Test
    void wmsGeoServiceRequestWithLayerPath() {
        Optional<GeoServiceRequest> request = geoServiceInspectorService.inspectGetRequestWithQueryParams(
                "/geoserver/myws/mylayer/wms?REQUEST=GetMap&SERVICE=WMS"
        );
        assertThat(request.isPresent(), is(true));
        assertThat(request.get().getEndpoint(), is(GeoServiceEndpoint.WMS));
        assertThat(request.get().getServiceType(), is(ServiceType.WMS));
        assertThat(request.get().getWorkspaceName(), is(Optional.of("myws")));
        assertThat(request.get().getRequestType(), is(Optional.of("getmap")));
        assertThat(request.get().getLayers(), is(Set.of(
                new QualifiedLayerName("myws", "mylayer")
        )));
    }

    @Test
    void wmsGeoServiceRequestWithWorkspacePathAndOmmitedWorkspaceInLayerName() {
        Optional<GeoServiceRequest> request = geoServiceInspectorService.inspectGetRequestWithQueryParams(
                "/geoserver/myws/wms?REQUEST=GetMap&SERVICE=WMS&layers=mylayer,otherws:otherlayer"
        );
        assertThat(request.isPresent(), is(true));
        assertThat(request.get().getEndpoint(), is(GeoServiceEndpoint.WMS));
        assertThat(request.get().getServiceType(), is(ServiceType.WMS));
        assertThat(request.get().getWorkspaceName(), is(Optional.of("myws")));
        assertThat(request.get().getRequestType(), is(Optional.of("getmap")));
        assertThat(request.get().getLayers(), is(Set.of(
                new QualifiedLayerName("myws", "mylayer"),
                new QualifiedLayerName("otherws", "otherlayer"))));
    }


    @Test
    void OwsWmsGeoServiceRequest() {
        Optional<GeoServiceRequest> request = geoServiceInspectorService.inspectGetRequestWithQueryParams(
                "/geoserver/ows?REQUEST=GetMap&SERVICE=WMS&LAYERS=myws:mylayer,myws:myotherlayer"
        );
        assertThat(request.isPresent(), is(true));
        assertThat(request.get().getEndpoint(), is(GeoServiceEndpoint.OWS));
        assertThat(request.get().getServiceType(), is(ServiceType.WMS));
        assertThat(request.get().getWorkspaceName(), is(Optional.empty()));
        assertThat(request.get().getRequestType(), is(Optional.of("getmap")));
        assertThat(request.get().getLayers(), is(Set.of(
                new QualifiedLayerName("myws", "mylayer"),
                new QualifiedLayerName("myws", "myotherlayer"))));
    }

    @Test
    void OwsWmsGeoServiceRequestWithWorkspacePath() {
        Optional<GeoServiceRequest> request = geoServiceInspectorService.inspectGetRequestWithQueryParams(
                "/geoserver/myworkspace/ows?REQUEST=GetMap&SERVICE=WMS&LAYERS=myws:mylayer,myws:myotherlayer"
        );
        assertThat(request.isPresent(), is(true));
        assertThat(request.get().getEndpoint(), is(GeoServiceEndpoint.OWS));
        assertThat(request.get().getServiceType(), is(ServiceType.WMS));
        assertThat(request.get().getWorkspaceName(), is(Optional.of("myworkspace")));
        assertThat(request.get().getRequestType(), is(Optional.of("getmap")));
        assertThat(request.get().getLayers(), is(Set.of(
                new QualifiedLayerName("myws", "mylayer"),
                new QualifiedLayerName("myws", "myotherlayer")
        )));
    }

    @Test
    void OwsWmsGeoServiceRequestWithLayerPath() {
        Optional<GeoServiceRequest> request = geoServiceInspectorService.inspectGetRequestWithQueryParams(
                "/geoserver/myws/mylayer/ows?REQUEST=GetMap&SERVICE=WMS"
        );
        assertThat(request.isPresent(), is(true));
        assertThat(request.get().getEndpoint(), is(GeoServiceEndpoint.OWS));
        assertThat(request.get().getServiceType(), is(ServiceType.WMS));
        assertThat(request.get().getWorkspaceName(), is(Optional.of("myws")));
        assertThat(request.get().getRequestType(), is(Optional.of("getmap")));
        assertThat(request.get().getLayers(), is(Set.of(new QualifiedLayerName("myws", "mylayer"))));
    }

    @Test
    void wfsGeoServiceRequest() {
        Optional<GeoServiceRequest> request = geoServiceInspectorService.inspectGetRequestWithQueryParams(
                "/geoserver/wfs?SERVICE=WFS&REQUEST=DescribeFeature&typeNames=myws:mylayer"
        );
        assertThat(request.isPresent(), is(true));
        assertThat(request.get().getEndpoint(), is(GeoServiceEndpoint.WFS));
        assertThat(request.get().getServiceType(), is(ServiceType.WFS));
        assertThat(request.get().getWorkspaceName(), is(Optional.empty()));
        assertThat(request.get().getRequestType(), is(Optional.of("describefeature")));
        assertThat(request.get().getLayers(), is(Set.of(new QualifiedLayerName("myws", "mylayer"))));
    }

    @Test
    void wfsGeoServiceRequestWithWorkspacePath() {
        Optional<GeoServiceRequest> request = geoServiceInspectorService.inspectGetRequestWithQueryParams(
                "/geoserver/myworkspace/wfs?SERVICE=WFS&REQUEST=DescribeFeature&typeNames=myws:mylayer"
        );
        assertThat(request.isPresent(), is(true));
        assertThat(request.get().getEndpoint(), is(GeoServiceEndpoint.WFS));
        assertThat(request.get().getServiceType(), is(ServiceType.WFS));
        assertThat(request.get().getWorkspaceName(), is(Optional.of("myworkspace")));
        assertThat(request.get().getRequestType(), is(Optional.of("describefeature")));
        assertThat(request.get().getLayers(), is(Set.of(new QualifiedLayerName("myws","mylayer"))));
    }

    @Test
    void OwsWfsGeoServiceRequest() {
        Optional<GeoServiceRequest> request = geoServiceInspectorService.inspectGetRequestWithQueryParams(
                "/geoserver/ows?SERVICE=WFS&REQUEST=DescribeFeature&typeNames=myws:mylayer"
        );
        assertThat(request.isPresent(), is(true));
        assertThat(request.get().getEndpoint(), is(GeoServiceEndpoint.OWS));
        assertThat(request.get().getServiceType(), is(ServiceType.WFS));
        assertThat(request.get().getWorkspaceName(), is(Optional.empty()));
        assertThat(request.get().getRequestType(), is(Optional.of("describefeature")));
        assertThat(request.get().getLayers(), is(Set.of(new QualifiedLayerName("myws", "mylayer"))));
    }

    @Test
    void OwsWfsGeoServiceRequestWithWorkspacePath() {
        Optional<GeoServiceRequest> request = geoServiceInspectorService.inspectGetRequestWithQueryParams(
                "/geoserver/myworkspace/ows?SERVICE=WFS&REQUEST=DescribeFeature&typeNames=myws:mylayer"
        );
        assertThat(request.isPresent(), is(true));
        assertThat(request.get().getEndpoint(), is(GeoServiceEndpoint.OWS));
        assertThat(request.get().getServiceType(), is(ServiceType.WFS));
        assertThat(request.get().getWorkspaceName(), is(Optional.of("myworkspace")));
        assertThat(request.get().getRequestType(), is(Optional.of("describefeature")));
        assertThat(request.get().getLayers(), is(Set.of(new QualifiedLayerName("myws", "mylayer"))));
    }

    @Test
    void OwsWfsGeoServiceRequestWithLayerPath() {
        Optional<GeoServiceRequest> request = geoServiceInspectorService.inspectGetRequestWithQueryParams(
                "/geoserver/myws/mylayer/ows?SERVICE=WFS&REQUEST=DescribeFeature"
        );
        assertThat(request.isPresent(), is(true));
        assertThat(request.get().getEndpoint(), is(GeoServiceEndpoint.OWS));
        assertThat(request.get().getServiceType(), is(ServiceType.WFS));
        assertThat(request.get().getWorkspaceName(), is(Optional.of("myws")));
        assertThat(request.get().getRequestType(), is(Optional.of("describefeature")));
        assertThat(request.get().getLayers(), is(Set.of(new QualifiedLayerName("myws", "mylayer"))));
    }

    @Test
    void wmtsGeoServiceRequest() {
        Optional<GeoServiceRequest> request = geoServiceInspectorService.inspectGetRequestWithQueryParams(
                "/geoserver/gwc/service/wmts?REQUEST=GetTile&LAyER=myws:mylayer"
        );
        assertThat(request.isPresent(), is(true));
        assertThat(request.get().getServiceType(), is(ServiceType.WMTS));
        assertThat(request.get().getWorkspaceName(), is(Optional.empty()));
        assertThat(request.get().getRequestType(), is(Optional.of("gettile")));
        assertThat(request.get().getLayers(), is(Set.of(new QualifiedLayerName("myws", "mylayer"))));
    }

    @Test
    void wmsGfiQueryLayersIncluded() {
        Optional<GeoServiceRequest> request = geoServiceInspectorService.inspectGetRequestWithQueryParams(
                "/geoserver/play/wms?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetFeatureInfo&FORMAT=image%2Fpng&TRANSPARENT=true&QUERY_LAYERS=play:gis_osm_pois_free_1_o2o_query&CACHEID=1766091&LAYERS=play:gis_osm_pois_free_1_o2o&SINGLETILE=false&WIDTH=512&HEIGHT=512&INFO_FORMAT=application%2Fjson&FEATURE_COUNT=10&I=288&J=65&CRS=EPSG%3A25832&STYLES=&BBOX=690239.36%2C5334908.48%2C690382.72%2C5335051.840000001"
        );
        assertThat(request.isPresent(), is(true));
        assertThat(request.get().getLayers(), is(Set.of(
                new QualifiedLayerName("play", "gis_osm_pois_free_1_o2o_query"),
                new QualifiedLayerName("play", "gis_osm_pois_free_1_o2o"))));
    }
}
