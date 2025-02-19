package de.swm.lhm.geoportal.gateway.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

// copy from https://github.com/socketio/socket.io-client-java/blob/main/src/main/java/io/socket/client/Url.java

@UtilityClass
public class UrlParser {

    private static final Pattern PATTERN_AUTHORITY = compile("^(.*@)?(?<host>[^:]+)(:(?<port>\\d+))?$");
    private static final String URI_DELIMITER = "/";

    public static URI parse(String uriString) throws URISyntaxException {

        if (isEmptyString(uriString)) {
            throw new URISyntaxException("", "given uri string is null or empty");
        }

        URI parsedUri = new URI(uriString.trim());

        String host = resolveHostStrict(parsedUri);
        String scheme = resolveScheme(parsedUri.getScheme());

        return new URI(
                String.format(
                        "%s://%s%s%s%s%s%s",
                        scheme,
                        resolveString(parsedUri.getRawUserInfo(), null, "@"),
                        host,
                        resolveString(resolvePort(parsedUri.getPort(), scheme), ":", null),
                        resolvePath(parsedUri.getRawPath(), host),
                        resolveString(parsedUri.getRawQuery(), "?", null),
                        resolveString(parsedUri.getRawFragment(), "#", null)
                )
        );

    }

    public static String resolveScheme(String scheme) {

        if (isEmptyString(scheme)) {
            return "http";
        }

        return scheme.trim();

    }

    public static String resolveString(int integer, String prefix, String suffix) {

        if (integer == -1) {
            return "";
        }

        return resolveString(String.valueOf(integer), prefix, suffix);

    }

    public static String resolveString(final String string, final String prefix,final String suffix) {

        if (isEmptyString(string)) {
            return "";
        }

        String resolvedString = string.trim();

        if (isNotEmptyString(prefix)) {
            resolvedString = prefix.trim() + resolvedString;
        }

        if (isNotEmptyString(suffix)) {
            resolvedString = resolvedString + suffix.trim();
        }

        return resolvedString;

    }

    public static String resolvePath(final String path, final String host) {

        if (isEmptyString(path)) {
            return URI_DELIMITER;
        }

        if (path.startsWith(host)) {
            return resolvePath(path.substring(host.length()), host);
        }

        String newPath = path;
        if (!path.startsWith(URI_DELIMITER)) {
            newPath = URI_DELIMITER + path;
        }

        return newPath.trim();
    }

    public static String resolveHostStrict(URI uri) throws URISyntaxException {

        Optional<String> result = resolveHost(uri);

        if (result.isPresent()) {
            return result.get();
        }

        throw new InvalidParameterException("Could not extract host from uri " + uri);

    }

    public static String resolveHostTolerant(URI uri) {

        try {
            return resolveHost(uri).orElse("");
        } catch (URISyntaxException e) {
            return "";
        }

    }

    public static Optional<String> resolveHost(URI uri) throws URISyntaxException {

        String host = uri.getHost();

        if (isNotEmptyString(host)) {
            return Optional.of(host.trim());
        }

        if (uri.getRawAuthority() != null) {
            Optional<String> result = resolveHostFromAuthority(uri);
            if (result.isPresent()) {
                return result;
            }
        }

        if (uri.getScheme() == null || uri.getScheme().isEmpty()) {
            host = new URI("my://" + uri).getHost();

            if (isNotEmptyString(host)) {
                return Optional.of(host.trim());
            }
        }
        return Optional.empty();
    }

    public static boolean isNotEmptyString(String string) {
        return !StringUtils.isBlank(string);
    }

    public static Optional<String> resolveHostFromAuthority(URI uri) {
        Matcher matcher = PATTERN_AUTHORITY.matcher(uri.getRawAuthority());

        if (matcher.matches()) {
            return Optional.of(matcher.group("host").trim());
        }

        return Optional.empty();

    }

    public static Optional<Integer> resolvePortFromAuthority(URI uri) {
        Matcher matcher = PATTERN_AUTHORITY.matcher(uri.getRawAuthority());

        if (matcher.matches()) {
            return Optional.of(Integer.valueOf(matcher.group("port").trim()));
        }

        return Optional.empty();

    }

    public static int resolvePort(int port, String scheme) {

        if (port == -1) {
            return scheme.matches("^(http|ws)s$") ? 443 : 80;
        }

        return port;

    }

    private static boolean isEmptyString(String string) {
        return StringUtils.isBlank(string);
    }

}

