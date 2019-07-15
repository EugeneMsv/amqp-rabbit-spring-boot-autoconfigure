package com.demo.amqp.properties;


import com.demo.amqp.utils.ValidationUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

import static com.demo.amqp.AmqpAutoConfigurationConstants.Property.ROOT_PREFIX;

@ConfigurationProperties(prefix = ROOT_PREFIX)
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
        ValidationUtils.notNullEnv(this.configurations, "%s configurations property required", ROOT_PREFIX);
        for (Map.Entry<String, AmqpConfigurationProperties> configurationEntry : configurations.entrySet()) {
            String connectionName = configurationEntry.getKey();
            ValidationUtils.notEmptyEnv(connectionName, "Amqp connection name is required");

            AmqpConfigurationProperties configurationProperties = configurationEntry.getValue();
            ValidationUtils.notNullEnv(configurationProperties.getConnectionPrefix(),
                    "Connection prefix required for connection name=%s", connectionName);

            //Validate queues or topic exchange existence
            Map<String, AmqpQueueProperties> queues = configurationProperties.getQueues();
            Map<String, AmqpTopicExchangeProperties> topicExchanges = configurationProperties.getTopicExchanges();
            ValidationUtils.isTrueEnv(
                    (queues != null && !queues.isEmpty())
                            || (topicExchanges != null && !topicExchanges.isEmpty()),
                    "At least one queue or topic exchange should be defined for connection name=%s",
                    connectionName);

            //Validate binding existence and matching with queues and topic exchanges
            Map<String, Map<String, AmqpBindingProperties>> bindings = configurationProperties.getBindings();
            if (bindings != null) {
                validateBindings(queues, topicExchanges, bindings);
            }
        }
    }

    private void validateBindings(Map<String, AmqpQueueProperties> queues,
                                  Map<String, AmqpTopicExchangeProperties> topicExchanges,
                                  Map<String, Map<String, AmqpBindingProperties>> bindings) {
        ValidationUtils.notEmptyEnv(queues, "Queues should be defined for non empty bindings");
        ValidationUtils.notEmptyEnv(topicExchanges, "Topic exchanges should be defined for non empty bindings");
        for (Map.Entry<String, Map<String, AmqpBindingProperties>> bindingToQueue : bindings.entrySet()) {
            String queueKey = bindingToQueue.getKey();
            ValidationUtils.notEmptyEnv(queueKey, "Binding queue key required");
            ValidationUtils.isTrueEnv(queues.containsKey(queueKey),
                    "Not found queue for binding queue key=%s", queueKey);

            for (Map.Entry<String, AmqpBindingProperties> bindingToTopicExchange :
                    bindingToQueue.getValue().entrySet()) {
                String topicExchangeKey = bindingToTopicExchange.getKey();
                ValidationUtils.notEmptyEnv(topicExchangeKey, "Binding topic exchange key required");
                ValidationUtils.isTrueEnv(topicExchanges.containsKey(topicExchangeKey),
                        "Not found topic exchange for binding topic exchange key=%s", queueKey);
            }
        }
    }
}
