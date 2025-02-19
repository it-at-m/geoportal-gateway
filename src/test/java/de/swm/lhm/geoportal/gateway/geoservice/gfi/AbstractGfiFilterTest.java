package de.swm.lhm.geoportal.gateway.geoservice.gfi;

import de.swm.lhm.geoportal.gateway.base_classes.FileLoader;
import de.swm.lhm.geoportal.gateway.geoservice.PropertyFieldService;

import static org.mockito.Mockito.mock;

abstract class AbstractGfiFilterTest extends FileLoader {
    protected PropertyFieldService mockPropertyFieldService() {
        return mock(PropertyFieldService.class);
    }
}
