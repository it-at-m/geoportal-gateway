package de.swm.lhm.geoportal.gateway.util;

import lombok.experimental.UtilityClass;
import org.springframework.http.MediaType;

/**
 * Extensions to Springs MediaType
 */
@UtilityClass
public class MediaTypeExt {
    public static final MediaType APPLICATION_VND_GEO_JSON = new MediaType("application", "vnd.geo+json");
    public static final String APPLICATION_VND_GEO_JSON_VALUE = "application/vnd.geo+json";
    public static final MediaType APPLICATION_GEO_JSON = new MediaType("application", "geo+json");
    public static final String APPLICATION_GEO_JSON_VALUE = "application/geo+json";

    public static final MediaType APPLICATION_GML = new MediaType("application", "gml");
    public static final String APPLICATION_GML_VALUE = "application/gml";

    public static final MediaType APPLICATION_VND_OGC_GML = new MediaType("application", "vnd.ogc.gml");
    public static final String APPLICATION_VND_OGC_GML_VALUE = "application/vnd.ogc.gml";

}
