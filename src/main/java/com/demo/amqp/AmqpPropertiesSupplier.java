package com.demo.amqp;

import com.demo.amqp.properties.*;
import com.demo.amqp.utils.ValidationUtils;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.demo.amqp.AmqpAutoConfigurationConstants.Property.*;

public class AmqpPropertiesSupplier {

    private final String springApplicationName;

    private final AmqpProperties rootProperties;

    private final Map<String, AmqpConnectionProperties> connectionPropertiesMap;

    private final String factoryBeanName;

    public AmqpPropertiesSupplier(Environment environment) {
        this.springApplicationName = environment.getProperty(APPLICATION_NAME, APPLICATION_NAME_DEFAULT);
        this.rootProperties = initAmqpProperties(environment);
        this.connectionPropertiesMap = initConnectionProperties(this.rootProperties, environment);
        this.factoryBeanName = environment.getProperty(FACTORY_BEAN_NAME, FACTORY_BEAN_NAME_DEFAULT);
    }

    private AmqpProperties initAmqpProperties(Environment environment) {
        return Binder.get(environment)
                .bind(ROOT_PREFIX, AmqpProperties.class)
                .orElseThrow(() -> new IllegalAmqpEnvironmentException(
                        "Not found required environment rootProperties under %s",
                        ROOT_PREFIX));
    }

    private Map<String, AmqpConnectionProperties> initConnectionProperties(AmqpProperties properties, Environment env) {
        ValidationUtils.notNullEnv(properties.getConfigurations(), "%s configurations required", ROOT_PREFIX);
        return properties.getConfigurations().entrySet().stream()
                .filter(Objects::nonNull)
                .filter(entry -> !StringUtils.isEmpty(entry.getKey()) && entry.getValue() != null)
                .map(entry -> {
                    AmqpConfigurationProperties value = entry.getValue();
                    String prefix = value.getConnectionPrefix();
                    ValidationUtils.notNullEnv(prefix, "connectionPrefix property required");
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

    public AmqpConfigurationProperties getConfigurationProperties(String connectionName) {
        return rootProperties.getConfigurations().get(connectionName);
    }

    public AmqpQueueProperties getQueueProperties(String connectionName, String queueKey) {
        return getConfigurationProperties(connectionName).getQueues().get(queueKey);
    }

    public AmqpTopicExchangeProperties getTopicExchangeProperties(String connectionName, String topicExchangekey) {
        return getConfigurationProperties(connectionName).getTopicExchanges().get(topicExchangekey);
    }

    public AmqpBindingProperties getBindingProperties(String connectionName, String queueKey, String topicExchangeKey) {
        return getConfigurationProperties(connectionName).getBindings().get(queueKey).get(topicExchangeKey);
    }

    public String getFactoryBeanName() {
        return factoryBeanName;
    }
}
