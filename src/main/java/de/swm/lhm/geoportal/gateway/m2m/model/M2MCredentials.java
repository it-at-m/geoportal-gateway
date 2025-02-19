package de.swm.lhm.geoportal.gateway.m2m.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class M2MCredentials {
    private String userName;
    private String password;
}
