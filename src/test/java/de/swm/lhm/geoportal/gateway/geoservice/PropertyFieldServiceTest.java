package de.swm.lhm.geoportal.gateway.geoservice;

import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import de.swm.lhm.geoportal.gateway.geoservice.model.PropertyField;
import de.swm.lhm.geoportal.gateway.geoservice.model.PropertyFieldEscaping;
import de.swm.lhm.geoportal.gateway.resource.ResourceProperties;
import de.swm.lhm.geoportal.gateway.resource.ResourceRepository;
import de.swm.lhm.geoportal.gateway.resource.ResourceService;
import de.swm.lhm.geoportal.gateway.shared.GatewayService;
import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PropertyFieldServiceTest {

    private PropertyFieldService buildPropertyFieldService(Stage stage, PropertyFieldRepository propertyFieldRepository, GeoServiceRepository geoServiceRepository) {
        GatewayService gatewayService = mock(GatewayService.class);
        when(gatewayService.getStage()).thenReturn(stage);
        when(gatewayService.getExternalUrl()).thenReturn("http://example.com");

        ResourceProperties resourceProperties = new ResourceProperties();
        resourceProperties.setEndpoint("/resource/download");
        resourceProperties.setDocumentsFolder("documents");

        ResourceService resourceService = new ResourceService(
                gatewayService,
                mock(ResourceRepository.class),
                resourceProperties
        );

        return new PropertyFieldService(gatewayService, propertyFieldRepository, resourceService, geoServiceRepository);
    }

    @Test
    void getGeoServicePropertyFieldsByNameAndWorkspace() {
        QualifiedLayerName qualifiedLayerName = new QualifiedLayerName("myworkspace", "mylayer");
        Stage stage = Stage.CONFIGURATION;

        PropertyField pf1 = new PropertyField("schema1", "table1", "pf1", "pf1label", true, PropertyFieldEscaping.NONE);
        PropertyField pf2 = new PropertyField("schema2", "table2", "pf2", "pf2label", false, PropertyFieldEscaping.URL);

        PropertyFieldRepository propertyFieldRepository = mock(PropertyFieldRepository.class);
        when(propertyFieldRepository.findGeoServicePropertyFieldsByWorkspaceAndNameAndStage(qualifiedLayerName.workspaceName(), qualifiedLayerName.layerName(), stage))
                .thenReturn(Flux.fromIterable(List.of(pf1, pf2)));

        GeoServiceRepository geoServiceRepository = mock(GeoServiceRepository.class);
        when(geoServiceRepository.findContainedQualifiedLayerNames(stage, qualifiedLayerName))
                .thenReturn(Flux.fromIterable(List.of(qualifiedLayerName)));

        PropertyFieldService service = buildPropertyFieldService(stage, propertyFieldRepository, geoServiceRepository);

        Map<String, PropertyField> propertyFields = service.getGeoServicePropertyFieldsByNameAndWorkspace(qualifiedLayerName)
                .block();
        assertThat(propertyFields.size(), is(2));
        assertThat(propertyFields.get("pf1"), is(pf1));
        assertThat(propertyFields.get("pf2"), is(pf2));
    }

    void testEscapePropertyField(PropertyFieldEscaping escaping) {
        PropertyFieldService service = buildPropertyFieldService(Stage.CONFIGURATION, mock(PropertyFieldRepository.class), mock(GeoServiceRepository.class));
        Optional<String> escapedValue = service.escapeFieldValue("myvalue", new PropertyField(
                "myschema",
                "mytable",
                "myfield",
                "mylabel",
                true,
                escaping
        ));
        assertThat(escapedValue.get(), is("http://example.com/resource/download/myschema/documents/mytable/myfield/myvalue"));
    }

    @Test
    void escapeFilePropertyField() {
        testEscapePropertyField(PropertyFieldEscaping.FILE);
    }

    @Test
    void escapeImagePropertyField() {
        testEscapePropertyField(PropertyFieldEscaping.IMAGE);
    }
}
