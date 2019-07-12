package com.demo.amqp;

import com.demo.amqp.properties.*;
import com.demo.amqp.utils.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;


public class AmqpBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private static final Logger logger = LoggerFactory.getLogger(AmqpBeanDefinitionRegistrar.class);

    protected AmqpPropertiesSupplier propertiesSupplier;

    private BeanDefinitionRegistry registry;

    protected void registerAmqpBeanDefinitions(Map.Entry<String, AmqpConfigurationProperties> configEntry) {
        String connectionName = configEntry.getKey();
        ValidationUtils.notNullEnv(connectionName,
                "Can't apply register amqp bean definitions with empty connectionName");

        AmqpConfigurationProperties configProperties = configEntry.getValue();
        ValidationUtils.notNullEnv(configProperties,
                "Can't register amqp bean definitions with null AmqpConfigurationProperties for connectionName=\'%s\'",
                connectionName);
        registerConnectionFactoryDefinition(connectionName);
        registerRabbitListenerContainerFactoryDefinition(connectionName);
        registerRabbitAdminDefinition(connectionName);
        registerRabbitTemplateDefinition(connectionName);
        registerDeadLetterQueuesDefinition(connectionName);
        registerQueuesDefinition(connectionName);
        registerTopicExchangesDefinition(connectionName);
        registerBindingsDefinition(connectionName);
    }

    protected void registerRabbitListenerContainerFactoryDefinition(String connectionName) {
        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(ConsumerSpecificRabbitListenerContainerFactory.class)
                .setScope(BeanDefinition.SCOPE_SINGLETON)
                .addConstructorArgValue(connectionName)
                .setFactoryMethodOnBean(AmqpBeanNameResolver.getRabbitListenerContainerFactoryFactoryMethodName(),
                        propertiesSupplier.getFactoryBeanName())
                .getBeanDefinition();
        registerBeanDefinition(AmqpBeanNameResolver.getRabbitListenerContainerFactoryBeanName(connectionName),
                beanDefinition);
    }

    protected void registerConnectionFactoryDefinition(String connectionName) {
        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(CachingConnectionFactory.class)
                .setScope(BeanDefinition.SCOPE_SINGLETON)
                .addConstructorArgValue(connectionName)
                .setFactoryMethodOnBean(AmqpBeanNameResolver.getConnectionFactoryFactoryMethodName(),
                        propertiesSupplier.getFactoryBeanName())
                .getBeanDefinition();
        registerBeanDefinition(AmqpBeanNameResolver.getConnectionFactoryBeanName(connectionName), beanDefinition);
    }

    protected void registerRabbitAdminDefinition(String connectionName) {
        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(RabbitAdmin.class)
                .setScope(BeanDefinition.SCOPE_SINGLETON)
                .addConstructorArgValue(connectionName)
                .setFactoryMethodOnBean(AmqpBeanNameResolver.getRabbitAdminFactoryMethodName(),
                        propertiesSupplier.getFactoryBeanName())
                .getBeanDefinition();
        registerBeanDefinition(AmqpBeanNameResolver.getRabbitAdminBeanName(connectionName), beanDefinition);
    }

    protected void registerRabbitTemplateDefinition(String connectionName) {
        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(RabbitTemplate.class)
                .setScope(BeanDefinition.SCOPE_SINGLETON)
                .addConstructorArgValue(connectionName)
                .setFactoryMethodOnBean(AmqpBeanNameResolver.getRabbitTemplateFactoryMethodName(),
                        propertiesSupplier.getFactoryBeanName())
                .getBeanDefinition();
        registerBeanDefinition(AmqpBeanNameResolver.getRabbitTemplateBeanName(connectionName), beanDefinition);
    }

    protected void registerDeadLetterQueuesDefinition(String connectionName) {
        Map<String, AmqpQueueProperties> queuePropertiesMap = propertiesSupplier
                .getConfigurationProperties(connectionName).getQueues();
        ValidationUtils.notEmptyEnv(queuePropertiesMap,
                "Can't apply AmqpAutoConfiguration with empty queue properties for connectionName=\'%s\'",
                connectionName);
        long count = queuePropertiesMap.entrySet().stream()
                .filter(Objects::nonNull)
                .filter(entry -> isEntryWithoutNulls(entry) && entry.getValue().isWithDeadLetter())
                .map(Map.Entry::getKey)
                .peek(queueKey -> registerDeadLetterQueueDefinition(connectionName, queueKey))
                .count();
        logger.debug("Registered {} dead letter queue definitions for connectionName=\'{}\'", count, connectionName);
    }

    protected void registerDeadLetterQueueDefinition(String connectionName, String queueKey) {
        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(Queue.class)
                .setScope(BeanDefinition.SCOPE_SINGLETON)
                .addConstructorArgValue(connectionName)
                .addConstructorArgValue(queueKey)
                .setFactoryMethodOnBean(AmqpBeanNameResolver.getDeadLetterQueueFactoryMethodName(),
                        propertiesSupplier.getFactoryBeanName())
                .getBeanDefinition();
        registerBeanDefinition(AmqpBeanNameResolver.getDeadLetterQueueBeanName(connectionName, queueKey),
                beanDefinition);
    }

    protected void registerQueuesDefinition(String connectionName) {
        Map<String, AmqpQueueProperties> queuePropertiesMap = propertiesSupplier
                .getConfigurationProperties(connectionName).getQueues();
        ValidationUtils.notEmptyEnv(queuePropertiesMap,
                "Can't apply AmqpAutoConfiguration with empty queue properties for connectionName=\'%s\', you should declare at least one queue",
                connectionName);
        long count = queuePropertiesMap.entrySet()
                .stream()
                .filter(Objects::nonNull)
                .filter(this::isEntryWithoutNulls)
                .map(Map.Entry::getKey)
                .peek(queueKey -> registerQueueDefinition(connectionName, queueKey))
                .count();
        logger.debug("Registered {} queue bean definitions for connectionName=\'{}\'", count, connectionName);
    }

    protected void registerQueueDefinition(String connectionName, String queueKey) {
        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(Queue.class)
                .setScope(BeanDefinition.SCOPE_SINGLETON)
                .addConstructorArgValue(connectionName)
                .addConstructorArgValue(queueKey)
                .setFactoryMethodOnBean(AmqpBeanNameResolver.getQueueFactoryMethodName(),
                        propertiesSupplier.getFactoryBeanName())
                .getBeanDefinition();
        registerBeanDefinition(AmqpBeanNameResolver.getQueueBeanName(connectionName, queueKey),
                beanDefinition);
    }

    protected void registerTopicExchangesDefinition(String connectionName) {
        Map<String, AmqpTopicExchangeProperties> topicExchangePropertiesMap = propertiesSupplier
                .getConfigurationProperties(connectionName).getTopicExchanges();
        if (topicExchangePropertiesMap == null || topicExchangePropertiesMap.isEmpty()) {
            return;
        }

        long count = topicExchangePropertiesMap.entrySet()
                .stream()
                .filter(Objects::nonNull)
                .filter(this::isEntryWithoutNulls)
                .map(Map.Entry::getKey)
                .peek(topicExchangeKey -> registerTopicExchangeDefinition(connectionName, topicExchangeKey))
                .count();
        logger.debug("Registered {} topic exchange definitions for connectionName=\'{}\'", count, connectionName);
    }

    protected void registerTopicExchangeDefinition(String connectionName, String topicExchangeKey) {
        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(TopicExchange.class)
                .setScope(BeanDefinition.SCOPE_SINGLETON)
                .addConstructorArgValue(connectionName)
                .addConstructorArgValue(topicExchangeKey)
                .setFactoryMethodOnBean(AmqpBeanNameResolver.getTopicExchangeFactoryMethodName(),
                        propertiesSupplier.getFactoryBeanName())
                .getBeanDefinition();
        registerBeanDefinition(AmqpBeanNameResolver.getTopicExchangeBeanName(connectionName, topicExchangeKey),
                beanDefinition);
    }

    protected void registerBindingsDefinition(String connectionName) {
        Map<String, Map<String, AmqpBindingProperties>> bindingsMap = propertiesSupplier
                .getConfigurationProperties(connectionName).getBindings();

        if (bindingsMap == null || bindingsMap.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Map<String, AmqpBindingProperties>> bindingEntry : bindingsMap.entrySet()) {
            final String queueKey = bindingEntry.getKey();
            for (Map.Entry<String, AmqpBindingProperties> topicExchangeEntry : bindingEntry.getValue().entrySet()) {
                final String topicExchangeKey = topicExchangeEntry.getKey();
                final AmqpBindingProperties bindingsConfig = topicExchangeEntry.getValue();

                ValidationUtils.notEmptyEnv(bindingsConfig.getRoutingKeys(),
                        "Routing key patterns for binding queue key=\'%s\' and topic exchange key=\'%s\' not found",
                        queueKey, topicExchangeKey);
                for (Map.Entry<String, String> routingEntry : bindingsConfig.getRoutingKeys().entrySet()) {
                    registerBindingDefinition(connectionName, queueKey, topicExchangeKey, routingEntry.getKey());
                }
            }
        }
    }

    protected void registerBindingDefinition(String connectionName, String queueKey,
                                             String topicExchangeKey, String routingKey) {
        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(Binding.class)
                .setScope(BeanDefinition.SCOPE_SINGLETON)
                .addConstructorArgValue(connectionName)
                .addConstructorArgValue(queueKey)
                .addConstructorArgValue(topicExchangeKey)
                .addConstructorArgValue(routingKey)
                .setFactoryMethodOnBean(AmqpBeanNameResolver.getBindingFactoryMethodName(),
                        propertiesSupplier.getFactoryBeanName())
                .getBeanDefinition();
        registerBeanDefinition(
                AmqpBeanNameResolver.getBindingBeanName(connectionName, queueKey, topicExchangeKey, routingKey),
                beanDefinition);
    }

    private boolean isEntryWithoutNulls(Map.Entry<String, ?> entry) {
        return !StringUtils.isEmpty(entry.getKey())
                && entry.getValue() != null;
    }

    protected void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        this.registry.registerBeanDefinition(beanName, beanDefinition);
        logger.debug("Registered bean definition for =\'{}\', type={}", beanName, beanDefinition.getBeanClassName());
    }

    @Override
    public void setEnvironment(Environment environment) {
        System.out.println("AmqpBeanDefinitionRegistrar.setEnvironment");
        this.propertiesSupplier = new AmqpPropertiesSupplier(environment);
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        System.out.println("AmqpBeanDefinitionRegistrar.registerBeanDefinitions");
        this.registry = registry;
        AmqpProperties rootProperties = propertiesSupplier.getRootProperties();
        for (Map.Entry<String, AmqpConfigurationProperties> configEntry : rootProperties.getConfigurations()
                .entrySet()) {
            registerAmqpBeanDefinitions(configEntry);
        }
    }
}
