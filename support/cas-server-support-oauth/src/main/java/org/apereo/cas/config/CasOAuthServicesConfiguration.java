package org.apereo.cas.config;

import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.authentication.principal.ServiceFactory;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.services.DenyAllAttributeReleasePolicy;
import org.apereo.cas.services.RegexRegisteredService;
import org.apereo.cas.services.ServiceRegistryExecutionPlan;
import org.apereo.cas.services.ServiceRegistryExecutionPlanConfigurer;
import org.apereo.cas.support.oauth.OAuth20Constants;
import org.apereo.cas.support.oauth.services.OAuth20ServiceRegistry;
import org.apereo.cas.util.RandomUtils;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * This is {@link CasOAuthServicesConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 6.1.0
 */
@Configuration("oauthServicesConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Slf4j
public class CasOAuthServicesConfiguration {
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired
    @Qualifier("webApplicationServiceFactory")
    private ObjectProvider<ServiceFactory> webApplicationServiceFactory;

    @Bean
    public Service oauthCallbackService() {
        val oAuthCallbackUrl = casProperties.getServer().getPrefix()
            + OAuth20Constants.BASE_OAUTH20_URL + '/' + OAuth20Constants.CALLBACK_AUTHORIZE_URL_DEFINITION;
        return webApplicationServiceFactory.getIfAvailable().createService(oAuthCallbackUrl);
    }

    @Bean
    @ConditionalOnMissingBean(name = "oauthServiceRegistryExecutionPlanConfigurer")
    public ServiceRegistryExecutionPlanConfigurer oauthServiceRegistryExecutionPlanConfigurer() {
        return new ServiceRegistryExecutionPlanConfigurer() {
            @Override
            public void configureServiceRegistry(final ServiceRegistryExecutionPlan plan) {
                val service = new RegexRegisteredService();
                service.setId(RandomUtils.getNativeInstance().nextLong());
                service.setEvaluationOrder(Ordered.HIGHEST_PRECEDENCE);
                service.setName(service.getClass().getSimpleName());
                service.setDescription("OAuth Authentication Callback Request URL");
                service.setServiceId(oauthCallbackService().getId());
                service.setAttributeReleasePolicy(new DenyAllAttributeReleasePolicy());
                plan.registerServiceRegistry(new OAuth20ServiceRegistry(eventPublisher, service));
            }
        };
    }

}
