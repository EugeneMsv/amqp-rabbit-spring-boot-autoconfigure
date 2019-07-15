package com.demo.amqp.bean;

import com.demo.amqp.properties.AmqpListenerContainerProperties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class ConsumerSpecificRabbitListenerContainerFactoryTest {

    private ConsumerSpecificRabbitListenerContainerFactory containerFactory = new ConsumerSpecificRabbitListenerContainerFactory();

    private ObjectProvider objectProvider = Mockito.mock(ObjectProvider.class);


    @Before
    public void setUp() {
        containerFactory.setExecutorObjectProvider(objectProvider);
        AmqpListenerContainerProperties properties1 = new AmqpListenerContainerProperties();
        properties1.setConcurrentConsumers(5);
        properties1.setMaxConcurrentConsumers(10);

        AmqpListenerContainerProperties properties2 = new AmqpListenerContainerProperties();
        properties2.setConcurrentConsumers(7);
        properties2.setMaxConcurrentConsumers(15);


        Map<String, AmqpListenerContainerProperties> propertiesMap = new HashMap<>();
        propertiesMap.put("queue1", properties1);
        propertiesMap.put("queue2", properties2);
        containerFactory.setConsumerProperties(propertiesMap);
    }

    @Test
    public void customizeConsumer() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = Mockito.mock(ThreadPoolTaskExecutor.class);
        when(objectProvider.getObject(eq(5), eq(10))).thenReturn(threadPoolTaskExecutor);

        SimpleMessageListenerContainer listenerContainer = mock(SimpleMessageListenerContainer.class);
        when(listenerContainer.getQueueNames()).thenReturn(new String[]{"queue1", "queue3"});
        containerFactory.customizeConsumer(listenerContainer);

        verify(threadPoolTaskExecutor, times(1)).setThreadNamePrefix(eq("rabbit-queue1,queue3,-"));
        verify(listenerContainer, times(1)).setTaskExecutor(same(threadPoolTaskExecutor));
        verify(listenerContainer, times(1)).setConcurrentConsumers(eq(5));
        verify(listenerContainer, times(1)).setMaxConcurrentConsumers(eq(10));

    }

    @Test
    public void customizeConsumer_MoreThanOneQueueProps() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = Mockito.mock(ThreadPoolTaskExecutor.class);
        when(objectProvider.getObject(eq(7), eq(15))).thenReturn(threadPoolTaskExecutor);

        SimpleMessageListenerContainer listenerContainer = mock(SimpleMessageListenerContainer.class);
        when(listenerContainer.getQueueNames()).thenReturn(new String[]{"queue1", "queue2"});
        containerFactory.customizeConsumer(listenerContainer);

        verify(threadPoolTaskExecutor, times(1)).setThreadNamePrefix(eq("rabbit-queue1,queue2,-"));
        verify(listenerContainer, times(1)).setTaskExecutor(same(threadPoolTaskExecutor));
        verify(listenerContainer, times(1)).setConcurrentConsumers(eq(7));
        verify(listenerContainer, times(1)).setMaxConcurrentConsumers(eq(15));

    }
}