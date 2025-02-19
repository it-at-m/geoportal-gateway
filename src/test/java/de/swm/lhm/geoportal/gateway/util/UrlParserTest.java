package de.swm.lhm.geoportal.gateway.util;

import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import static de.swm.lhm.geoportal.gateway.util.UrlParser.resolveHost;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


class UrlParserTest {

    @Test
    void parseShouldThrowIfEmpty() {
        assertThatThrownBy(() -> parse("")).isInstanceOf(URISyntaxException.class);
    }

    @Test
    void resolveStringShouldStripHostIfPathStartsWithHost() {
        String path = UrlParser.resolvePath("localhost:8989", "localhost");
        assertThat(path, is("/:8989"));
    }

    @Test
    void testResolveHostWithHost() throws URISyntaxException {
        URI uri = new URI("http://example.com/path");
        Optional<String> host = resolveHost(uri);
        Assertions.assertThat(host).isPresent();
        Assertions.assertThat(host.get()).isEqualTo("example.com");
    }

    @Test
    void testResolveHostFromAuthority() throws URISyntaxException {
        // Assuming resolveHostFromAuthority works as expected for this URI
        URI uri = new URI("http://user:pass@example.com/path");
        Optional<String> host = resolveHost(uri);
        Assertions.assertThat(host).isPresent();
        Assertions.assertThat(host.get()).isEqualTo("example.com");
    }

    @Test
    void testResolveHostEmpty() throws URISyntaxException {
        // Assuming the host and authority cannot be resolved
        URI uri = new URI("http:///"); // Triple slash might cause no host part
        Optional<String> host = resolveHost(uri);
        Assertions.assertThat(host).isEmpty();
    }

    @Test
    void testResolveHostFromAuthorityWithUserinfoHostPort() throws URISyntaxException {
        URI uriWithAuthority = new URI("http://user:pass@example.com:8080");
        Optional<String> result = UrlParser.resolveHostFromAuthority(uriWithAuthority);
        Assertions.assertThat(result).isPresent().contains("example.com");
    }

    @Test
    void testResolveHostFromAuthorityWithHostOnly() throws URISyntaxException {
        URI uriWithAuthorityOnly = new URI("http://example.com");
        Optional<String> result = UrlParser.resolveHostFromAuthority(uriWithAuthorityOnly);
        Assertions.assertThat(result).isPresent().contains("example.com");
    }


    @Test
    void testResolveHostFromAuthorityWithInvalidURI() {
        // This assumes your resolveHostFromAuthority method declares throwing URISyntaxException
        assertThrows(URISyntaxException.class, () -> {
            URI invalidUri = new URI("this is not a valid uri");
            UrlParser.resolveHostFromAuthority(invalidUri);
        });
    }

    @Test
    void testResolveHostThrowsExceptionForInvalidURI() {
        assertThrows(URISyntaxException.class, () -> {
            URI uri = new URI("://shouldfail");
            resolveHost(uri);
        });
    }


    @Test
    void testResolvePortFromAuthorityWithPort() throws URISyntaxException {
        URI uri = new URI("http://user:pass@example.com:8080");
        Optional<Integer> port = UrlParser.resolvePortFromAuthority(uri);
        Assertions.assertThat(port).isPresent().contains(8080);
    }

    @Test
    void testResolvePortFromAuthorityWithInvalidURI() {
        assertThrows(URISyntaxException.class, () -> {
            URI invalidUri = new URI("this is not a valid uri");
            UrlParser.resolvePortFromAuthority(invalidUri);
        });
    }

    @Test
    void resolveStringShouldReturnEmptyString() {
        String resolvedString = UrlParser.resolveString("", "", "");
        assertThat(resolvedString, is(""));
    }

    @Test
    void parse() {
        assertThat(parse("http://username:password@host:8080/directory/file?query#ref").toString(), is("http://username:password@host:8080/directory/file?query#ref"));
    }

    @Test
    void parseRelativePath() {
        URI uri = parse("https://woot.com/test");
        assertThat(uri.getScheme(), is("https"));
        assertThat(uri.getHost(), is("woot.com"));
        assertThat(uri.getPath(), is("/test"));
    }

    @Test
    void parseNoProtocol() {
        URI uri = parse("//localhost:3000");
        assertThat(uri.getHost(), is("localhost"));
        assertThat(uri.getPort(), is(3000));
    }

    @Test
    void parseNamespace() {
        assertThat(parse("http://woot.com/woot").getPath(), is("/woot"));
        assertThat(parse("http://google.com").getPath(), is("/"));
        assertThat(parse("http://google.com/").getPath(), is("/"));
    }

    @Test
    void parseDefaultPort() {
        assertThat(parse("http://google.com/").toString(), is("http://google.com:80/"));
        assertThat(parse("https://google.com/").toString(), is("https://google.com:443/"));
    }

    @Test
    void testWsProtocol() {
        URI uri = parse("ws://woot.com/test");
        assertThat(uri.getScheme(), is("ws"));
        assertThat(uri.getHost(), is("woot.com"));
        assertThat(uri.getPort(), is(80));
        assertThat(uri.getPath(), is("/test"));
    }

    @Test
    void testWssProtocol() {
        URI uri = parse("wss://woot.com/test");
        assertThat(uri.getScheme(), is("wss"));
        assertThat(uri.getHost(), is("woot.com"));
        assertThat(uri.getPort(), is(443));
        assertThat(uri.getPath(), is("/test"));
    }

    @SneakyThrows
    private URI parse(String uri) {
        return UrlParser.parse(uri);
    }

}