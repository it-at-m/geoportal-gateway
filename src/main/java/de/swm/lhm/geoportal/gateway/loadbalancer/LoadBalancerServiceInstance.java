package de.swm.lhm.geoportal.gateway.loadbalancer;

import com.google.common.hash.Hashing;
import de.swm.lhm.geoportal.gateway.util.UrlParser;
import lombok.Getter;
import org.springframework.cloud.client.ServiceInstance;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;


// https://github.com/socketio/socket.io-client-java/blob/main/src/main/java/io/socket/client/Url.java

public class LoadBalancerServiceInstance implements ServiceInstance {

    @Getter
    private final String instanceId;
    @Getter
    private final String serviceId;

    @Getter
    private final Map<String, String> metadata = new HashMap<>();
    private final URI uri;

    public LoadBalancerServiceInstance(String serviceId, String uri) throws URISyntaxException {

        this.serviceId = serviceId;
        this.uri = UrlParser.parse(uri);
        this.instanceId = serviceId + "@" + Hashing.sha256()
                .hashString(this.uri.toString(), StandardCharsets.UTF_8);

    }

    @Override
    public String getHost() {

        String result = this.uri.getHost();

        if (result == null) {
            Optional<String> optionalResult = UrlParser.resolveHostFromAuthority(this.uri);
            if (optionalResult.isPresent()) {
                result = optionalResult.get();
            }
        }

        return result;

    }

    @Override
    public int getPort() {

        int port = this.uri.getPort();

        if (port == -1) {
            Optional<Integer> optionalResult = UrlParser.resolvePortFromAuthority(this.uri);
            if (optionalResult.isPresent()) {
                port = optionalResult.get();
            }
        }

        return port;

    }

    @Override
    public boolean isSecure() {
        return getScheme().matches("^(http|ws)s$");
    }

    @Override
    public URI getUri() {
        return this.uri;
    }

    @Override
    public String getScheme() {
        return this.uri.getScheme();
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId, uri);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoadBalancerServiceInstance)) return false;
        LoadBalancerServiceInstance that = (LoadBalancerServiceInstance) o;
        return Objects.equals(uri, that.uri) && Objects.equals(serviceId, that.serviceId);
    }

    @Override
    public String toString() {
        return "LoadBalancerServiceInstance{"
                + "uri=" + uri
                + ", instanceId='" + instanceId + '\''
                + ", serviceId='" + serviceId + '\''
                + '}';
    }
}
