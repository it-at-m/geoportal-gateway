package de.swm.lhm.geoportal.gateway.unit;

import de.swm.lhm.geoportal.gateway.base_classes.SqlRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;


@Import({
        UnitService.class,
        UnitRepository.class
})
@ExtendWith({OutputCaptureExtension.class})
class UnitServiceTest extends SqlRunner {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    UnitService unitService;

    @BeforeEach
    void setUp() throws IOException {
        runSql(loadFileContent("setup.sql").split(";"));
    }

    @AfterEach
    void tearDown() throws IOException {
        runSql(loadFileContent("teardown.sql").split(";"));
    }

    @Test
    void getUnitNameById() {
        String unitName = unitService.getUnitNameById(1).block();
        assertThat(unitName, is("firstUnit"));
    }

    @Test
    void getUnitNameByIdNonExisting() {
        String unitName = unitService.getUnitNameById(2).block();
        assertThat(unitName, is(nullValue()));
    }
}