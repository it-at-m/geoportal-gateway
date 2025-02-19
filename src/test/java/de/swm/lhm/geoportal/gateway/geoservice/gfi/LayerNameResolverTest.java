package de.swm.lhm.geoportal.gateway.geoservice.gfi;

import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class LayerNameResolverTest {

    private LayerNameResolver buildLayerNameResolver() {
        LayerNameResolver layerNameResolver = new LayerNameResolver();
        layerNameResolver.add(
                new QualifiedLayerName("myws", "myparentlayer"),
                Stream.of("myws:mylayer", "myotherws:myotherlayer" )
                        .map(QualifiedLayerName::fromString)
                        .toList());
        return layerNameResolver;
    }

    @Test
    void resolveNumericFeatureIdSuccess() {
        LayerNameResolver layerNameResolver = buildLayerNameResolver();
        assertThat(layerNameResolver.resolveLayerNameFromFeatureId("mylayer.452"),
                is(Optional.of(QualifiedLayerName.fromString("myws:mylayer"))));
    }

    @Test
    void resolveAlphaNumericFeatureIdSuccess() {
        LayerNameResolver layerNameResolver = buildLayerNameResolver();
        assertThat(layerNameResolver.resolveLayerNameFromFeatureId("mylayer.h4zz52"),
                is(Optional.of(QualifiedLayerName.fromString("myws:mylayer"))));
    }

    @Test
    void resolveMissingFail() {
        LayerNameResolver layerNameResolver = buildLayerNameResolver();
        assertThat(layerNameResolver.resolveLayerNameFromFeatureId("mymissinglayer.234"), is(Optional.empty()));
    }

    @Test
    void containsQualifiedLayerNameSuccess() {
        LayerNameResolver layerNameResolver = buildLayerNameResolver();
        assertThat(layerNameResolver.contains(QualifiedLayerName.fromString("myws:mylayer")), is(true));
    }

    @Test
    void containsQualifiedLayerNameFail() {
        LayerNameResolver layerNameResolver = buildLayerNameResolver();
        assertThat(layerNameResolver.contains(QualifiedLayerName.fromString("myws:mynonexistinglayer")), is(false));
    }
}
