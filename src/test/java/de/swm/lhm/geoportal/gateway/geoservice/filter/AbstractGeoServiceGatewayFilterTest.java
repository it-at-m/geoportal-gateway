package de.swm.lhm.geoportal.gateway.geoservice.filter;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import de.swm.lhm.geoportal.gateway.geoservice.AbstractGeoServiceTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

abstract class AbstractGeoServiceGatewayFilterTest extends AbstractGeoServiceTest {

    protected final String WFS_ENDPOINT = "/geoserver/ows?Service=WFS&Request=GetCapabilities";
    protected final String WFS_DOCUMENT = "geoservice/get-capabilities/wfs-capabilities.xml";
    protected final String WMS_DOCUMENT = "geoservice/get-capabilities/wms-capabilities.xml";
    protected final String WMS_ENDPOINT = "/geoserver/ows?Service=WMS&Request=GetCapabilities";
    protected final String WMTS_DOCUMENT = "geoservice/get-capabilities/wmts-capabilities.xml";
    protected final String WMTS_ENDPOINT = "/geoserver/gwc/service/wmts?Service=WMTS&Request=GetCapabilities";

    StubMapping serveContent(String content, String contentType) {
        return stubFor(get(urlPathMatching("/geoserver/.*"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, contentType)
                        .withHeader(HttpHeaders.SERVER, GEOSERVER_SERVER_HEADER)
                        .withBody(content)));
    }

    StubMapping serveXmlDocument(String capabilitiesDocumentResource, boolean skipLayerCheck) throws IOException {
        String capabilities = loadFileContent(capabilitiesDocumentResource);
        if (!skipLayerCheck) {
            assertThat(capabilities, containsString(PROTECTED_LAYER));
            assertThat(capabilities, containsString(RESTRICTED_LAYER));
        }

        return serveContent(capabilities, MediaType.APPLICATION_XML_VALUE);
    }

    StubMapping serveXmlDocument(String capabilitiesDocumentResource) throws IOException {
        return serveXmlDocument(capabilitiesDocumentResource, false);
    }

}
