package com.demo.amqp.properties;


import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "queue.management")
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
}
