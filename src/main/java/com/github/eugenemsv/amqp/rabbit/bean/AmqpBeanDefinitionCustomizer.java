package com.github.eugenemsv.amqp.rabbit.bean;

import com.github.eugenemsv.amqp.rabbit.AmqpAutoConfigurationConstants;

/**
 * Amqp Bean definition customizer.
 * You can define custom implementation and set full class name to the property
 * {@link AmqpAutoConfigurationConstants.Property#BEAN_DEFINITION_CUSTOMIZER}.
 * Take care about no-args constructor in your custom implementation.
 */
public interface AmqpBeanDefinitionCustomizer {

    Class<?> getConnectionFactoryClass();

    Class<?> getRabbitListenerContainerFactoryClass();

    Class<?> getRabbitTemplateClass();

    Class<?> getRabbitAdminClass();
}
