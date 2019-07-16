package com.amqp.rabbit.bean;

import com.amqp.rabbit.AmqpAutoConfigurationConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;

/**
 * Factory for supplying bean instances. Each method can be called more then one time (depends on configuration properties).
 * You can provide custom implementation, but in that case you also should provide bean name of your factory via
 * {@link AmqpAutoConfigurationConstants.Property#FACTORY_BEAN_NAME} property
 */
public interface AmqpBeansFactory {

    ConnectionFactory supplyConnectionFactory(String connectionName);

    RabbitListenerContainerFactory supplyRabbitListenerContainerFactory(String connectionName);

    RabbitAdmin supplyRabbitAdmin(String connectionName);

    RabbitTemplate supplyRabbitTemplate(String connectionName);

    Queue supplyDeadLetterQueue(String connectionName, String queueKey);

    Queue supplyQueue(String connectionName, String queueKey);

    TopicExchange supplyTopicExchange(String connectionName, String topicExchangeKey);

    Binding supplyBinding(String connectionName, String queueKey, String topicExchangeKey, String routingKey);
}
