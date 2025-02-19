package de.swm.lhm.geoportal.gateway.util;

import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
public class UuidUtils {

    public static boolean isUuid(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

}
