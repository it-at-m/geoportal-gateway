package de.swm.lhm.geoportal.gateway.authentication.keycloak;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.swm.lhm.geoportal.gateway.authorization.model.StorkQaaLevel;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class UserInfoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testDeserializeUserInfo() throws IOException {
        String json = """
                {
                    "authorities": [
                        "Produkt_ta_produkt21060452_",
                        "Produkt_ta_produkt24101204_",
                        "Produkt_ta_produkt11080314"
                    ],
                    "authlevel": "STORK-QAA-Level-4"
                }""";

        UserInfo userInfo = objectMapper.readValue(json, UserInfo.class);

        assertThat(userInfo, is(notNullValue()));
        assertThat(userInfo.getAuthorities(), containsInAnyOrder(
                "Produkt_ta_produkt21060452_",
                "Produkt_ta_produkt24101204_",
                "Produkt_ta_produkt11080314"
        ));
        assertThat(userInfo.getAuthLevel(), is(StorkQaaLevel.STORK_QAA_LEVEL_4));
    }

    @Test
    void testDeserializeFullUserInfo() throws IOException {
        String json = """
                {
                    "sub": "d575d5ae-4d26-4c1f-ac7a-77a24a3d14f9",
                    "postkorbMailAddress": "null@bsp-postkorb-pre.akdb.doi-de.net",
                    "email_verified": false,
                    "address": {},
                    "user_name": "gsm_user4",
                    "preferred_username": "gsm_user4",
                    "user_roles": [
                        "ROLE_test_fd74c6b6",
                        "ROLE_ITM",
                        "ROLE_GSM_nonComposite",
                        "ROLE_GSM",
                        "ROLE_THW_nonComposite",
                        "ROLE_ITM_nonComposite",
                        "ROLE_THW"
                    ],
                    "given_name": "gsm",
                    "authorities": [
                        "Produkt_ta_produkt21060452_",
                        "Produkt_ta_produkt24101204_",
                        "Produkt_ta_produkt11080314"
                    ],
                    "authlevel": "STORK-QAA-Level-4",
                    "name": "gsm user4",
                    "family_name": "user4",
                    "email": "gsm_user4@gsm.muc"
                }""";

        UserInfo userInfo = objectMapper.readValue(json, UserInfo.class);

        assertThat(userInfo, is(notNullValue()));
        assertThat(userInfo.getAuthorities(), containsInAnyOrder(
                "Produkt_ta_produkt21060452_",
                "Produkt_ta_produkt24101204_",
                "Produkt_ta_produkt11080314"
        ));
        assertThat(userInfo.getAuthLevel(), is(StorkQaaLevel.STORK_QAA_LEVEL_4));

        assertThat(userInfo.getSubject(), is("d575d5ae-4d26-4c1f-ac7a-77a24a3d14f9"));
        assertThat(userInfo.getUserName(), is("gsm_user4"));
        assertThat(userInfo.getPreferredUserName(), is("gsm_user4"));

    }
}