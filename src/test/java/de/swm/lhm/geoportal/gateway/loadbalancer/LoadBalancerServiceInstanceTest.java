package de.swm.lhm.geoportal.gateway.loadbalancer;

import com.jparams.verifier.tostring.NameStyle;
import com.jparams.verifier.tostring.ToStringVerifier;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;


class LoadBalancerServiceInstanceTest {

    private static Stream<Arguments> getArgumentsForUriTest() {

        //noinspection HttpUrlsUsage
        return Stream.of(
                Arguments.of("http://test.de/test", "http", false),
                Arguments.of("test.de/test", "http", false),
                Arguments.of("//test.de/test", "http", false),
                Arguments.of("ws://test.de/test", "ws", false),
                Arguments.of("https://test.de/test", "https", true),
                Arguments.of("wss://test.de/test", "wss", true)
        );

    }

    @ParameterizedTest
    @MethodSource("getArgumentsForUriTest")
    void uriTest(String uri, String expectedScheme, boolean isSecure) throws URISyntaxException {

        LoadBalancerServiceInstance test = new LoadBalancerServiceInstance("test", uri);

        assertThat(test.getServiceId(), is("test"));

        if (isSecure) {
            assertTrue(test.isSecure());
            assertThat(test.getPort(), is(443));
            assertThat(test.getUri().toString(), is(expectedScheme + "://test.de:443/test"));
        } else {
            assertFalse(test.isSecure());
            assertThat(test.getPort(), is(80));
            assertThat(test.getUri().toString(), is(expectedScheme + "://test.de:80/test"));
        }

        assertThat(test.getHost(), is("test.de"));
        assertThat(test.getScheme(), is(expectedScheme));

    }

    @Test
    void hostFromAuthorityTest() throws URISyntaxException {

        String url = "lb://test__1__";
        LoadBalancerServiceInstance test = new LoadBalancerServiceInstance("test", url);

        assertThat(test.getScheme(), is("lb"));
        assertThat(test.getHost(), is("test__1__"));
        assertThat(test.getPort(), is(80));
        assertThat(test.getUri().toString(), is("lb://test__1__:80/"));

    }

    @Test
    void ipv6Test() throws URISyntaxException {

        String url = "http://[::1]:8080";
        LoadBalancerServiceInstance test = new LoadBalancerServiceInstance("test", url);

        assertThat(test.getScheme(), is("http"));
        assertThat(test.getHost(), is("[::1]"));
        assertThat(test.getPort(), is(8080));
        assertThat(test.getUri().toString(), is("http://[::1]:8080/"));

    }

    @Test
    void uriExceptionTest() {

        Exception exception = assertThrows(URISyntaxException.class, () ->
            new LoadBalancerServiceInstance("test", "://test.de/test")
        );

        assertThat(exception.getMessage(), containsString("Expected scheme name"));

    }

    @Test
    void uriRelativeExceptionTest() {

        Exception exception = assertThrows(URISyntaxException.class, () ->
                new LoadBalancerServiceInstance("test", "    ")
        );

        assertThat(exception.getMessage(), containsString("given uri string is null or empty"));

    }

    @Test
    void uriEmptyExceptionTest() {

        Exception exception = assertThrows(InvalidParameterException.class, () ->
            new LoadBalancerServiceInstance("test", "/test.de/test")
        );

        assertThat(exception.getMessage(), containsString("Could not extract host from uri"));

    }

    @Test
    void equalsTest() {

        EqualsVerifier.simple()
                .forClass(LoadBalancerServiceInstance.class)
                .withIgnoredFields("instanceId", "metadata")
                .verify();

    }

    @Test
    void toStringTest() {

        ToStringVerifier.forClass(LoadBalancerServiceInstance.class)
                .withClassName(NameStyle.SIMPLE_NAME)
                .withIgnoredFields("metadata")
                .verify();

    }
}
