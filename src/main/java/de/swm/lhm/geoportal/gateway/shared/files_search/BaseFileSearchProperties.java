package de.swm.lhm.geoportal.gateway.shared.files_search;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseFileSearchProperties {
    private String dir;
    private String globPattern;
    private String fileIdentifier;

}