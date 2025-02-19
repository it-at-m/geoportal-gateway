package de.swm.lhm.geoportal.gateway.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

public class ExtendedURIBuilder extends URIBuilder {

    public ExtendedURIBuilder(String string) throws URISyntaxException {
        super(string);
    }

    public ExtendedURIBuilder(URI uri) {
        super(uri);
    }

    public ExtendedURIBuilder addPath(final String subPath) {

        if (subPath == null || StringUtils.isBlank(subPath) || StringUtils.isBlank(subPath.trim()))
            return this;

        final String newSubPath = subPath.trim().replace("\\", "/");

        if ("/".equals(newSubPath)) {
            return this;
        }

        return (ExtendedURIBuilder) setPathSegments(
                Arrays.stream(appendSegmentToPath(getPath(), newSubPath).split("/"))
                        .filter(segment -> segment != null && !segment.isEmpty())
                        .toList()
        );
    }

    private String appendSegmentToPath(final String path, final String segment) {
        String newPath = path;
        if (path == null || path.isEmpty()) {
            newPath = "/";
        }

        if (newPath.charAt(newPath.length() - 1) == '/' || segment.startsWith("/")) {
            return newPath + segment;
        }

        return newPath + "/" + segment;
    }

}
