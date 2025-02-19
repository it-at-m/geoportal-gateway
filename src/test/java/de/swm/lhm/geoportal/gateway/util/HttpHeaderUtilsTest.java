package de.swm.lhm.geoportal.gateway.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class HttpHeaderUtilsTest {

    @Test
    void isXmlText() {
        HttpHeaders headers = HttpHeaders.writableHttpHeaders(new HttpHeaders());
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML_VALUE);
        assertThat(HttpHeaderUtils.isContentTypeXml(headers), is(true));
    }

    @Test
    void isXmlApplication() {
        HttpHeaders headers = HttpHeaders.writableHttpHeaders(new HttpHeaders());
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE);
        assertThat(HttpHeaderUtils.isContentTypeXml(headers), is(true));
    }

    @Test
    void isNotXml() {
        HttpHeaders headers = HttpHeaders.writableHttpHeaders(new HttpHeaders());
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE);
        assertThat(HttpHeaderUtils.isContentTypeXml(headers), is(false));
    }


    @Test
    void isGmlVnd() {
        HttpHeaders headers = HttpHeaders.writableHttpHeaders(new HttpHeaders());
        headers.set(HttpHeaders.CONTENT_TYPE, MediaTypeExt.APPLICATION_VND_OGC_GML_VALUE);
        assertThat(HttpHeaderUtils.isContentTypeXmlOrGml(headers), is(true));
    }

    @Test
    void isGml() {
        HttpHeaders headers = HttpHeaders.writableHttpHeaders(new HttpHeaders());
        headers.set(HttpHeaders.CONTENT_TYPE, MediaTypeExt.APPLICATION_GML_VALUE);
        assertThat(HttpHeaderUtils.isContentTypeXmlOrGml(headers), is(true));
    }


    @Test
    void isNotGml() {
        HttpHeaders headers = HttpHeaders.writableHttpHeaders(new HttpHeaders());
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_GIF_VALUE);
        assertThat(HttpHeaderUtils.isContentTypeXmlOrGml(headers), is(false));
    }
}
