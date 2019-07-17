package com.github.eugenemsv.amqp.rabbit;

import com.github.eugenemsv.amqp.rabbit.properties.AmqpProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AmqpProperties.class)
@ConditionalOnProperty(name = AmqpAutoConfigurationConstants.Property.MANAGEMENT_ENABLED, havingValue = "true", matchIfMissing = true)
@AutoConfigureBefore(RabbitAutoConfiguration.class)
public class AmqpMessageConverterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    @ConditionalOnProperty(name = "queue.rabbit.management.custom-object-mapper", havingValue = "false", matchIfMissing = true)
    public ObjectMapper amqpDefaultObjectMapper() {
        return new ObjectMapper();
    }

    /**
     * Json converter for {@link RabbitTemplate} internal usage, with existing {@link ObjectMapper} usage
     *
     * @param objectMapper existing {@link ObjectMapper}
     * @return converter
     */
    @Bean
    @ConditionalOnBean(ObjectMapper.class)
    @ConditionalOnMissingBean(MessageConverter.class)
    public MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
