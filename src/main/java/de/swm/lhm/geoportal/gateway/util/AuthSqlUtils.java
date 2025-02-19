package de.swm.lhm.geoportal.gateway.util;

import de.swm.lhm.geoportal.gateway.authorization.model.AccessLevel;
import de.swm.lhm.geoportal.gateway.authorization.model.AuthorizationGroup;
import lombok.experimental.UtilityClass;
import org.springframework.data.relational.core.query.Criteria;

import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.regex.Pattern;


@UtilityClass
public  class AuthSqlUtils {

    // the only allowed chars for roles are [a-zA-Z0-9_ ]
    // to prevent possible sql injections the roles are checked against this regex
    public static final Pattern ROLE_ALLOWED_CHARS_PATTERN = Pattern.compile("[\\w_\\s]+");

    /**
     * Criteria that can be used as part of a sql query in the R2dbcEntityTemplate
     *
     * @param authorizationGroup authorizationGroup with granted productRoles and authLevelHigh for the user
     * @return Criteria that can be used as part of a sql query in the R2dbcEntityTemplate
     */
    public static Criteria getStandaloneCriteriaFromAuthorizationGroup(AuthorizationGroup authorizationGroup){

        return Criteria.from()
                .and(getCriteriaFromAccessLevel(authorizationGroup))
                .and(getCriteriaFromAuthLevel(authorizationGroup));

    }

    /**
     * Criteria that can be used as part of a sql query in the R2dbcEntityTemplate
     *
     * @param criteria to be concatenated with a sql AND clause
     * @param authorizationGroup authorizationGroup with granted productRoles and authLevelHigh for the user
     * @return Criteria that can be used as part of a sql query in the R2dbcEntityTemplate
     */
    public static Criteria getConcatenatedCriteriaFromAuthorizationGroup(Criteria criteria, AuthorizationGroup authorizationGroup){
        return criteria
                .and(getCriteriaFromAccessLevel(authorizationGroup))
                .and(getCriteriaFromAuthLevel(authorizationGroup));
    }

    public static Criteria getCriteriaFromAccessLevel(AuthorizationGroup authorizationGroup){
        return getCriteriaFromAccessLevel(authorizationGroup.getProductRoles());
    }

    public static Criteria getCriteriaFromAccessLevel(Collection<String> productRoles){
        Criteria criteria = Criteria.where("role_name").isNull()
                .or("access_level").not(AccessLevel.RESTRICTED.name());
        if (productRoles != null && !productRoles.isEmpty()) {
            checkRoles(productRoles);
            criteria = criteria.or("role_name").in(productRoles);
        }
        return criteria;
    }

    public static Criteria getCriteriaFromAuthLevel(AuthorizationGroup authorizationGroup){
        return getCriteriaFromAuthLevel(authorizationGroup.isAuthLevelHigh());
    }

    public static Criteria getCriteriaFromAuthLevel(boolean isAuthLevelHigh){
        Criteria criteria = Criteria.where("auth_level_high").isFalse();
        if (isAuthLevelHigh) {
            criteria = criteria.or("auth_level_high").isTrue();
        }
        return criteria;
    }

    public static void checkRoles(Collection<String> strings){
        strings.forEach(AuthSqlUtils::checkRole);
    }

    public static void checkRole(String string){
        if (!ROLE_ALLOWED_CHARS_PATTERN.matcher(string).matches())
            throw new InvalidParameterException(String.format("Invalid String in AuthUtils found, string is '%s'", string));
    }

}
