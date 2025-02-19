package de.swm.lhm.geoportal.gateway.resource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class ResourcePropertiesTest{
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ResourceProperties resourceProperties = new ResourceProperties();

    private String VALID_ENDPOINT = "/valid-endpoint";

    @ParameterizedTest
    @ValueSource(strings = {"/test/endpoint", "/example" })
    void noConstraintViolationForValidEndpoint(String validEndpoint) {
        resourceProperties.setEnableWebserver(false);
        resourceProperties.setEndpoint(validEndpoint);
        Set<ConstraintViolation<ResourceProperties>> violations = validator.validate(resourceProperties);
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"test/endpoint", "example", "./dir", "/endpoint/" })
    void constraintViolationForInvalidEndpoint(String invalidEndpoint) {
        resourceProperties.setEnableWebserver(false);
        resourceProperties.setEndpoint(invalidEndpoint);
        Set<ConstraintViolation<ResourceProperties>> violations = validator.validate(resourceProperties);
        assertThat(violations).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void noConstraintViolationWhenEnableWebserverSetToBooleanValue(boolean value){
        resourceProperties.setEnableWebserver(value);
        resourceProperties.setEndpoint(VALID_ENDPOINT);
        Set<ConstraintViolation<ResourceProperties>> violations = validator.validate(resourceProperties);
        assertThat(violations).hasSize(0);
    }

    @Test
    void constraintViolationIfEnableWebserverNotSet(){
        resourceProperties.setEndpoint(VALID_ENDPOINT);
        Set<ConstraintViolation<ResourceProperties>> violations = validator.validate(resourceProperties);
        assertThat(violations).hasSize(1);
    }
}
