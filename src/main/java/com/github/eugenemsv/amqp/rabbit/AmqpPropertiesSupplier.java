package com.github.eugenemsv.amqp.rabbit;

import com.github.eugenemsv.amqp.rabbit.properties.*;
import com.github.eugenemsv.amqp.rabbit.utils.ValidationUtils;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.github.eugenemsv.amqp.rabbit.AmqpAutoConfigurationConstants.Property.*;

public class AmqpPropertiesSupplier {

    private final String springApplicationName;

    private final AmqpProperties rootProperties;

    private final Map<String, AmqpConnectionProperties> connectionPropertiesMap;

    private final String factoryBeanName;

    private final String beanDefinitionCustomizerCanonicalClassName;

    public AmqpPropertiesSupplier(Environment environment) {
        this.springApplicationName = environment.getProperty(APPLICATION_NAME, APPLICATION_NAME_DEFAULT);
        this.rootProperties = initAmqpProperties(environment);
        this.connectionPropertiesMap = initConnectionProperties(this.rootProperties, environment);
        this.factoryBeanName = environment.getProperty(FACTORY_BEAN_NAME, FACTORY_BEAN_NAME_DEFAULT);
        this.beanDefinitionCustomizerCanonicalClassName = environment.getProperty(
                BEAN_DEFINITION_CUSTOMIZER, BEAN_DEFINITION_CUSTOMIZER_DEFAULT);
    }

    private AmqpProperties initAmqpProperties(Environment environment) {
        AmqpProperties amqpProperties = Binder.get(environment)
                .bind(ROOT_PREFIX, AmqpProperties.class)
                .orElseThrow(() -> new IllegalAmqpEnvironmentException(
                        "Not found required environment rootProperties under %s",
                        ROOT_PREFIX));
        amqpProperties.validate();
        return amqpProperties;
    }

    private Map<String, AmqpConnectionProperties> initConnectionProperties(AmqpProperties properties, Environment env) {
        return properties.getConfigurations().entrySet().stream()
                .filter(Objects::nonNull)
                .filter(entry -> !StringUtils.isEmpty(entry.getKey()) && entry.getValue() != null)
                .map(entry -> {
                    AmqpConfigurationProperties value = entry.getValue();
                    String prefix = value.getConnectionPrefix();
                    AmqpConnectionProperties connectionProperties = new AmqpConnectionProperties(
                            env.getRequiredProperty(prefix + HOST),
                            Integer.parseInt(env.getRequiredProperty(prefix + PORT)),
                            env.getRequiredProperty(prefix + USER),
                            env.getRequiredProperty(prefix + PASSWORD),
                            env.getRequiredProperty(prefix + VHOST));
                    connectionProperties.setConnectionKey(entry.getKey());
                    return connectionProperties;
                })
                .collect(Collectors.toMap(AmqpConnectionProperties::getConnectionKey, item -> item));
    }

    public String getSpringApplicationName() {
        return springApplicationName;
    }

    public AmqpProperties getRootProperties() {
        return rootProperties;
    }

    public Map<String, AmqpConnectionProperties> getConnectionPropertiesMap() {
        return connectionPropertiesMap;
    }

    public AmqpConnectionProperties getConnectionProperties(String connectionName) {
        return connectionPropertiesMap.get(connectionName);
    }

    public AmqpConnectionProperties getConnectionPropertiesFailFast(String connectionName) {
        AmqpConnectionProperties connectionProperties = connectionPropertiesMap.get(connectionName);
        ValidationUtils.notNullEnv(connectionProperties, "Not found AmqpConnectionProperties for connectionName=\'%s\'",
                connectionName);
        return connectionProperties;
    }

    public AmqpConfigurationProperties getConfigurationProperties(String connectionName) {
        return rootProperties.getConfigurations().get(connectionName);
    }

    public AmqpConfigurationProperties getConfigurationPropertiesFailFast(String connectionName) {
        AmqpConfigurationProperties configProperties = rootProperties.getConfigurations().get(connectionName);
        ValidationUtils.notNullEnv(configProperties, "Not found AmqpConfigurationProperties for connectionName=\'%s\'",
                connectionName);
        return configProperties;
    }

    public AmqpQueueProperties getQueueProperties(String connectionName, String queueKey) {
        return getConfigurationPropertiesFailFast(connectionName).getQueues().get(queueKey);
    }

    public AmqpQueueProperties getQueuePropertiesFailFast(String connectionName, String queueKey) {
        AmqpQueueProperties queueProperties = getConfigurationPropertiesFailFast(connectionName)
                .getQueues()
                .get(queueKey);
        ValidationUtils.notNullEnv(queueProperties,
                "Not found AmqpQueueProperties for connectionName=\'%s\' queueKey=\'%s\'", connectionName, queueKey);
        return queueProperties;
    }

    public AmqpTopicExchangeProperties getTopicExchangeProperties(String connectionName, String topicExchangeKey) {
        return getConfigurationPropertiesFailFast(connectionName).getTopicExchanges().get(topicExchangeKey);
    }

    public AmqpTopicExchangeProperties getTopicExchangePropertiesFailFast(String connectionName,
                                                                          String topicExchangeKey) {
        AmqpTopicExchangeProperties topicExchangeProperties = getConfigurationPropertiesFailFast(connectionName)
                .getTopicExchanges().get(topicExchangeKey);
        ValidationUtils.notNullEnv(topicExchangeProperties,
                "Not found AmqpTopicExchangeProperties for connectionName=\'%s\' topicExchangeKey=\'%s\'",
                connectionName, topicExchangeKey);
        return topicExchangeProperties;
    }

    public AmqpBindingProperties getBindingProperties(String connectionName, String queueKey, String topicExchangeKey) {
        return getConfigurationPropertiesFailFast(connectionName).getBindings().get(queueKey).get(topicExchangeKey);
    }

    public AmqpBindingProperties getBindingPropertiesFailFast(String connectionName, String queueKey,
                                                              String topicExchangeKey) {
        AmqpBindingProperties bindingProperties = getConfigurationPropertiesFailFast(connectionName).getBindings()
                .get(queueKey).get(topicExchangeKey);
        ValidationUtils.notNullEnv(bindingProperties,
                "Not found AmqpBindingProperties for connectionName=\'%s\' queueKey=\'%s\' topicExchangeKey=\'%s\'",
                connectionName, queueKey, topicExchangeKey);
        return bindingProperties;
    }

    public String getFactoryBeanName() {
        return factoryBeanName;
    }

    public String getBeanDefinitionCustomizerCanonicalClassName() {
        return beanDefinitionCustomizerCanonicalClassName;
    }
}
