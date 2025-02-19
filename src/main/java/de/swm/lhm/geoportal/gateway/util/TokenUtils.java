package de.swm.lhm.geoportal.gateway.util;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Utility class for masking sensitive information in tokens and URIs.
 */
public final class TokenUtils {

    // Private constructor to prevent instantiation
    private TokenUtils() {}

    /**
     * Masks a token for safe logging or display.
     * <p>
     * This method takes a JWT (or any other token represented as a string) and masks its content
     * for security purposes, ensuring that only the first and last three characters of each part
     * are visible.
     *
     * @param token The token to be masked. Can be {@code null}.
     * @return The masked token, or {@code null} if the input token is {@code null}.
     */
    public static String maskToken(String token) {
        if (token == null) {
            return null;
        }
        String[] parts = token.split("\\.");
        StringBuilder maskedTokenBuilder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].length() < 6) {
                maskedTokenBuilder.append("*".repeat(parts[i].length()));
            } else {
                maskedTokenBuilder.append(parts[i], 0, 3)
                                    .append("*".repeat(parts[i].length() - 6))
                                    .append(parts[i].substring(parts[i].length() - 3));
            }
            if (i < parts.length - 1) {
                maskedTokenBuilder.append('.');
            }
        }
        return maskedTokenBuilder.toString();
    }

    /**
     * Masks the ID token in a URI's query string for safe logging or display.
     * <p>
     * This method locates the ID token within the URI's query parameters, masks it using
     * the {@link #maskToken(String)} method, and reconstructs the URI with the masked token.
     *
     * @param uri The original URI containing the ID token.
     * @param idTokenKey The key of the query parameter that contains the ID token.
     * @return A new {@link URI} with the masked ID token.
     * @throws URISyntaxException If the URI syntax is incorrect.
     */
    public static URI maskUri(URI uri, String idTokenKey) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(uri);
        Optional<NameValuePair> idTokenParam = uriBuilder.getQueryParams().stream()
                .filter(param -> param.getName().equals(idTokenKey))
                .findFirst();

        if (idTokenParam.isPresent()) {
            String idToken = idTokenParam.get().getValue();
            String maskedToken = maskToken(idToken);
            uriBuilder.setParameter(idTokenKey, maskedToken);
        }

        return uriBuilder.build();
    }
}