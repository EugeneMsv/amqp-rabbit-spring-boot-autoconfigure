package com.demo.amqp.bean;

/**
 * Amqp Bean definition customizer.
 * You can define custom implementation and set full class name to the property
 * {@link com.demo.amqp.AmqpAutoConfigurationConstants.Property#BEAN_DEFINITION_CUSTOMIZER}.
 * Take care about no-args constructor in your custom implementation.
 */
public interface AmqpBeanDefinitionCustomizer {

    Class<?> getConnectionFactoryClass();

    Class<?> getRabbitListenerContainerFactoryClass();

    Class<?> getRabbitTemplateClass();

    Class<?> getRabbitAdminClass();
}
