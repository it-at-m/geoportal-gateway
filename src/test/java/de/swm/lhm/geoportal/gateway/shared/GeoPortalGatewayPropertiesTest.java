package de.swm.lhm.geoportal.gateway.shared;


import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


class GeoPortalGatewayPropertiesTest {

    @Nested
    @ExtendWith({SpringExtension.class})
    @EnableConfigurationProperties(value = GeoPortalGatewayProperties.class)
    class GeoPortalGatewayPropertiesConfigNullTest {

        @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
        @Autowired
        private GeoPortalGatewayProperties geoportalGatewayProperties;

        @Test
        void test() {
            assertThat(geoportalGatewayProperties.getExternalUrlPrepared(), is("http://localhost"));
            assertThat(geoportalGatewayProperties.getStagePrepared(), is( Stage.CONFIGURATION));
        }
    }

    @Nested
    @ExtendWith({SpringExtension.class})
    @EnableConfigurationProperties(value = GeoPortalGatewayProperties.class)
    @TestPropertySource(properties = {
            "geoportal.gateway.external-url=localhost"
    })
    class GeoPortalGatewayPropertiesConfigLocalhostTest {

        @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
        @Autowired
        private GeoPortalGatewayProperties geoportalGatewayProperties;

        @Test
        void test() {
            assertThat(geoportalGatewayProperties.getExternalUrlPrepared(), is("http://localhost"));
            assertThat(geoportalGatewayProperties.getStagePrepared(), is( Stage.CONFIGURATION));
        }
    }

    @Nested
    @ExtendWith({SpringExtension.class})
    @EnableConfigurationProperties(value = GeoPortalGatewayProperties.class)
    @TestPropertySource(properties = {
            "geoportal.gateway.stage=config"
    })
    class GeoPortalGatewayPropertiesConfigConfigAsStageTest {

        @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
        @Autowired
        private GeoPortalGatewayProperties geoportalGatewayProperties;

        @Test
        void test() {
            assertThat(geoportalGatewayProperties.getStagePrepared(), is( Stage.CONFIGURATION));
        }
    }

    @EnableConfigurationProperties(GeoPortalGatewayProperties.class)
    static class GatewayPropertiesStartupFailConfiguration {
    }

    @Test
    void GatewayPropertiesStartupFailTest() {

        // https://stackoverflow.com/questions/31692863/what-is-the-best-way-to-test-that-a-spring-application-context-fails-to-start

       new ApplicationContextRunner()
               .withInitializer(new ConfigDataApplicationContextInitializer())
               .withInitializer(new ConditionEvaluationReportLoggingListener())
               .withUserConfiguration(GatewayPropertiesStartupFailConfiguration.class)
               .withPropertyValues(            "geoportal.gateway.stage=nonExisting")
               .run((context) -> {

                    assertThat(context.getStartupFailure(), is(not(nullValue())));
                    assertThat(context.getStartupFailure().getMessage(), containsString("GatewayProperties"));

                    List<String> failureMessages = extractFailureCauseMessages(context);
                    assertThat(failureMessages, hasSize(1));
                    assertThat(failureMessages.get(0), containsString("stage nonExisting is not a valid stage, valid stages are"));
                });
    }

    private List<String> extractFailureCauseMessages(AssertableApplicationContext context) {
        var failureCauseMessages = new ArrayList<String>();
        var currentCause = context.getStartupFailure().getCause();
        while (!Objects.isNull(currentCause)) {//7
            failureCauseMessages.add(currentCause.getMessage());
            currentCause = currentCause.getCause();
        }
        return failureCauseMessages;
    }


}