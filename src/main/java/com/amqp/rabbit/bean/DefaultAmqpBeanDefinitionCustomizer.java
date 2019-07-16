package com.amqp.rabbit.bean;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class DefaultAmqpBeanDefinitionCustomizer implements AmqpBeanDefinitionCustomizer {

    @Override
    public Class<?> getConnectionFactoryClass() {
        return CachingConnectionFactory.class;
    }

    @Override
    public Class<?> getRabbitListenerContainerFactoryClass() {
        return ConsumerSpecificRabbitListenerContainerFactory.class;
    }

    @Override
    public Class<?> getRabbitTemplateClass() {
        return RabbitTemplate.class;
    }

    @Override
    public Class<?> getRabbitAdminClass() {
        return RabbitAdmin.class;
    }
}
