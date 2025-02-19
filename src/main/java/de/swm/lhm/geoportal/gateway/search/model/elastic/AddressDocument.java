package de.swm.lhm.geoportal.gateway.search.model.elastic;

import lombok.Data;

import java.util.Objects;

@Data
public class AddressDocument {

    private String id;
    private Integer objectId;
    private String streetName;
    private String houseNumber;
    private String streetNameComplete;
    private String zipCode;
    private String city;
    private double topValue;
    private double rightValue;
    private String srs;

    @Override
    public int hashCode() {
        return Objects.hash(objectId, srs);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof AddressDocument addressDocument)) {
            return false;
        }

        return Objects.equals(objectId, addressDocument.getObjectId())
                && Objects.equals(srs, addressDocument.getSrs());
    }

    @Override
    public String toString() {
        return String.format("%s %s, %s %s", getStreetName(), getHouseNumber(), getZipCode(), getCity());
    }
}
