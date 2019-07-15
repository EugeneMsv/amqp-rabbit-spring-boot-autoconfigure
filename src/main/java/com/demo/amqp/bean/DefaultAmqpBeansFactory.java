package com.demo.amqp.bean;

import com.demo.amqp.AmqpPropertiesSupplier;
import com.demo.amqp.properties.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.demo.amqp.AmqpAutoConfigurationConstants.Queue.*;

public class DefaultAmqpBeansFactory implements AmqpBeansFactory {

    private static final Logger logger = LoggerFactory.getLogger(DefaultAmqpBeansFactory.class);

    private final AmqpPropertiesSupplier propertiesSupplier;

    private final ApplicationContext applicationContext;

    private Optional<MessageConverter> messageConverter = Optional.empty();

    public DefaultAmqpBeansFactory(Environment environment, ApplicationContext applicationContext) {
        this.propertiesSupplier = new AmqpPropertiesSupplier(environment);
        this.applicationContext = applicationContext;
    }

    public void setMessageConverter(MessageConverter messageConverter) {
        this.messageConverter = Optional.ofNullable(messageConverter);
    }

    private Map<String, AmqpListenerContainerProperties> collectNotEmptyConsumerProperties(
            AmqpConfigurationProperties configProperties) {
        return configProperties.getQueues()
                .entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> entry.getValue().getListener() != null)
                .collect(Collectors.toMap(
                        entry -> entry.getValue().getName(),
                        entry -> entry.getValue().getListener(), (o, n) -> o));
    }

    private RetryTemplate createRetryTemplate(AmqpRetryProperties retryProperties) {
        RetryTemplate template = new RetryTemplate();
        SimpleRetryPolicy policy = new SimpleRetryPolicy();
        policy.setMaxAttempts(retryProperties.getMaxAttempts());
        template.setRetryPolicy(policy);
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(retryProperties.getInitialInterval());
        backOffPolicy.setMultiplier(retryProperties.getMultiplier());
        backOffPolicy.setMaxInterval(retryProperties.getMaxInterval());
        template.setBackOffPolicy(backOffPolicy);
        return template;
    }

    @Override
    public ConnectionFactory supplyConnectionFactory(String connectionName) {
        AmqpConnectionProperties connectionProperties = propertiesSupplier.getConnectionPropertiesFailFast(connectionName);
        CachingConnectionFactory cf = new CachingConnectionFactory(connectionProperties.getHost(),
                connectionProperties.getPort());
        cf.setUsername(connectionProperties.getUser());
        cf.setPassword(connectionProperties.getPassword());
        cf.setVirtualHost(connectionProperties.getvHost());

        cf.setConnectionNameStrategy(connectionFactory ->
                propertiesSupplier.getSpringApplicationName() + StringUtils.capitalize(connectionName));
        logger.debug("Created CachingConnectionFactory for connectionName={}", connectionName);
        return cf;
    }

    @Override
    public RabbitListenerContainerFactory supplyRabbitListenerContainerFactory(String connectionName) {
        AmqpConfigurationProperties configProperties = propertiesSupplier.getConfigurationPropertiesFailFast(connectionName);
        ConnectionFactory connectionFactory = applicationContext.getBean(
                AmqpBeanNameResolver.getConnectionFactoryBeanName(connectionName), ConnectionFactory.class);
        ConsumerSpecificRabbitListenerContainerFactory factory = new ConsumerSpecificRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        if (configProperties.getDefaultRequeueRejected() != null) {
            factory.setDefaultRequeueRejected(configProperties.getDefaultRequeueRejected());
        }
        factory.setConcurrentConsumers(configProperties.getListener().getConcurrentConsumers());
        factory.setMaxConcurrentConsumers(configProperties.getListener().getMaxConcurrentConsumers());
        factory.setMismatchedQueuesFatal(configProperties.isMismatchedQueuesFatal());
        factory.setConsumerProperties(collectNotEmptyConsumerProperties(configProperties));
        logger.debug("Created ConsumerSpecificRabbitListenerContainerFactory for connectionName={}", connectionName);
        return factory;
    }

    @Override
    public RabbitAdmin supplyRabbitAdmin(String connectionName) {
        ConnectionFactory connectionFactory = applicationContext.getBean(
                AmqpBeanNameResolver.getConnectionFactoryBeanName(connectionName), ConnectionFactory.class);
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        logger.debug("Created RabbitAdmin for connectionName={}", connectionName);
        return rabbitAdmin;
    }

    @Override
    public RabbitTemplate supplyRabbitTemplate(String connectionName) {
        RabbitAdmin rabbitAdmin = applicationContext.getBean(
                AmqpBeanNameResolver.getRabbitAdminBeanName(connectionName), RabbitAdmin.class);
        RabbitTemplate rabbitTemplate = rabbitAdmin.getRabbitTemplate();
        messageConverter.ifPresent(rabbitTemplate::setMessageConverter);
        AmqpConfigurationProperties configurationProperties =
                propertiesSupplier.getConfigurationPropertiesFailFast(connectionName);
        AmqpRetryProperties retryProperties = configurationProperties.getRetry();
        if (retryProperties.isEnabled()) {
            rabbitTemplate.setRetryTemplate(createRetryTemplate(retryProperties));
            logger.debug("Added RetryTemplate to connection='{}', properties={}", connectionName, retryProperties);
        }
        logger.debug("Created RabbitTemplate for connectionName={}", connectionName);
        return rabbitTemplate;
    }

    @Override
    public Queue supplyDeadLetterQueue(String connectionName, String queueKey) {
        RabbitAdmin rabbitAdmin = applicationContext.getBean(
                AmqpBeanNameResolver.getRabbitAdminBeanName(connectionName), RabbitAdmin.class);
        AmqpConfigurationProperties configurationProperties = propertiesSupplier
                .getConfigurationPropertiesFailFast(connectionName);
        AmqpQueueProperties queueProperties = propertiesSupplier.getQueuePropertiesFailFast(connectionName, queueKey);
        Queue deadLetterQueue = convertPropsToDeadLetterQueue(queueProperties, configurationProperties);
        deadLetterQueue.setAdminsThatShouldDeclare(rabbitAdmin);
        logger.debug("Created dead letter Queue for connectionName={} and queueKey={}", connectionName, queueKey);
        return deadLetterQueue;
    }

    private Queue convertPropsToDeadLetterQueue(AmqpQueueProperties queueProperties,
                                                AmqpConfigurationProperties configProperties) {
        Map<String, Object> arguments = queueProperties.getArguments();
        AmqpDeadLetterProperties deadLetterConfig = queueProperties.getDeadLetterConfig();
        if (deadLetterConfig != null) {
            // Cause dead-letter creation should use original(not_modified) arguments
            arguments = new HashMap<>(arguments);

            // Setting ordinary queue like a dead letter for dead letter queue
            if (deadLetterConfig.isBidirectional()) {
                Map<String, Object> deadLetterArguments = linkWithDeadLetter(queueProperties.getName());
                arguments.putAll(deadLetterArguments);
            }
            Map<String, Object> timeToLiveArguments = linkTimeToLive(deadLetterConfig.getTimeToLive());
            arguments.putAll(timeToLiveArguments);
        }
        return new Queue(getDeadLetterQueueName(queueProperties, configProperties),
                queueProperties.isDurable(),
                queueProperties.isExclusive(),
                queueProperties.isAutoDelete(),
                arguments);
    }

    private Map<String, Object> linkWithDeadLetter(String deadLetterQueueName) {
        Map<String, Object> arguments = new HashMap<>(2, 1.1F);
        arguments.put(ARGUMENT_KEY_DEAD_LETTER_EXCHANGE, ARGUMENT_VALUE_DEAD_LETTER_EXCHANGE);
        arguments.put(ARGUMENT_KEY_DEAD_LETTER_ROUTING, deadLetterQueueName);
        return arguments;
    }

    private Map<String, Object> linkTimeToLive(Integer timeToLive) {
        return Optional.ofNullable(timeToLive)
                .filter(v -> v > 0)
                .map(v -> {
                    Map<String, Object> ttlMap = new HashMap<>(1, 1.1F);
                    ttlMap.put(ARGUMENT_KEY_MESSAGE_TTL, v);
                    return ttlMap;
                }).orElse(Collections.emptyMap());
    }

    private String getDeadLetterQueueName(AmqpQueueProperties queueProperties,
                                          AmqpConfigurationProperties configProperties) {
        return queueProperties.getName() + configProperties.getDeadLetterSuffix();
    }

    @Override
    public Queue supplyQueue(String connectionName, String queueKey) {
        RabbitAdmin rabbitAdmin = applicationContext.getBean(
                AmqpBeanNameResolver.getRabbitAdminBeanName(connectionName), RabbitAdmin.class);
        AmqpConfigurationProperties configurationProperties = propertiesSupplier
                .getConfigurationPropertiesFailFast(connectionName);
        AmqpQueueProperties queueProperties = propertiesSupplier.getQueuePropertiesFailFast(connectionName, queueKey);
        Queue queue = convertAmqpQueuePropertiesToQueue(queueProperties, configurationProperties);
        queue.setAdminsThatShouldDeclare(rabbitAdmin);
        logger.debug("Created Queue for connectionName={} and queueKey={}", connectionName, queueKey);
        return queue;
    }

    private Queue convertAmqpQueuePropertiesToQueue(AmqpQueueProperties queueProperties,
                                                    AmqpConfigurationProperties configProperties) {
        Map<String, Object> arguments = queueProperties.getArguments();
        if (queueProperties.isWithDeadLetter()) {
            Map<String, Object> deadLetterArguments = linkWithDeadLetter(
                    getDeadLetterQueueName(queueProperties, configProperties));
            // Cause dead-letter creation should use original(not_modified) arguments
            arguments = new HashMap<>(arguments);
            arguments.putAll(deadLetterArguments);
        }
        return new Queue(queueProperties.getName(), queueProperties.isDurable(),
                queueProperties.isExclusive(), queueProperties.isAutoDelete(),
                arguments);
    }

    @Override
    public TopicExchange supplyTopicExchange(String connectionName, String topicExchangeKey) {
        RabbitAdmin rabbitAdmin = applicationContext.getBean(
                AmqpBeanNameResolver.getRabbitAdminBeanName(connectionName), RabbitAdmin.class);
        AmqpTopicExchangeProperties topicExchangeProperties = propertiesSupplier
                .getTopicExchangePropertiesFailFast(connectionName, topicExchangeKey);
        TopicExchange topicExchange = convertAmqpTopicExchangePropertiesToTopicExchange(topicExchangeProperties);
        topicExchange.setAdminsThatShouldDeclare(rabbitAdmin);
        logger.debug("Created TopicExchange for connectionName={} and topicExchangeKey={}", connectionName,
                topicExchangeKey);
        return topicExchange;
    }

    private TopicExchange convertAmqpTopicExchangePropertiesToTopicExchange(AmqpTopicExchangeProperties properties) {
        return new TopicExchange(properties.getName(), properties.isDurable(),
                properties.isAutoDelete(), properties.getArguments());
    }

    @Override
    public Binding supplyBinding(String connectionName, String queueKey, String topicExchangeKey, String routingKey) {
        RabbitAdmin rabbitAdmin = applicationContext.getBean(
                AmqpBeanNameResolver.getRabbitAdminBeanName(connectionName), RabbitAdmin.class);
        Queue queue = applicationContext.getBean(
                AmqpBeanNameResolver.getQueueBeanName(connectionName, queueKey), Queue.class);
        TopicExchange topicExchange = applicationContext.getBean(
                AmqpBeanNameResolver.getTopicExchangeBeanName(connectionName, topicExchangeKey), TopicExchange.class);
        AmqpBindingProperties bindingProperties = propertiesSupplier
                .getBindingPropertiesFailFast(connectionName, queueKey, topicExchangeKey);
        String routingKeyValue = bindingProperties.getRoutingKeys().get(routingKey);
        Binding binding = BindingBuilder
                .bind(queue)
                .to(topicExchange)
                .with(routingKeyValue);
        binding.setAdminsThatShouldDeclare(rabbitAdmin);
        logger.debug("Created Binding for connectionName={}, queueKey={} topicExchangeKey={}, routingKey={}",
                connectionName, queueKey, topicExchangeKey, routingKey);
        return binding;
    }
}
