package com.amqp.rabbit.bean;

import com.amqp.rabbit.AmqpAutoConfigurationConstants;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.amqp.rabbit.AmqpAutoConfigurationConstants.Bean.*;

public class AmqpBeanNameResolver {

    private AmqpBeanNameResolver() {
        throw new UnsupportedOperationException(AmqpAutoConfigurationConstants.CONSTRUCTOR_CALL_NOT_ALLOWED);
    }

    private static String concat(String... parts) {
        return Stream.of(parts)
                .filter(Objects::nonNull)
                .collect(Collectors.joining());
    }

    public static String getConnectionFactoryBeanName(String connectionName) {
        return concat(connectionName, CONNECTION_FACTORY_SUFFIX);
    }

    public static String getConnectionFactoryFactoryMethodName() {
        return concat(getFactoryMethodPrefix(), CONNECTION_FACTORY_SUFFIX);
    }

    private static String getFactoryMethodPrefix() {
        return FACTORY_METHOD_PREFIX;
    }

    public static String getRabbitListenerContainerFactoryBeanName(String connectionName) {
        return concat(connectionName, RABBIT_LISTENER_CONTAINER_FACTORY_SUFFIX);
    }

    public static String getRabbitListenerContainerFactoryFactoryMethodName() {
        return concat(getFactoryMethodPrefix(), RABBIT_LISTENER_CONTAINER_FACTORY_SUFFIX);
    }

    public static String getRabbitAdminBeanName(String connectionName) {
        return concat(connectionName, RABBIT_ADMIN_SUFFIX);
    }

    public static String getRabbitAdminFactoryMethodName() {
        return concat(getFactoryMethodPrefix(), RABBIT_ADMIN_SUFFIX);
    }

    public static String getRabbitTemplateBeanName(String connectionName) {
        return concat(connectionName, RABBIT_TEMPLATE_SUFFIX);
    }

    public static String getRabbitTemplateFactoryMethodName() {
        return concat(getFactoryMethodPrefix(), RABBIT_TEMPLATE_SUFFIX);
    }

    public static String getDeadLetterQueueBeanName(String connectionName, String queueKey) {
        return concat(connectionName, queueKey, QUEUE_DEAD_LETTER_SUFFIX);
    }

    public static String getDeadLetterQueueFactoryMethodName() {
        return concat(getFactoryMethodPrefix(), QUEUE_DEAD_LETTER_SUFFIX);
    }

    public static String getQueueBeanName(String connectionName, String queueKey) {
        return concat(connectionName, queueKey, QUEUE_SUFFIX);
    }

    public static String getQueueFactoryMethodName() {
        return concat(getFactoryMethodPrefix(), QUEUE_SUFFIX);
    }

    public static String getTopicExchangeBeanName(String connectionName, String topicExchangeKey) {
        return concat(connectionName, topicExchangeKey, TOPIC_EXCHANGE_SUFFIX);
    }

    public static String getTopicExchangeFactoryMethodName() {
        return concat(getFactoryMethodPrefix(), TOPIC_EXCHANGE_SUFFIX);
    }

    public static String getBindingBeanName(String connectionName, String queueKey,
                                            String topicExchangeKey, String routingKey) {
        return concat(connectionName, queueKey, topicExchangeKey, routingKey, BINDING_SUFFIX);
    }

    public static String getBindingFactoryMethodName() {
        return concat(getFactoryMethodPrefix(), BINDING_SUFFIX);
    }

}
