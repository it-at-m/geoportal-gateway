package de.swm.lhm.geoportal.gateway.m2m.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EaiResponse {
    @JsonProperty("responseType")
    private String responseType;

    @JsonProperty("resultCode")
    private String resultCode;

    @JsonProperty("resultText")
    private String resultText;

    @JsonProperty("errorCode")
    private String errorCode;

    @JsonProperty("errorText")
    private String errorText;

    @JsonProperty("errorDetails")
    private String errorDetails;

    @JsonProperty("executionSteps")
    private Map<String, Boolean> executionSteps;

    @JsonProperty("password")
    private String password;

}