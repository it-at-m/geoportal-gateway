package de.swm.lhm.geoportal.gateway.util;

import de.swm.lhm.geoportal.gateway.authorization.model.AuthorizationGroup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.relational.core.query.Criteria;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.data.relational.core.query.Criteria.where;


class AuthSqlUtilsTest {

    @Test
    void getCriteriaFromAuthorizationGroupWithOtherConcatenatedCriteria() {

        String result = AuthSqlUtils.getConcatenatedCriteriaFromAuthorizationGroup(
                Criteria.where("other").isNull(),
                AuthorizationGroup.builder()
                        .productRoles(List.of("a", "b", "c"))
                        .authLevelHigh(true)
                        .build()
        ).toString();

        assertThat(result, is("other IS NULL AND (role_name IS NULL OR access_level != 'RESTRICTED' OR role_name IN ('a', 'b', 'c')) AND (auth_level_high IS FALSE OR auth_level_high IS TRUE)"));

        String result2 = where("other").isNull()
                .and(
                        AuthSqlUtils.getStandaloneCriteriaFromAuthorizationGroup(
                                AuthorizationGroup.builder()
                                        .productRoles(List.of("a", "b", "c"))
                                        .authLevelHigh(true)
                                        .build()
                )).toString();

        assertThat(result2, is("other IS NULL AND ((role_name IS NULL OR access_level != 'RESTRICTED' OR role_name IN ('a', 'b', 'c')) AND (auth_level_high IS FALSE OR auth_level_high IS TRUE))"));

    }


    @Test
    void getConcatenatedCriteriaFromAuthorizationGroupAuthLevelHigh() {

        String result = AuthSqlUtils.getStandaloneCriteriaFromAuthorizationGroup(
                AuthorizationGroup.builder()
                        .productRoles(List.of("a", "b", "c"))
                        .authLevelHigh(true)
                        .build()
        ).toString();

        assertThat(result, is("(role_name IS NULL OR access_level != 'RESTRICTED' OR role_name IN ('a', 'b', 'c')) AND (auth_level_high IS FALSE OR auth_level_high IS TRUE)"));

    }

    @Test
    void getConcatenatedCriteriaFromAuthorizationGroupNotAuthLevelHigh() {

        String result = AuthSqlUtils.getStandaloneCriteriaFromAuthorizationGroup(
                AuthorizationGroup.builder()
                        .productRoles(List.of("a", "b", "c"))
                        .authLevelHigh(false)
                        .build()
        ).toString();

        assertThat(result, is("(role_name IS NULL OR access_level != 'RESTRICTED' OR role_name IN ('a', 'b', 'c')) AND (auth_level_high IS FALSE)"));

    }

    @Test
    void getConcatenatedCriteriaFromAuthorizationGroupNoProdcutRoles() {

        String result = AuthSqlUtils.getStandaloneCriteriaFromAuthorizationGroup(
                AuthorizationGroup.builder()
                        .authLevelHigh(false)
                        .build()
        ).toString();

        assertThat(result, is("(role_name IS NULL OR access_level != 'RESTRICTED') AND (auth_level_high IS FALSE)"));

    }

    @ParameterizedTest
    @ValueSource(strings = {"a-b", "a\\'b", "äö#"})
    void testTestRolesFails(String string) {

        Set<String> testSet = Set.of(string);

        assertThrows(
                InvalidParameterException.class,
                () -> AuthSqlUtils.checkRoles(testSet)
        );

        assertThrows(
                InvalidParameterException.class,
                () -> AuthSqlUtils.checkRole(string)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "a_b", "aA1b", "Default Resource"})
    void testTestRolesSuccess(String string) {

        Set<String> testSet = Set.of(string);

        assertDoesNotThrow(() -> AuthSqlUtils.checkRoles(testSet));

        assertDoesNotThrow(() -> AuthSqlUtils.checkRole(string));

    }
}