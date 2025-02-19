package de.swm.lhm.geoportal.gateway.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Slf4j
class TokenUtilsTest {

    @Test
    void shouldMaskTokenWithNull() {
        String token = null;
        String maskedToken = TokenUtils.maskToken(token);
        assertThat(maskedToken, is(nullValue()));
    }

    @Test
    void shouldMaskShortToken() {
        String token = "abcde";
        String maskedToken = TokenUtils.maskToken(token);
        assertThat(maskedToken, is("*****"));
    }

    @Test
    void shouldMaskToken() {
        String token = "abcdefghij";
        String maskedToken = TokenUtils.maskToken(token);
        assertThat(maskedToken, is("abc****hij"));
    }

    @Test
    void shouldMaskUriWhenIdTokenIsPresent() throws URISyntaxException {
        URI uri = new URIBuilder("http://localhost/test")
                .addParameter("id_token", "abcdefghij")
                .build();
        URI maskedUri = TokenUtils.maskUri(uri, "id_token");

        assertThat(maskedUri.toString(), containsString("id_token=abc****hij"));
    }

    @Test
    void shouldNotChangeUriWhenIdTokenIsAbsent() throws URISyntaxException {
        URI uri = new URIBuilder("http://localhost/test")
                .addParameter("access_token", "abcdefghij")
                .build();
        URI maskedUri = TokenUtils.maskUri(uri, "id_token");

        assertThat(maskedUri.toString(), is(uri.toString()));
    }

    @Test
    void shouldMaskRealBase64Token() {
        // Example of a real Base64-encoded token payload with JSON data
        String jsonPayload = "{\"sub\":\"1234567890\",\"name\":\"John Doe\",\"admin\":true}";
        String encodedToken = Base64.getUrlEncoder().encodeToString(jsonPayload.getBytes());

        String maskedToken = TokenUtils.maskToken(encodedToken);
        log.info("Real Token: {}", encodedToken);
        log.info("Masked Token: {}", maskedToken);

        // Ensure that maskedToken is not equal to encodedToken
        assertThat(maskedToken, is(not(encodedToken)));

        // Validating that only the first and last 3 characters are visible
        assertThat(maskedToken.substring(0, 3), is(encodedToken.substring(0, 3)));
        assertThat(maskedToken.substring(maskedToken.length() - 3), is(encodedToken.substring(encodedToken.length() - 3)));
        assertThat(maskedToken.length(), is(encodedToken.length()));
    }

    @Test
    void shouldMaskUriWithRealBase64Token() throws URISyntaxException {
        // Example of a real Base64-encoded token payload with JSON data
        String jsonPayload = "{\"sub\":\"1234567890\",\"name\":\"John Doe\",\"admin\":true}";
        String encodedToken = Base64.getUrlEncoder().encodeToString(jsonPayload.getBytes());

        URI uri = new URIBuilder("http://localhost/test")
                .addParameter("id_token", encodedToken)
                .build();
        URI maskedUri = TokenUtils.maskUri(uri, "id_token");

        // Ensure that the masked URI token is not equal to the original token
        String maskedToken = TokenUtils.maskToken(encodedToken);
        assertThat(maskedToken, is(not(encodedToken)));
        assertThat(maskedUri.toString(), containsString("id_token=" + maskedToken));
        assertThat(maskedUri.toString().length(), is(uri.toString().length()));
    }

    @Test
    void shouldMaskJwtToken() {
        // Example JWT token consisting of Header, Payload and Signature
        String header = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
        String payload = "{\"sub\":\"1234567890\",\"name\":\"John Doe\",\"admin\":true}";
        String signature = "abcdefghijklmnopqrstuvwxyz0123456789";

        String encodedHeader = Base64.getUrlEncoder().encodeToString(header.getBytes());
        String encodedPayload = Base64.getUrlEncoder().encodeToString(payload.getBytes());
        String encodedSignature = Base64.getUrlEncoder().encodeToString(signature.getBytes());

        String jwtToken = String.join(".", encodedHeader, encodedPayload, encodedSignature);
        String maskedToken = TokenUtils.maskToken(jwtToken);
        log.info("JWT Token: {}", jwtToken);
        log.info("Masked Token: {}", maskedToken);

        // Ensure that maskedToken is not equal to jwtToken
        assertThat(maskedToken, is(not(jwtToken)));

        // Validate that only the first and last 3 characters of each part are visible
        String[] parts = jwtToken.split("\\.");
        String[] maskedParts = maskedToken.split("\\.");

        assertThat(maskedParts.length, is(3));
        for (int i = 0; i < 3; i++) {
            assertThat(maskedParts[i].substring(0, 3), is(parts[i].substring(0, 3)));
            assertThat(maskedParts[i].substring(maskedParts[i].length() - 3), is(parts[i].substring(parts[i].length() - 3)));
            assertThat(maskedParts[i].length(), is(parts[i].length()));
        }
    }

    @Test
    void shouldMaskUriWithJwtToken() throws URISyntaxException {
        // Example JWT token consisting of Header, Payload and Signature
        String header = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
        String payload = "{\"sub\":\"1234567890\",\"name\":\"John Doe\",\"admin\":true}";
        String signature = "abcdefghijklmnopqrstuvwxyz0123456789";

        String encodedHeader = Base64.getUrlEncoder().encodeToString(header.getBytes());
        String encodedPayload = Base64.getUrlEncoder().encodeToString(payload.getBytes());
        String encodedSignature = Base64.getUrlEncoder().encodeToString(signature.getBytes());

        String jwtToken = String.join(".", encodedHeader, encodedPayload, encodedSignature);

        URI uri = new URIBuilder("http://localhost/test")
                .addParameter("id_token", jwtToken)
                .build();
        URI maskedUri = TokenUtils.maskUri(uri, "id_token");

        log.info("uri: {}", uri);
        log.info("masked uri: {}", maskedUri);

        // Ensure that the masked URI token is not equal to the original token
        String maskedToken = TokenUtils.maskToken(jwtToken);
        assertThat(maskedToken, is(not(jwtToken)));
        assertThat(maskedUri.toString(), containsString("id_token=" + maskedToken));
        assertThat(maskedUri.toString().length(), is(uri.toString().length()));
    }
}