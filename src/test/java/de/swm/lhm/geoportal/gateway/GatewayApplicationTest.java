package de.swm.lhm.geoportal.gateway;

import de.swm.lhm.geoportal.gateway.base_classes.BaseIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(OutputCaptureExtension.class)
class GatewayApplicationTest extends BaseIntegrationTest {

    @AfterAll
    @DirtiesContext
    public static void shouldLogWhenApplicationIsShuttingDown(CapturedOutput capturedOutput) {
        assertThat(capturedOutput.getOut()).contains("Gateway is shutting down");
    }

    @Test
    void shouldLogWhenApplicationHasBeenStarted(CapturedOutput capturedOutput) {
        assertThat(capturedOutput.getOut()).contains("Started GatewayApplicationTest");
    }

}
