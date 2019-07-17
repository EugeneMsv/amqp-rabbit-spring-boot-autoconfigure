package com.github.eugene.msv.amqp.rabbit.bean;

import com.github.eugene.msv.amqp.rabbit.AmqpPropertiesSupplier;
import com.github.eugene.msv.amqp.rabbit.IllegalAmqpEnvironmentException;
import com.github.eugene.msv.amqp.rabbit.utils.ValidationUtils;
import com.github.eugene.msv.amqp.rabbit.properties.AmqpBindingProperties;
import com.github.eugene.msv.amqp.rabbit.properties.AmqpProperties;
import com.github.eugene.msv.amqp.rabbit.properties.AmqpQueueProperties;
import com.github.eugene.msv.amqp.rabbit.properties.AmqpTopicExchangeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Objects;


public class AmqpBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private static final Logger logger = LoggerFactory.getLogger(AmqpBeanDefinitionRegistrar.class);

    protected AmqpPropertiesSupplier propertiesSupplier;

    protected AmqpBeanDefinitionCustomizer definitionCustomizer;

    private BeanDefinitionRegistry registry;

    protected void registerAmqpBeanDefinitions(String connectionName) {
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
                .genericBeanDefinition(definitionCustomizer.getRabbitListenerContainerFactoryClass())
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
                .genericBeanDefinition(definitionCustomizer.getConnectionFactoryClass())
                .setScope(BeanDefinition.SCOPE_SINGLETON)
                .addConstructorArgValue(connectionName)
                .setFactoryMethodOnBean(AmqpBeanNameResolver.getConnectionFactoryFactoryMethodName(),
                        propertiesSupplier.getFactoryBeanName())
                .getBeanDefinition();
        registerBeanDefinition(AmqpBeanNameResolver.getConnectionFactoryBeanName(connectionName), beanDefinition);
    }

    protected void registerRabbitAdminDefinition(String connectionName) {
        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(definitionCustomizer.getRabbitAdminClass())
                .setScope(BeanDefinition.SCOPE_SINGLETON)
                .addConstructorArgValue(connectionName)
                .setFactoryMethodOnBean(AmqpBeanNameResolver.getRabbitAdminFactoryMethodName(),
                        propertiesSupplier.getFactoryBeanName())
                .getBeanDefinition();
        registerBeanDefinition(AmqpBeanNameResolver.getRabbitAdminBeanName(connectionName), beanDefinition);
    }

    protected void registerRabbitTemplateDefinition(String connectionName) {
        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(definitionCustomizer.getRabbitTemplateClass())
                .setScope(BeanDefinition.SCOPE_SINGLETON)
                .addConstructorArgValue(connectionName)
                .setFactoryMethodOnBean(AmqpBeanNameResolver.getRabbitTemplateFactoryMethodName(),
                        propertiesSupplier.getFactoryBeanName())
                .getBeanDefinition();
        registerBeanDefinition(AmqpBeanNameResolver.getRabbitTemplateBeanName(connectionName), beanDefinition);
    }

    protected void registerDeadLetterQueuesDefinition(String connectionName) {
        Map<String, AmqpQueueProperties> queuePropertiesMap = propertiesSupplier
                .getConfigurationPropertiesFailFast(connectionName).getQueues();
        if (queuePropertiesMap == null || queuePropertiesMap.isEmpty()) {
            return;
        }

        queuePropertiesMap.entrySet().stream()
                .filter(Objects::nonNull)
                .filter(entry -> isEntryWithoutNulls(entry) && entry.getValue().isWithDeadLetter())
                .map(Map.Entry::getKey)
                .forEach(queueKey -> registerDeadLetterQueueDefinition(connectionName, queueKey));
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
                .getConfigurationPropertiesFailFast(connectionName).getQueues();
        if (queuePropertiesMap == null || queuePropertiesMap.isEmpty()) {
            return;
        }

        queuePropertiesMap.entrySet()
                .stream()
                .filter(Objects::nonNull)
                .filter(this::isEntryWithoutNulls)
                .map(Map.Entry::getKey)
                .forEach(queueKey -> registerQueueDefinition(connectionName, queueKey));
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
                .getConfigurationPropertiesFailFast(connectionName).getTopicExchanges();
        if (topicExchangePropertiesMap == null || topicExchangePropertiesMap.isEmpty()) {
            return;
        }

        topicExchangePropertiesMap.entrySet()
                .stream()
                .filter(Objects::nonNull)
                .filter(this::isEntryWithoutNulls)
                .map(Map.Entry::getKey)
                .forEach(topicExchangeKey -> registerTopicExchangeDefinition(connectionName, topicExchangeKey));
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
                .getConfigurationPropertiesFailFast(connectionName).getBindings();

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
        this.propertiesSupplier = new AmqpPropertiesSupplier(environment);
        String definitionCustomizerClass = this.propertiesSupplier
                .getBeanDefinitionCustomizerCanonicalClassName();
        try {
            Constructor<?> constructor = ReflectionUtils
                    .accessibleConstructor(Class.forName(definitionCustomizerClass));
            this.definitionCustomizer = (AmqpBeanDefinitionCustomizer) constructor.newInstance();
        } catch (Exception e) {
            throw new IllegalAmqpEnvironmentException(
                    "Unable to initiate AmqpBeanDefinitionCustomizer instance for class=%s. "
                            + "It should implement AmqpBeanDefinitionCustomizer and has a no-args constructor.",
                    definitionCustomizerClass);
        }
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        this.registry = registry;
        AmqpProperties rootProperties = propertiesSupplier.getRootProperties();
        for (String connectionName : rootProperties.getConfigurations().keySet()) {
            registerAmqpBeanDefinitions(connectionName);
        }
    }
}
