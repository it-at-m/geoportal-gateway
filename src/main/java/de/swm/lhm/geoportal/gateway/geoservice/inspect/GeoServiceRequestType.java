package de.swm.lhm.geoportal.gateway.geoservice.inspect;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public enum GeoServiceRequestType {

    // String values match the valid GET parameter value for these requests
    TRANSACTION("transaction"),
    GET_MAP("getmap"),
    GET_CAPABILITIES("getcapabilities"),
    GET_PROPERTY_VALUE("getpropertyvalue"),
    CREATE_STORED_QUERY("createstoredquery"),
    DROP_STORED_QUERY("dropstoredquery"),
    LIST_STORED_QUERIES("liststoredqueries"),
    DESCRIBE_STORED_QUERIES("describestoredqueries"),
    GET_FEATURE_WITH_LOCK("getfeaturewithlock"),
    GET_TILE("gettile"),
    GET_FEATURE("getfeature"),
    DESCRIBE_FEATURE_TYPE("describefeaturetype"),
    GET_FEATURE_INFO("getfeatureinfo");

    public final String name;

    private GeoServiceRequestType(String name) {
        this.name = name;
    }

    public static Optional<GeoServiceRequestType> getFromString(String input) {
        if (StringUtils.isBlank(input)) {
            return Optional.empty();
        }
        String inputLower = input.toLowerCase(Locale.ROOT).replaceAll(".[\\-_]", "");
        return Arrays.stream(GeoServiceRequestType.values())
                .filter(value -> value.name.equals(inputLower))
                .findFirst();
    }

    public String getGetParameterValue() {
        return this.name;
    }
}
