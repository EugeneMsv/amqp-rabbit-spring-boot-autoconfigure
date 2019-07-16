package com.demo.amqp.bean;

import com.demo.amqp.properties.AmqpListenerContainerProperties;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class ConsumerSpecificRabbitListenerContainerFactory extends SimpleRabbitListenerContainerFactory {

    public static final int DEFAULT_MAX_THREAD_NAME_PREFIX_LENGTH = 25;

    public static final String RABBIT_THREAD_PREFIX = "rabbit-";

    private ObjectProvider<ThreadPoolTaskExecutor> executorObjectProvider;

    private Map<String, AmqpListenerContainerProperties> consumerPropertiesByQueueName = Collections.emptyMap();

    @Autowired
    @Qualifier("messagingThreadPoolTaskExecutor")
    public void setExecutorObjectProvider(ObjectProvider<ThreadPoolTaskExecutor> executorObjectProvider) {
        this.executorObjectProvider = executorObjectProvider;
    }

    public void setConsumerProperties(Map<String, AmqpListenerContainerProperties> consumerProperties) {
        if (consumerProperties != null) {
            this.consumerPropertiesByQueueName = consumerProperties;
        }
    }

    @Override
    protected void initializeContainer(SimpleMessageListenerContainer instance, RabbitListenerEndpoint endpoint) {
        customizeConsumer(instance);
    }

    protected void customizeConsumer(SimpleMessageListenerContainer instance) {
        StringBuilder rawThreadNamePrefix = new StringBuilder(RABBIT_THREAD_PREFIX);
        AmqpListenerContainerProperties listenerProperties = null;
        for (String queueName : instance.getQueueNames()) {

            listenerProperties = tryFindListenerProperties(listenerProperties, queueName);

            rawThreadNamePrefix.append(queueName).append(',');
        }
        // If at least one queue has not default listener properties
        if (listenerProperties != null) {

            String threadNamePrefix = rawThreadNamePrefix
                    .substring(0, Math.min(DEFAULT_MAX_THREAD_NAME_PREFIX_LENGTH, rawThreadNamePrefix.length()));
            ThreadPoolTaskExecutor threadPoolTaskExecutor = buildTaskExecutor(
                    threadNamePrefix + "-", listenerProperties);

            instance.setTaskExecutor(threadPoolTaskExecutor);
            instance.setMaxConcurrentConsumers(listenerProperties.getMaxConcurrentConsumers());
            instance.setConcurrentConsumers(listenerProperties.getConcurrentConsumers());
            // if MaxConcurrentConsumers > ConcurrentConsumers
            // and if any consumer handles at least one message try to start new consumer
            instance.setConsecutiveActiveTrigger(1);
        }
    }

    private AmqpListenerContainerProperties tryFindListenerProperties(
            AmqpListenerContainerProperties listenerProperties, String queueName) {
        return Optional.ofNullable(consumerPropertiesByQueueName.get(queueName))
                .filter(properties -> properties.getConcurrentConsumers() > 1)
                .filter(properties -> properties.getMaxConcurrentConsumers() > 1)
                .filter(properties -> listenerProperties == null
                        || properties.getMaxConcurrentConsumers() > listenerProperties
                        .getMaxConcurrentConsumers())
                .orElse(listenerProperties);
    }

    private ThreadPoolTaskExecutor buildTaskExecutor(String threadNamePrefix,
                                                     AmqpListenerContainerProperties listenerProperties) {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = executorObjectProvider
                .getObject(listenerProperties.getConcurrentConsumers(),
                        listenerProperties.getMaxConcurrentConsumers()+1);
        threadPoolTaskExecutor.setThreadNamePrefix(threadNamePrefix);
        return threadPoolTaskExecutor;
    }
}
