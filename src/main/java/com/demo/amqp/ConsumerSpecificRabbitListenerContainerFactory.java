package com.demo.amqp;

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

            AmqpListenerContainerProperties finalListenerProperties = listenerProperties;
            listenerProperties = Optional.ofNullable(consumerPropertiesByQueueName.get(queueName))
                    .filter(properties -> properties.getConcurrentConsumers() > 1)
                    .filter(properties -> properties.getMaxConcurrentConsumers() > 1)
                    .filter(properties -> finalListenerProperties == null
                            || properties.getMaxConcurrentConsumers() > finalListenerProperties
                            .getMaxConcurrentConsumers())
                    .orElse(finalListenerProperties);

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

        }
    }

    private ThreadPoolTaskExecutor buildTaskExecutor(String threadNamePrefix,
                                                     AmqpListenerContainerProperties listenerProperties) {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = executorObjectProvider
                .getObject(listenerProperties.getConcurrentConsumers(),
                        listenerProperties.getMaxConcurrentConsumers());
        threadPoolTaskExecutor.setThreadNamePrefix(threadNamePrefix);
        return threadPoolTaskExecutor;
    }
}
