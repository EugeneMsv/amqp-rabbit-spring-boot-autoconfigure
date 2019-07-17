package com.github.eugenemsv.amqp.rabbit.properties;


import com.github.eugenemsv.amqp.rabbit.AmqpAutoConfigurationConstants;
import com.github.eugenemsv.amqp.rabbit.utils.ValidationUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = AmqpAutoConfigurationConstants.Property.ROOT_PREFIX)
public class AmqpProperties {

    private boolean enabled;

    private boolean customObjectMapper;

    private Map<String, AmqpConfigurationProperties> configurations;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isCustomObjectMapper() {
        return customObjectMapper;
    }

    public void setCustomObjectMapper(boolean customObjectMapper) {
        this.customObjectMapper = customObjectMapper;
    }

    public Map<String, AmqpConfigurationProperties> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(Map<String, AmqpConfigurationProperties> configurations) {
        this.configurations = configurations;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AmqpProperties{");
        sb.append("configurations=").append(configurations);
        sb.append('}');
        return sb.toString();
    }

    public void validate() {
        ValidationUtils
                .notNullEnv(this.configurations, "%s configurations property required", AmqpAutoConfigurationConstants.Property.ROOT_PREFIX);
        for (Map.Entry<String, AmqpConfigurationProperties> configurationEntry : configurations.entrySet()) {
            String connectionName = configurationEntry.getKey();
            ValidationUtils.notEmptyEnv(connectionName, "Amqp connection-name property is required");

            AmqpConfigurationProperties configurationProperties = configurationEntry.getValue();
            ValidationUtils.notNullEnv(configurationProperties.getConnectionPrefix(),
                    "Connection prefix property required for connection-name=%s", connectionName);

            //Validate queues or topic exchange existence
            Map<String, AmqpQueueProperties> queues = configurationProperties.getQueues();
            Map<String, AmqpTopicExchangeProperties> topicExchanges = configurationProperties.getTopicExchanges();
            ValidationUtils.isTrueEnv(
                    (queues != null && !queues.isEmpty())
                            || (topicExchanges != null && !topicExchanges.isEmpty()),
                    "At least one queue or topic exchange should be defined for connection-name=%s",
                    connectionName);

            //Validate binding existence and matching with queues and topic exchanges
            Map<String, Map<String, AmqpBindingProperties>> bindings = configurationProperties.getBindings();
            if (bindings != null) {
                validateBindings(connectionName, queues, topicExchanges, bindings);
            }
        }
    }

    private void validateBindings(String connectionName, Map<String, AmqpQueueProperties> queues,
                                  Map<String, AmqpTopicExchangeProperties> topicExchanges,
                                  Map<String, Map<String, AmqpBindingProperties>> bindings) {
        ValidationUtils.notEmptyEnv(queues, "Queues properties should be defined for non empty bindings");
        ValidationUtils
                .notEmptyEnv(topicExchanges, "Topic exchanges properties should be defined for non empty bindings");
        for (Map.Entry<String, Map<String, AmqpBindingProperties>> bindingToQueue : bindings.entrySet()) {
            String queueKey = bindingToQueue.getKey();
            ValidationUtils.notEmptyEnv(queueKey, "Binding queue-key property is required, connection-name=%s",
                    connectionName);
            ValidationUtils.isTrueEnv(queues.containsKey(queueKey),
                    "Not found queue for binding queue-key=%s, connection-name=%s", queueKey, connectionName);

            for (Map.Entry<String, AmqpBindingProperties> bindingToTopicExchange :
                    bindingToQueue.getValue().entrySet()) {
                String topicExchangeKey = bindingToTopicExchange.getKey();
                ValidationUtils.notEmptyEnv(topicExchangeKey,
                        "Binding topic-exchange-key property is required, connection-name=%s", connectionName);
                ValidationUtils.isTrueEnv(topicExchanges.containsKey(topicExchangeKey),
                        "Not found topic exchange for binding queue-key=%s, connection-name=%s",
                        queueKey, connectionName);
            }
        }
    }
}
