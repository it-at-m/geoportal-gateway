package de.swm.lhm.geoportal.gateway.shared;

import de.swm.lhm.geoportal.gateway.base_classes.BaseIntegrationTest;
import org.junit.experimental.runners.Enclosed;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;


@RunWith(Enclosed.class)
class IsStageConfigurationConditionTest {

    @Nested
    @TestPropertySource(properties = "geoportal.gateway.stage=config")
    @Import(ConfigStageConditionTestConfig.class)
    class IsStageConfigurationConditionEnabled1Test extends BaseIntegrationTest {

        @Autowired
        @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
        private ConfigStageConditionTestConfig.TestBean testBean;

        @Test
        void qsTestBeanShouldBeEnabled() {
            assertThat(testBean, instanceOf(ConfigStageConditionTestConfig.TestBeanConfig.class));
        }
    }

    @Nested
    @TestPropertySource(properties = "geoportal.gateway.stage=CONFIGURATION")
    @Import(ConfigStageConditionTestConfig.class)
    class IsStageConfigurationConditionEnabled2Test extends BaseIntegrationTest {

        @Autowired
        @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
        private ConfigStageConditionTestConfig.TestBean testBean;

        @Test
        void qsTestBeanShouldBeEnabled() {
            assertThat(testBean, instanceOf(ConfigStageConditionTestConfig.TestBeanConfig.class));
        }
    }

    @Nested
    @RunWith(SpringRunner.class)
    @SpringBootTest
    @TestPropertySource(properties = "geoportal.gateway.stage=qs")
    @Import(ConfigStageConditionTestConfig.class)
    class IsStageConfigurationConditionDisabledTest extends BaseIntegrationTest {

        @Autowired
        private ConfigStageConditionTestConfig.TestBean testBean;

        @Test
        void configTestBeanShouldNotBeEnabled() {
            assertThat(testBean, instanceOf(ConfigStageConditionTestConfig.TestBeanQs.class));
        }
    }

}

