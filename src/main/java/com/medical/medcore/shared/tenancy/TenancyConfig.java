package com.medical.medcore.shared.tenancy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;

@Configuration
public class TenancyConfig {

    private static final String PRIMARY_DATA_SOURCE = "dataSource";

    @Bean
    public static BeanPostProcessor tenantAwareDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (PRIMARY_DATA_SOURCE.equals(beanName) && bean instanceof DataSource dataSource
                        && !(bean instanceof TenantAwareDataSource)) {
                    return new TenantAwareDataSource(dataSource);
                }
                return bean;
            }
        };
    }

    @Bean
    public TenantHostResolver tenantHostResolver(
            @Value("${app.tenancy.base-domains:pemedcore.xyz,localhost}") List<String> baseDomains,
            @Value("${app.tenancy.allow-header:false}") boolean headerAllowed) {
        return new TenantHostResolver(baseDomains, headerAllowed);
    }
}
