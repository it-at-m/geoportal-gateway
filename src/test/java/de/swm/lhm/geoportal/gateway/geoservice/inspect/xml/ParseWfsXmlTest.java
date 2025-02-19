package de.swm.lhm.geoportal.gateway.geoservice.inspect.xml;


import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceRequestType;
import de.swm.lhm.geoportal.gateway.shared.model.ServiceType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ParseWfsXmlTest extends AbstractParseXmlTest {

    @Test
    void parseWfsGetCapabilities() {
        Optional<GeoServiceXmlRequestParameters> params = parse(TestBodies.GET_CAPABILITIES.body, geoServiceXmlRequestDocumentParser.getWfsGetCapabilitiesParser());
        assertThat(params.isPresent(), is(true));
        assertThat(params.get().getGeoServiceType(), is(ServiceType.WFS));
        assertThat(params.get().getGeoServiceRequestType(), is(GeoServiceRequestType.GET_CAPABILITIES));
        assertThat(params.get().getRequestedLayers(), is(Collections.emptySet()));
    }

    @Test
    void parseWfs20GetPropertyValue() {
        Optional<GeoServiceXmlRequestParameters> params = parse(TestBodies.GET_PROPERTY_VALUE.body, geoServiceXmlRequestDocumentParser.getWfs20GetPropertyValueParser());
        assertThat(params.isPresent(), is(true));
        assertThat(params.get().getGeoServiceType(), is(ServiceType.WFS));
        assertThat(params.get().getGeoServiceRequestType(), is(GeoServiceRequestType.GET_PROPERTY_VALUE));
        assertThat(params.get().getRequestedLayers(), is(Set.of("topp:states", "myws:myl")));
    }

    @Test
    void parseWfs20GetFeatureWithLock() {
        Optional<GeoServiceXmlRequestParameters> params = parse(TestBodies.GET_FEATURE_WITH_LOCK.body, geoServiceXmlRequestDocumentParser.getWfs20GetFeatureWithLockParser());
        assertThat(params.isPresent(), is(true));
        assertThat(params.get().getGeoServiceType(), is(ServiceType.WFS));
        assertThat(params.get().getGeoServiceRequestType(), is(GeoServiceRequestType.GET_FEATURE_WITH_LOCK));
        assertThat(params.get().getRequestedLayers(), is(Set.of("topp:states")));
    }

    @Test
    void parseWfs20CreateStoredQuery() {
        Optional<GeoServiceXmlRequestParameters> params = parse(TestBodies.CREATE_STORED_QUERY.body, geoServiceXmlRequestDocumentParser.getWfs20CreateStoredQueryParser());
        assertThat(params.isPresent(), is(true));
        assertThat(params.get().getGeoServiceType(), is(ServiceType.WFS));
        assertThat(params.get().getGeoServiceRequestType(), is(GeoServiceRequestType.CREATE_STORED_QUERY));
        assertThat(params.get().getRequestedLayers(), is(Set.of("topp:states")));
    }

    @Test
    void parseWfs20DropStoredQuery() {
        Optional<GeoServiceXmlRequestParameters> params = parse(TestBodies.DROP_STORED_QUERY.body, geoServiceXmlRequestDocumentParser.getWfs20DropStoredQueryParser());
        assertThat(params.isPresent(), is(true));
        assertThat(params.get().getGeoServiceType(), is(ServiceType.WFS));
        assertThat(params.get().getGeoServiceRequestType(), is(GeoServiceRequestType.DROP_STORED_QUERY));
        assertThat(params.get().getRequestedLayers(), is(Collections.emptySet()));
    }

    @Test
    void parseWfs20ListStoredQueries() {
        Optional<GeoServiceXmlRequestParameters> params = parse(TestBodies.LIST_STORED_QUERIES.body, geoServiceXmlRequestDocumentParser.getWfs20ListStoredQueriesParser());
        assertThat(params.isPresent(), is(true));
        assertThat(params.get().getGeoServiceType(), is(ServiceType.WFS));
        assertThat(params.get().getGeoServiceRequestType(), is(GeoServiceRequestType.LIST_STORED_QUERIES));
        assertThat(params.get().getRequestedLayers(), is(Collections.emptySet()));
    }

    @Test
    void parseWfs20DescribeStoredQueries() {
        Optional<GeoServiceXmlRequestParameters> params = parse(TestBodies.DESCRIBE_STORED_QUERIES.body, geoServiceXmlRequestDocumentParser.getWfs20DescribeStoredQueriesParser());
        assertThat(params.isPresent(), is(true));
        assertThat(params.get().getGeoServiceType(), is(ServiceType.WFS));
        assertThat(params.get().getGeoServiceRequestType(), is(GeoServiceRequestType.DESCRIBE_STORED_QUERIES));
        assertThat(params.get().getRequestedLayers(), is(Collections.emptySet()));
    }

    void assertWfs20Transaction(String body) {
        Optional<GeoServiceXmlRequestParameters> params = parse(body, geoServiceXmlRequestDocumentParser.getWfs20TransactionParser());
        assertThat(params.isPresent(), is(true));
        assertThat(params.get().getGeoServiceType(), is(ServiceType.WFS));
        assertThat(params.get().getGeoServiceRequestType(), is(GeoServiceRequestType.TRANSACTION));
        assertThat(params.get().getRequestedLayers(), is(Set.of("topp:tasmania_roads")));
    }

    @Test
    void parseTransactionDelete() {
        assertWfs20Transaction(TestBodies.TRANSACTION_DELETE.body);
    }

    @Test
    void parseTransactionInsert() {
        assertWfs20Transaction(TestBodies.TRANSACTION_INSERT.body);
    }

    @Test
    void parseTransactionUpdate() {
        assertWfs20Transaction(TestBodies.TRANSACTION_UPDATE.body);
    }

    @Test
    void parseTransactionReplace() {
        assertWfs20Transaction(TestBodies.TRANSACTION_REPLACE.body);
    }

    @Test
    void parseGetFeature() {
        Optional<GeoServiceXmlRequestParameters> params = parse(TestBodies.GET_FEATURE.body, geoServiceXmlRequestDocumentParser.getWfsGetFeatureParser());
        assertThat(params.isPresent(), is(true));
        assertThat(params.get().getGeoServiceType(), is(ServiceType.WFS));
        assertThat(params.get().getGeoServiceRequestType(), is(GeoServiceRequestType.GET_FEATURE));
        assertThat(params.get().getRequestedLayers(), is(Set.of("topp:states")));
    }

    @Test
    void parseDescribeFeatureType() {
        Optional<GeoServiceXmlRequestParameters> params = parse(TestBodies.DESCRIBE_FEATURE_TYPE.body, geoServiceXmlRequestDocumentParser.getWfsDescribeFeatureTypeParser());
        assertThat(params.isPresent(), is(true));
        assertThat(params.get().getGeoServiceType(), is(ServiceType.WFS));
        assertThat(params.get().getGeoServiceRequestType(), is(GeoServiceRequestType.DESCRIBE_FEATURE_TYPE));
        assertThat(params.get().getRequestedLayers(), is(Set.of("topp:states")));
    }

    @Test
    void allWfsBodiesParsable() {
        assertBodiesParsable(
                Arrays.stream(TestBodies.values()).map(v -> v.body).toList(),
                ServiceType.WFS
        );
    }

    enum TestBodies {
        GET_CAPABILITIES("""
                <GetCapabilities
                 service="WFS"
                 xmlns="http://www.opengis.net/wfs"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://www.opengis.net/wfs
                 http://schemas.opengis.net/wfs/1.1.0/wfs.xsd"/>
                """),
        GET_PROPERTY_VALUE("""
                <wfs:GetPropertyValue service='WFS' version='2.0.0'
                 xmlns:topp='http://www.openplans.org/topp'
                 xmlns:fes='http://www.opengis.net/fes/2.0'
                 xmlns:wfs='http://www.opengis.net/wfs/2.0'
                 valueReference='the_geom'>
                  <wfs:Query typeNames='topp:states,myws:myl'/>
                </wfs:GetPropertyValue>
                """),

        GET_FEATURE_WITH_LOCK("""
                <wfs:GetFeatureWithLock service='WFS' version='2.0.0'
                 handle='GetFeatureWithLock-tc1' expiry='5' resultType='results'
                 xmlns:topp='http://www.openplans.org/topp'
                 xmlns:fes='http://www.opengis.net/fes/2.0'
                 xmlns:wfs='http://www.opengis.net/wfs/2.0'
                 valueReference='the_geom'>
                  <wfs:Query typeNames='topp:states'/>
                </wfs:GetFeatureWithLock>
                """),

        CREATE_STORED_QUERY("""
                <wfs:CreateStoredQuery service='WFS' version='2.0.0'
                 xmlns:wfs='http://www.opengis.net/wfs/2.0'
                 xmlns:fes='http://www.opengis.org/fes/2.0'
                 xmlns:gml='http://www.opengis.net/gml/3.2'
                 xmlns:myns='http://www.someserver.com/myns'
                 xmlns:topp='http://www.openplans.org/topp'>
                  <wfs:StoredQueryDefinition id='myStoredQuery'>
                    <wfs:Parameter name='AreaOfInterest' type='gml:Polygon'/>
                    <wfs:QueryExpressionText
                     returnFeatureTypes='topp:states'
                     language='urn:ogc:def:queryLanguage:OGC-WFS::WFS_QueryExpression'
                     isPrivate='false'>
                      <wfs:Query typeNames='topp:states'>
                        <fes:Filter>
                          <fes:Within>
                            <fes:ValueReference>the_geom</fes:ValueReference>
                             ${AreaOfInterest}
                          </fes:Within>
                        </fes:Filter>
                      </wfs:Query>
                    </wfs:QueryExpressionText>
                  </wfs:StoredQueryDefinition>
                </wfs:CreateStoredQuery>
                """),
        DROP_STORED_QUERY("""
                <wfs:DropStoredQuery xmlns:wfs='http://www.opengis.net/wfs/2.0' service='WFS' id='myStoredQuery'/>
                """),

        LIST_STORED_QUERIES("""
                <wfs:ListStoredQueries service='WFS' version='2.0.0' xmlns:wfs='http://www.opengis.net/wfs/2.0'/>
                """),

        DESCRIBE_STORED_QUERIES("""
                            <wfs:DescribeStoredQueries xmlns:wfs='http://www.opengis.net/wfs/2.0' service='WFS'>
                              <wfs:StoredQueryId>urn:ogc:def:query:OGC-WFS::GetFeatureById</wfs:StoredQueryId>
                            </wfs:DescribeStoredQueries>
                """),

        // https://github.com/geoserver/geoserver/blob/174d2617d98ad00f3f4c4a2ce25ca54d9987723d/data/release/demo/WFS_describeFeatureType-1.1.xml
        DESCRIBE_FEATURE_TYPE("""
                <DescribeFeatureType
                  version="1.1.0"
                  service="WFS"
                  xmlns="http://www.opengis.net/wfs"
                  xmlns:topp="http://www.openplans.org/topp"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd">
                    <TypeName>topp:states</TypeName>
                </DescribeFeatureType>
                """),

        // https://github.com/geoserver/geoserver/blob/174d2617d98ad00f3f4c4a2ce25ca54d9987723d/data/release/demo/WFS_getFeatureIntersects-1.0.xml
        GET_FEATURE("""
                <wfs:GetFeature service="WFS" version="1.0.0"
                  outputFormat="GML2"
                  xmlns:topp="http://www.openplans.org/topp"
                  xmlns:wfs="http://www.opengis.net/wfs"
                  xmlns="http://www.opengis.net/ogc"
                  xmlns:gml="http://www.opengis.net/gml"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://www.opengis.net/wfs
                                      http://schemas.opengis.net/wfs/1.0.0/WFS-basic.xsd">
                  <wfs:Query typeName="topp:states">
                    <Filter>
                      <Intersects>
                        <PropertyName>the_geom</PropertyName>
                          <gml:Point srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
                            <gml:coordinates>-74.817265,40.5296504</gml:coordinates>
                          </gml:Point>
                        </Intersects>
                      </Filter>
                  </wfs:Query>
                </wfs:GetFeature>
                """),

        TRANSACTION_DELETE("""
                                <wfs:Transaction
                                   version="2.0.0"
                                   service="WFS"
                                   xmlns:fes="http://www.opengis.net/fes/2.0"
                                   xmlns:wfs="http://www.opengis.net/wfs/2.0"
                                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                   xsi:schemaLocation="http://www.opengis.net/wfs/2.0
                                                       http://schemas.opengis.net/wfs/2.0/wfs.xsd">
                                   <wfs:Delete typeName="topp:tasmania_roads">
                                      <fes:Filter>
                                         <fes:ResourceId rid="tasmania_roads.14"/>
                                      </fes:Filter>
                                   </wfs:Delete>
                                </wfs:Transaction>
                """),

        TRANSACTION_INSERT("""
                        <wfs:Transaction
                           version="2.0.0"
                           service="WFS"
                           xmlns:topp="http://www.openplans.org/topp"
                           xmlns:fes="http://www.opengis.net/fes/2.0"
                           xmlns:gml="http://www.opengis.net/gml/3.2"
                           xmlns:wfs="http://www.opengis.net/wfs/2.0"
                           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                           xsi:schemaLocation="http://www.opengis.net/wfs/2.0
                                               http://schemas.opengis.net/wfs/2.0/wfs.xsd
                                               http://www.opengis.net/gml/3.2
                                               http://schemas.opengis.net/gml/3.2.1/gml.xsd">
                           <wfs:Insert>
                            <topp:tasmania_roads gml:id="tasmania_roads.13">
                              <topp:the_geom>
                                <gml:MultiCurve srsName="http://www.opengis.net/def/crs/epsg/0/4326">
                                  <gml:curveMember>
                                    <gml:LineString>
                                      <gml:posList>-146.46 -41.24 146.57 -41.25 146.64 -41.25 146.76 -41.33</gml:posList>
                                    </gml:LineString>
                                  </gml:curveMember>
                                </gml:MultiCurve>
                              </topp:the_geom>
                              <topp:TYPE>RnbwRd</topp:TYPE>
                            </topp:tasmania_roads>
                            <!-- you can insert multiple features if you wish-->
                           </wfs:Insert>
                           <fes:Filter>
                             <fes:ResourceId rid="tasmania_roads.13"/>
                          </fes:Filter>
                        </wfs:Transaction>
                """),

        TRANSACTION_UPDATE("""
                        <wfs:Transaction
                           version="2.0.0"
                           service="WFS"
                           xmlns:fes="http://www.opengis.net/fes/2.0"
                           xmlns:wfs="http://www.opengis.net/wfs/2.0"
                           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                           xsi:schemaLocation="http://www.opengis.net/wfs/2.0
                                               http://schemas.opengis.net/wfs/2.0.0/wfs.xsd">
                           <wfs:Update typeName="topp:tasmania_roads">
                              <wfs:Property>
                                 <wfs:ValueReference>TYPE</wfs:ValueReference>
                                 <wfs:Value>YllwBrk</wfs:Value>
                              </wfs:Property>
                              <fes:Filter>
                                 <fes:ResourceId rid="tasmania_roads.14"/>
                              </fes:Filter>
                           </wfs:Update>
                        </wfs:Transaction>
                """),

        TRANSACTION_REPLACE("""
                        <wfs:Transaction
                            version="2.0.0"
                            service="WFS"
                            xmlns:topp="http://www.openplans.org/topp"
                            xmlns:fes="http://www.opengis.net/fes/2.0"
                            xmlns:wfs="http://www.opengis.net/wfs/2.0"
                            xmlns:gml="http://www.opengis.net/gml/3.2"
                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xsi:schemaLocation="http://www.opengis.net/wfs/2.0
                        			http://schemas.opengis.net/wfs/2.0/wfs.xsd">
                          <wfs:Replace>
                            <topp:tasmania_roads gml:id="tasmania_roads.15">
                              <topp:the_geom>
                                <gml:MultiCurve srsName="http://www.opengis.net/def/crs/epsg/0/4326">
                                  <gml:curveMember>
                                    <gml:LineString>
                                      <gml:posList>-146.46 -41.24 146.57 -41.25 146.64 -41.25 146.76 -41.33</gml:posList>
                                    </gml:LineString>
                                  </gml:curveMember>
                                </gml:MultiCurve>
                              </topp:the_geom>
                              <topp:TYPE>FuryRd</topp:TYPE>
                            </topp:tasmania_roads>
                            <fes:Filter>
                              <fes:ResourceId rid="tasmania_roads.13"/>
                            </fes:Filter>
                          </wfs:Replace>
                        </wfs:Transaction>
                """);
        private final String body;

        TestBodies(String body) {
            this.body = body;
        }
    }
}
