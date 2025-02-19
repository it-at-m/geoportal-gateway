package de.swm.lhm.geoportal.gateway.m2m.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EaiResponseTest {

    @Test
    void testErrorResponseDeserialization() throws IOException {
        String json = """
                {
                  "errorCode": "F900",
                  "errorText": "RH-SSO meldet unautorisierten Zugriff. Ggf. wurde der RH-SSO ohne Token aufgerufen.",
                  "errorDetails": "HTTP operation failed invoking http://svlhmgdii03.muenchen.swm.de:8090/auth/admin/realms/public/users/d575d5ae-4d26-4c1f-ac7a-77a24a3d14f9 with statusCode: 401; {\\"error\\":\\"HTTP 401 Unauthorized\\"}",
                  "executionSteps": {
                    "de.muenchen.rhsso.routes.setUserPassword": false,
                    "de.muenchen.rhsso.routes.validateUserUUID": false,
                    "de.muenchen.rhsso.routes.userExists": false
                  },
                  "responseType": "failure",
                  "resultCode": "1021",
                  "resultText": "Fehler beim Überprüfen, ob der Benutzer existiert. (Schritt: de.muenchen.rhsso.routes.userExists)"
                }""";

        ObjectMapper objectMapper = new ObjectMapper();
        EaiResponse response = objectMapper.readValue(json, EaiResponse.class);

        assertNotNull(response);
        assertEquals("F900", response.getErrorCode());
        assertEquals("RH-SSO meldet unautorisierten Zugriff. Ggf. wurde der RH-SSO ohne Token aufgerufen.", response.getErrorText());
        assertEquals("HTTP operation failed invoking http://svlhmgdii03.muenchen.swm.de:8090/auth/admin/realms/public/users/d575d5ae-4d26-4c1f-ac7a-77a24a3d14f9 with statusCode: 401; {\"error\":\"HTTP 401 Unauthorized\"}", response.getErrorDetails());
        assertEquals(Map.of(
                "de.muenchen.rhsso.routes.setUserPassword", false,
                "de.muenchen.rhsso.routes.validateUserUUID", false,
                "de.muenchen.rhsso.routes.userExists", false
        ), response.getExecutionSteps());
        assertEquals("failure", response.getResponseType());
        assertEquals("1021", response.getResultCode());
        assertEquals("Fehler beim Überprüfen, ob der Benutzer existiert. (Schritt: de.muenchen.rhsso.routes.userExists)", response.getResultText());
    }

    @Test
    void testSuccessResponseDeserialization() throws IOException {
        String json = """
                {
                    "responseType": "user-password",
                    "resultCode": "1000",
                    "resultText": "Ausführung war erfolgreich.",
                    "password": "c8119c1e-f911-448e-a45e-5405a9f45788"
                }""";

        ObjectMapper objectMapper = new ObjectMapper();
        EaiResponse response = objectMapper.readValue(json, EaiResponse.class);

        assertNotNull(response);
        assertEquals("user-password", response.getResponseType());
        assertEquals("1000", response.getResultCode());
        assertEquals("Ausführung war erfolgreich.", response.getResultText());
        assertEquals("c8119c1e-f911-448e-a45e-5405a9f45788", response.getPassword());

    }

    @Test
    void testSuccessResponseWithOnlyPAsswordDeserialization() throws IOException {
        String json = """
                {
                    "password": "c8119c1e-f911-448e-a45e-5405a9f45788"
                }""";

        ObjectMapper objectMapper = new ObjectMapper();
        EaiResponse response = objectMapper.readValue(json, EaiResponse.class);

        assertNotNull(response);
        assertEquals("c8119c1e-f911-448e-a45e-5405a9f45788", response.getPassword());

    }

}