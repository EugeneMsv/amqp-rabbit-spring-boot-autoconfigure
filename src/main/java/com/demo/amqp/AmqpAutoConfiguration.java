package com.demo.amqp;

import com.demo.amqp.bean.AmqpBeanDefinitionRegistrar;
import com.demo.amqp.bean.AmqpBeansFactory;
import com.demo.amqp.bean.DefaultAmqpBeansFactory;
import com.demo.amqp.properties.AmqpProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurationSelector;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableConfigurationProperties(AmqpProperties.class)
@ConditionalOnProperty(name = AmqpAutoConfigurationConstants.Property.MANAGEMENT_ENABLED, havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(AmqpMessageConverterAutoConfiguration.class)
@AutoConfigureBefore(RabbitAutoConfiguration.class)
@Import({AmqpBeanDefinitionRegistrar.class, RabbitListenerConfigurationSelector.class})
public class AmqpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AmqpBeansFactory.class)
    public AmqpBeansFactory amqpBeansFactory(Environment environment, ApplicationContext applicationContext,
                                             @Autowired(required = false) MessageConverter messageConverter) {
        DefaultAmqpBeansFactory defaultAmqpBeansFactory = new DefaultAmqpBeansFactory(environment, applicationContext);
        defaultAmqpBeansFactory.setMessageConverter(messageConverter);
        return defaultAmqpBeansFactory;
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    @ConditionalOnMissingBean(name = "messagingThreadPoolTaskExecutor")
    public ThreadPoolTaskExecutor messagingThreadPoolTaskExecutor(int corePoolSize, int maxPoolSize) {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(corePoolSize);
        threadPoolTaskExecutor.setMaxPoolSize(maxPoolSize);
        // Require increase pool size as fast as possible
        threadPoolTaskExecutor.setQueueCapacity(1);
        return threadPoolTaskExecutor;
    }
}
