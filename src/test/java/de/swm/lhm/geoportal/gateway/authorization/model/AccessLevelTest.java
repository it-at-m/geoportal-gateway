package de.swm.lhm.geoportal.gateway.authorization.model;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class AccessLevelTest {
    @Test
    void highestAccessLevelPublicRestricted() {
        AccessLevel highest = AccessLevel.PUBLIC.getHighestAccessLevel(AccessLevel.RESTRICTED);
        assertThat(highest, is(AccessLevel.RESTRICTED));
    }

    @Test
    void highestAccessLevelPublicProtected() {
        AccessLevel highest = AccessLevel.PUBLIC.getHighestAccessLevel(AccessLevel.PROTECTED);
        assertThat(highest, is(AccessLevel.PROTECTED));
    }

    @Test
    void highestAccessLevelProtectedRestricted() {
        AccessLevel highest = AccessLevel.RESTRICTED.getHighestAccessLevel(AccessLevel.PROTECTED);
        assertThat(highest, is(AccessLevel.RESTRICTED));
    }

    @Test
    void testPublicGetOrder() {
        assertThat(AccessLevel.PUBLIC.getOrder(), is(0));
    }

    @Test
    void testProtectedGetOrder() {
        assertThat(AccessLevel.PROTECTED.getOrder(), is(1));
    }

    @Test
    void testRestrictedGetOrder() {
        assertThat(AccessLevel.RESTRICTED.getOrder(), is(2));
    }

    @Test
    void testPublicAsMetadataClassificationCode() {
        assertThat(AccessLevel.PUBLIC.asMetadataClassificationCode(), is("unclassified"));
    }

    @Test
    void testProtectedAsMetadataClassificationCode() {
        assertThat(AccessLevel.PROTECTED.asMetadataClassificationCode(), is("restricted"));
    }

    @Test
    void testRestrictedAsMetadataClassificationCode() {
        assertThat(AccessLevel.RESTRICTED.asMetadataClassificationCode(), is("secret"));
    }

    @Test
    void testFromMetadataClassificationCodeUnclassified() {
        assertThat(AccessLevel.fromMetadataClassificationCode("unclassified"), is(Optional.of(AccessLevel.PUBLIC)));
    }

    @Test
    void testFromMetadataClassificationCodeRestricted() {
        assertThat(AccessLevel.fromMetadataClassificationCode("restricted"), is(Optional.of(AccessLevel.PROTECTED)));
    }

    @Test
    void testFromMetadataClassificationCodeConfidential() {
        assertThat(AccessLevel.fromMetadataClassificationCode("confidential"), is(Optional.of(AccessLevel.PROTECTED)));
    }

    @Test
    void testFromMetadataClassificationCodeSecret() {
        assertThat(AccessLevel.fromMetadataClassificationCode("secret"), is(Optional.of(AccessLevel.RESTRICTED)));
    }

    @Test
    void testFromMetadataClassificationCodeTopSecret() {
        assertThat(AccessLevel.fromMetadataClassificationCode("topsecret"), is(Optional.of(AccessLevel.RESTRICTED)));
    }

    @Test
    void testFromMetadataClassificationCodeInvalid() {
        assertThat(AccessLevel.fromMetadataClassificationCode("invalid"), is(Optional.empty()));
    }

    @Test
    void testGetHighestAccessLevel() {
        assertThat(AccessLevel.PUBLIC.getHighestAccessLevel(AccessLevel.RESTRICTED), is(AccessLevel.RESTRICTED));
        assertThat(AccessLevel.PROTECTED.getHighestAccessLevel(AccessLevel.PUBLIC), is(AccessLevel.PROTECTED));
        assertThat(AccessLevel.RESTRICTED.getHighestAccessLevel(AccessLevel.PROTECTED), is(AccessLevel.RESTRICTED));
    }

    @Test
    void testFindHighestAccessLevel() {
        assertThat(AccessLevel.findHighestAccessLevel(Set.of(AccessLevel.PUBLIC)), is(AccessLevel.PUBLIC));
        assertThat(AccessLevel.findHighestAccessLevel(Set.of(AccessLevel.PUBLIC, AccessLevel.PROTECTED)), is(AccessLevel.PROTECTED));
        assertThat(AccessLevel.findHighestAccessLevel(Set.of(AccessLevel.PUBLIC, AccessLevel.PROTECTED, AccessLevel.RESTRICTED)), is(AccessLevel.RESTRICTED));
    }

    @Test
    void testFromMetadataClassificationCodeLowerCase() {
        assertThat(AccessLevel.fromMetadataClassificationCode("UNCLASSIFIED"), is(Optional.of(AccessLevel.PUBLIC)));
        assertThat(AccessLevel.fromMetadataClassificationCode("RESTRICTED"), is(Optional.of(AccessLevel.PROTECTED)));
        assertThat(AccessLevel.fromMetadataClassificationCode("CONFIDENTIAL"), is(Optional.of(AccessLevel.PROTECTED)));
        assertThat(AccessLevel.fromMetadataClassificationCode("SECRET"), is(Optional.of(AccessLevel.RESTRICTED)));
        assertThat(AccessLevel.fromMetadataClassificationCode("TOPSECRET"), is(Optional.of(AccessLevel.RESTRICTED)));
    }

    @Test
    void testFromMetadataClassificationCodeMixedCase() {
        assertThat(AccessLevel.fromMetadataClassificationCode("UnClAsSiFiEd"), is(Optional.of(AccessLevel.PUBLIC)));
        assertThat(AccessLevel.fromMetadataClassificationCode("ReStRiCtEd"), is(Optional.of(AccessLevel.PROTECTED)));
        assertThat(AccessLevel.fromMetadataClassificationCode("CoNfIdEnTiAl"), is(Optional.of(AccessLevel.PROTECTED)));
        assertThat(AccessLevel.fromMetadataClassificationCode("SeCrEt"), is(Optional.of(AccessLevel.RESTRICTED)));
        assertThat(AccessLevel.fromMetadataClassificationCode("ToPsEcReT"), is(Optional.of(AccessLevel.RESTRICTED)));
    }

    @Test
    void testFindHighestAccessLevelWithEmptySet() {
        Set<AccessLevel> emptySet = Set.of();
        assertThat(AccessLevel.findHighestAccessLevel(emptySet), is(AccessLevel.PUBLIC));
    }

    @Test
    void testFindHighestAccessLevelVariousCombinations() {
        assertThat(AccessLevel.findHighestAccessLevel(Set.of(AccessLevel.PUBLIC, AccessLevel.PROTECTED)), is(AccessLevel.PROTECTED));
        assertThat(AccessLevel.findHighestAccessLevel(Set.of(AccessLevel.PROTECTED, AccessLevel.RESTRICTED)), is(AccessLevel.RESTRICTED));
        assertThat(AccessLevel.findHighestAccessLevel(Set.of(AccessLevel.PUBLIC, AccessLevel.RESTRICTED)), is(AccessLevel.RESTRICTED));
        assertThat(AccessLevel.findHighestAccessLevel(Set.of(AccessLevel.PUBLIC, AccessLevel.PROTECTED, AccessLevel.RESTRICTED)), is(AccessLevel.RESTRICTED));
        assertThat(AccessLevel.findHighestAccessLevel(Set.of(AccessLevel.PUBLIC)), is(AccessLevel.PUBLIC));
    }

}
