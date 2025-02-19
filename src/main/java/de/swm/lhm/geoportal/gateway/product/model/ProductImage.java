package de.swm.lhm.geoportal.gateway.product.model;


import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

@Data
public class ProductImage {

    byte[] bytes;
    String name;

    @Builder
    ProductImage(final byte[] bytes, final String name) {

        if (StringUtils.isBlank(name)) {
            this.name = "default.png";
        } else {
            this.name = name;
        }

        this.bytes = Objects.requireNonNullElseGet(bytes, () -> new byte[0]);
    }

}
