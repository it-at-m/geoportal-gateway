package de.swm.lhm.geoportal.gateway.portal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.stringContainsInOrder;


@ExtendWith({OutputCaptureExtension.class})
@TestPropertySource(
        properties = {
                "geoportal.gateway.portal.path=" + PortalControllerTest.PORTAL_ROOT_PATH
        }
)
class PortalControllerTest extends BasePortalControllerTest {

    public static final String PORTAL_ROOT_PATH = "/dummy";

    @Test
    void requestNonExistingFiles(CapturedOutput output) {

        webTestClient.get()
                .uri("/portal/" + PUBLIC_PORTAL + "/")
                .exchange()
                .expectStatus().isNotFound();

        assertThat(output.getAll(), stringContainsInOrder("Could not find file ", PORTAL_ROOT_PATH, PUBLIC_PORTAL, "index.html"));

        webTestClient.get()
                .uri("/portal/" + PUBLIC_PORTAL + "/config.json")
                .exchange()
                .expectStatus().isNotFound();

        assertThat(output.getAll(), stringContainsInOrder("Could not find file ", PORTAL_ROOT_PATH, PUBLIC_PORTAL, "/config.json"));

    }

}