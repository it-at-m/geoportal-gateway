package de.swm.lhm.geoportal.gateway.shared;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

class ConfigStageConditionTestConfig {

    @Bean
    @Conditional(IsStageConfigurationCondition.class)
    public TestBean configStageCondition() {
        return new TestBeanConfig();
    }

    @Bean
    @ConditionalOnMissingBean(TestBean.class)
    public TestBean qsStageCondition() {
        return new TestBeanQs();
    }

    public interface TestBean{}
    public static class TestBeanConfig implements TestBean {}
    public static class TestBeanQs implements TestBean {}

}

