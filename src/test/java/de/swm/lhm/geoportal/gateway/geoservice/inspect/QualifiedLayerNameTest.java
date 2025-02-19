package de.swm.lhm.geoportal.gateway.geoservice.inspect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class QualifiedLayerNameTest {

    @Test
    void fromStringShouldNotBeBlank() {
        assertThrows(QualifiedLayerName.IllegalLayerNameSyntaxException.class, () -> QualifiedLayerName.fromString(""));
    }

    @Test
    void fromStringShouldNotContainMultipleDots() {
        assertThrows(QualifiedLayerName.IllegalLayerNameSyntaxException.class, () -> QualifiedLayerName.fromString("a.b.c"));
    }

    @Test
    void fromStringShouldOneDot() {
        assertThrows(QualifiedLayerName.IllegalLayerNameSyntaxException.class, () -> QualifiedLayerName.fromString("ab"));
    }
}
