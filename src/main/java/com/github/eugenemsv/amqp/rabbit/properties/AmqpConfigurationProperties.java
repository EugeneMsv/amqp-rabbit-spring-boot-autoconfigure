package com.github.eugenemsv.amqp.rabbit.properties;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.util.Map;

public class AmqpConfigurationProperties {

    private String connectionPrefix;

    private String deadLetterSuffix = ".errors";

    private AmqpRetryProperties retry = new AmqpRetryProperties();

    private AmqpListenerContainerProperties listener = new AmqpListenerContainerProperties();

    private Boolean defaultRequeueRejected = true;

    private boolean mismatchedQueuesFatal = true;

    private Map<String, AmqpQueueProperties> queues;

    private Map<String, AmqpTopicExchangeProperties> topicExchanges;

    //<queue-key>.<topic-key>.routingKeyPatterns.<pattern-name>=#
    private Map<String, Map<String, AmqpBindingProperties>> bindings;

    public String getConnectionPrefix() {
        return connectionPrefix;
    }

    public void setConnectionPrefix(String connectionPrefix) {
        this.connectionPrefix = connectionPrefix;
    }

    public String getDeadLetterSuffix() {
        return deadLetterSuffix;
    }

    public void setDeadLetterSuffix(String deadLetterSuffix) {
        this.deadLetterSuffix = deadLetterSuffix;
    }

    public AmqpRetryProperties getRetry() {
        return retry;
    }

    public void setRetry(AmqpRetryProperties retry) {
        this.retry = retry;
    }

    public AmqpListenerContainerProperties getListener() {
        return listener;
    }

    public void setListener(AmqpListenerContainerProperties listener) {
        this.listener = listener;
    }

    /**
     * From original documentation - Determines the default behavior when a message is rejected, for example because the listener
     * threw an exception. When true, messages will be requeued, when false, they will not. For
     * versions of Rabbit that support dead-lettering, the message must not be requeued in order
     * to be sent to the dead letter exchange. Setting to false causes all rejections to not
     * be requeued. When true, the default can be overridden by the listener throwing an
     * {@link AmqpRejectAndDontRequeueException}. Default true.
     */
    public Boolean getDefaultRequeueRejected() {
        return defaultRequeueRejected;
    }

    public void setDefaultRequeueRejected(Boolean defaultRequeueRejected) {
        this.defaultRequeueRejected = defaultRequeueRejected;
    }

    public boolean isMismatchedQueuesFatal() {
        return mismatchedQueuesFatal;
    }

    public void setMismatchedQueuesFatal(boolean mismatchedQueuesFatal) {
        this.mismatchedQueuesFatal = mismatchedQueuesFatal;
    }

    public Map<String, AmqpQueueProperties> getQueues() {
        return queues;
    }

    public void setQueues(
            Map<String, AmqpQueueProperties> queues) {
        this.queues = queues;
    }

    public Map<String, AmqpTopicExchangeProperties> getTopicExchanges() {
        return topicExchanges;
    }

    public void setTopicExchanges(
            Map<String, AmqpTopicExchangeProperties> topicExchanges) {
        this.topicExchanges = topicExchanges;
    }

    public Map<String, Map<String, AmqpBindingProperties>> getBindings() {
        return bindings;
    }

    public void setBindings(
            Map<String, Map<String, AmqpBindingProperties>> bindings) {
        this.bindings = bindings;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("connectionPrefix='").append(connectionPrefix).append('\'');
        sb.append(", deadLetterSuffix='").append(deadLetterSuffix).append('\'');
        sb.append(", mismatchedQueuesFatal='").append(mismatchedQueuesFatal).append('\'');
        sb.append(", retry=").append(retry);
        sb.append(", queues=").append(queues);
        sb.append('}');
        return sb.toString();
    }
}
