package com.github.eugenemsv.amqp.rabbit.properties;

public class AmqpListenerContainerProperties {

    private int concurrentConsumers = 1;

    private int maxConcurrentConsumers = 1;


    /**
     * Amount of concurrent consumer for message listener
     */
    public int getConcurrentConsumers() {
        return concurrentConsumers;
    }

    public void setConcurrentConsumers(int concurrentConsumers) {
        this.concurrentConsumers = concurrentConsumers;
    }

    /**
     * Amount of max concurrent consumer for message listener
     */
    public int getMaxConcurrentConsumers() {
        return maxConcurrentConsumers;
    }

    public void setMaxConcurrentConsumers(int maxConcurrentConsumers) {
        this.maxConcurrentConsumers = maxConcurrentConsumers;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("concurrentConsumers=").append(concurrentConsumers);
        sb.append(", maxConcurrentConsumers=").append(maxConcurrentConsumers);
        sb.append('}');
        return sb.toString();
    }
}
