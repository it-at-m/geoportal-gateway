package de.swm.lhm.geoportal.gateway.util;

import lombok.experimental.UtilityClass;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

@UtilityClass
public class HttpHeaderUtils {

    private static boolean isContentType(HttpHeaders httpHeaders, Predicate<String> headerValuePredicate) {
        List<String> headers = httpHeaders.get(HttpHeaders.CONTENT_TYPE);
        if (headers == null || headers.isEmpty()) {
            return false;
        }
        return headers
                .stream()
                .anyMatch(headerValue -> headerValuePredicate.test(headerValue.toLowerCase(Locale.ROOT)));

    }

    private static boolean isXml(String contentType) {
        return contentType.startsWith(MediaType.TEXT_XML_VALUE) || contentType.startsWith(MediaType.APPLICATION_XML_VALUE);
    }

    private static boolean isGml(String contentType) {
        return contentType.startsWith(MediaTypeExt.APPLICATION_GML_VALUE) || contentType.startsWith(MediaTypeExt.APPLICATION_VND_OGC_GML_VALUE);
    }

    private static boolean isJson(String contentType) {
        return contentType.startsWith(MediaType.APPLICATION_JSON_VALUE) || contentType.startsWith(
                MediaTypeExt.APPLICATION_GEO_JSON_VALUE) || contentType.startsWith(MediaTypeExt.APPLICATION_VND_GEO_JSON_VALUE);
    }

    private static boolean isText(String contentType) {
        return contentType.startsWith(MediaType.TEXT_PLAIN_VALUE);
    }

    public static boolean isContentTypeXml(HttpHeaders httpHeaders) {
        return isContentType(httpHeaders, HttpHeaderUtils::isXml);
    }

    public static boolean isContentTypeXmlOrGml(HttpHeaders httpHeaders) {
        return isContentType(httpHeaders, h -> isXml(h) || isGml(h));
    }

    public static boolean isContentTypeJson(HttpHeaders httpHeaders) {
        return isContentType(httpHeaders, HttpHeaderUtils::isJson);
    }

    public static boolean isTextFormatContentType(HttpHeaders httpHeaders) {
        return isContentType(httpHeaders, h -> isJson(h) || isText(h) || isXml(h) || isGml(h));
    }
}
