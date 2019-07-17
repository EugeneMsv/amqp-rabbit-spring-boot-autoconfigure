package com.github.eugenemsv.amqp.rabbit.properties;

import java.util.HashMap;
import java.util.Map;

public class AmqpQueueProperties {

    private AmqpListenerContainerProperties listener;

    private String name;

    private boolean durable;

    private boolean exclusive;

    private boolean autoDelete;

    private Map<String, Object> arguments;

    /**
     * Auto dead-letter creation, set it to true if you won't create separate config for dead letter
     */
    private boolean withDeadLetter;

    /**
     * Config for auto created dead-letter
     */
    private AmqpDeadLetterProperties deadLetterConfig;

    public AmqpListenerContainerProperties getListener() {
        return listener;
    }

    public void setListener(AmqpListenerContainerProperties listener) {
        this.listener = listener;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDurable() {
        return durable;
    }

    public void setDurable(boolean durable) {
        this.durable = durable;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

    public boolean isAutoDelete() {
        return autoDelete;
    }

    public void setAutoDelete(boolean autoDelete) {
        this.autoDelete = autoDelete;
    }

    public Map<String, Object> getArguments() {
        if (arguments == null) {
            arguments = new HashMap<>();
        }
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }

    public boolean isWithDeadLetter() {
        return withDeadLetter;
    }

    public void setWithDeadLetter(boolean withDeadLetter) {
        this.withDeadLetter = withDeadLetter;
    }

    public AmqpDeadLetterProperties getDeadLetterConfig() {
        return deadLetterConfig;
    }

    public void setDeadLetterConfig(AmqpDeadLetterProperties deadLetterConfig) {
        this.deadLetterConfig = deadLetterConfig;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{name='");
        sb.append(name).append('\'');
        sb.append(", durable=").append(durable);
        sb.append(", exclusive=").append(exclusive);
        sb.append(", autoDelete=").append(autoDelete);
        sb.append(", arguments=").append(arguments);
        sb.append(", withDeadLetter=").append(withDeadLetter);
        sb.append(", deadLetterConfig=").append(deadLetterConfig);
        sb.append(", listener=").append(listener);
        sb.append('}');
        return sb.toString();
    }
}
