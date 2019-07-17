package com.github.eugenemsv.amqp.rabbit;

import com.github.eugenemsv.amqp.rabbit.bean.AmqpBeansFactory;
import com.github.eugenemsv.amqp.rabbit.bean.DefaultAmqpBeanDefinitionCustomizer;
import com.github.eugenemsv.amqp.rabbit.properties.AmqpConnectionProperties;
import com.github.eugenemsv.amqp.rabbit.bean.AmqpBeanNameResolver;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.AbstractRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.RabbitListenerConfigUtils;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import static com.github.eugenemsv.amqp.rabbit.AmqpAutoConfigurationConstants.Property.BEAN_DEFINITION_CUSTOMIZER;
import static com.github.eugenemsv.amqp.rabbit.AmqpAutoConfigurationConstants.Property.FACTORY_BEAN_NAME;
import static com.github.eugenemsv.amqp.rabbit.AmqpAutoConfigurationConstants.Queue.*;
import static com.github.eugenemsv.amqp.rabbit.bean.AmqpBeanNameResolver.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class AmqpAutoConfigurationTest {

    private AnnotationApplicationContextTestWrapper contextWrapper;

    @Before
    public void setUp() {
        contextWrapper = AnnotationApplicationContextTestWrapper.from(new AnnotationConfigApplicationContext());
    }

    @After
    public void tearDown() {
        contextWrapper.close();
    }

    @Configuration
    protected static class MessageConverterConfiguration {

        @Bean
        public MessageConverter existingMessageConverter() {
            return mock(MessageConverter.class);
        }

    }

    private void assertConnectionBeans(String connectionName) {
        ApplicationContext context = contextWrapper.getContext();
        assertNotNull(context.getBean(RabbitListenerConfigUtils.RABBIT_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME));
        assertNotNull(context.getBean(RabbitListenerConfigUtils.RABBIT_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME));
        ConnectionFactory connectionFactory = context
                .getBean(getConnectionFactoryBeanName(connectionName), ConnectionFactory.class);
        assertNotNull(connectionFactory);

        SimpleRabbitListenerContainerFactory listenerContainerFactory = context.getBean(
                getRabbitListenerContainerFactoryBeanName(connectionName),
                SimpleRabbitListenerContainerFactory.class);
        assertNotNull(listenerContainerFactory);

        RabbitAdmin rabbitAdmin = context.getBean(getRabbitAdminBeanName(connectionName), RabbitAdmin.class);
        assertNotNull(rabbitAdmin);

        RabbitTemplate rabbitTemplate = context
                .getBean(getRabbitTemplateBeanName(connectionName), RabbitTemplate.class);
        assertNotNull(rabbitTemplate);
        assertSame(rabbitAdmin.getRabbitTemplate(), rabbitTemplate);
        ThreadPoolTaskExecutor threadPoolTaskExecutor1 = (ThreadPoolTaskExecutor) context
                .getBean("messagingThreadPoolTaskExecutor", 5, 10);
        ThreadPoolTaskExecutor threadPoolTaskExecutor2 = (ThreadPoolTaskExecutor) context
                .getBean("messagingThreadPoolTaskExecutor", 10, 20);

        assertNotSame(threadPoolTaskExecutor1, threadPoolTaskExecutor2);
        assertEquals(5, threadPoolTaskExecutor1.getCorePoolSize());
        assertEquals(10, threadPoolTaskExecutor1.getMaxPoolSize());

        assertEquals(10, threadPoolTaskExecutor2.getCorePoolSize());
        assertEquals(20, threadPoolTaskExecutor2.getMaxPoolSize());
    }

    @Test
    public void test_fillConnectionPropertiesMap_OneConnection() {
        String namePrefix = "local";

        ApplicationContext context = contextWrapper
                .withConfiguration(MessageConverterConfiguration.class, AmqpAutoConfiguration.class)
                .withEnvironment("queue.rabbit.management.configurations." + namePrefix + ".connection-prefix:rabbit.jms",
                        "rabbit.jms.host:fakeHost",
                        "rabbit.jms.port:123",
                        "rabbit.jms.user:fakeUser",
                        "rabbit.jms.password:fakePassword",
                        "rabbit.jms.vHost:fakeVHost",
                        "queue.rabbit.management.configurations.local.queues.fake-queue.name:fakeQueueName")
                .build()
                .getContext();
        AmqpPropertiesSupplier propertiesSupplier = new AmqpPropertiesSupplier(context.getEnvironment());
        Map<String, AmqpConnectionProperties> connectionMap = propertiesSupplier.getConnectionPropertiesMap();
        assertTrue(connectionMap.containsKey(namePrefix));
        AmqpConnectionProperties localConnection = connectionMap.get(namePrefix);

        assertEquals("fakeHost", localConnection.getHost());
        assertEquals(123, localConnection.getPort());
        assertEquals("fakeUser", localConnection.getUser());
        assertEquals("fakePassword", localConnection.getPassword());
        assertEquals("fakeVHost", localConnection.getvHost());
    }

    @Test
    public void test_fillConnectionPropertiesMap_ManyConnections() {
        String namePrefix1 = "local1";
        String namePrefix2 = "local2";
        String namePrefix3 = "local3";
        ApplicationContext context = contextWrapper
                .withConfiguration(MessageConverterConfiguration.class, AmqpAutoConfiguration.class)
                .withEnvironment("queue.rabbit.management.configurations.local1.connection-prefix:local1.jms",
                        "local1.jms.host:fakeHost1",
                        "local1.jms.port:1",
                        "local1.jms.user:fakeUser1",
                        "local1.jms.password:fakePassword1",
                        "local1.jms.vHost:fakeVHost1",
                        "queue.rabbit.management.configurations.local1.queues.fake-queue.name:fakeQueueName1",

                        "queue.rabbit.management.configurations.local2.connection-prefix:local2.jms",
                        "local2.jms.host:fakeHost2",
                        "local2.jms.port:2",
                        "local2.jms.user:fakeUser2",
                        "local2.jms.password:fakePassword2",
                        "local2.jms.vHost:fakeVHost2",
                        "queue.rabbit.management.configurations.local2.queues.fake-queue.name:fakeQueueName2",

                        "queue.rabbit.management.configurations.local3.connection-prefix:local3.jms",
                        "local3.jms.host:fakeHost3",
                        "local3.jms.port:3",
                        "local3.jms.user:fakeUser3",
                        "local3.jms.password:fakePassword3",
                        "local3.jms.vHost:fakeVHost3",
                        "queue.rabbit.management.configurations.local3.queues.fake-queue.name:fakeQueueName3")
                .build()
                .getContext();

        AmqpPropertiesSupplier propertiesSupplier = new AmqpPropertiesSupplier(context.getEnvironment());
        Map<String, AmqpConnectionProperties> connectionMap = propertiesSupplier.getConnectionPropertiesMap();

        AmqpConnectionProperties local1Connection = connectionMap.get(namePrefix1);
        assertNotNull(local1Connection);

        assertEquals("fakeHost1", local1Connection.getHost());
        assertEquals(1, local1Connection.getPort());
        assertEquals("fakeUser1", local1Connection.getUser());
        assertEquals("fakePassword1", local1Connection.getPassword());
        assertEquals("fakeVHost1", local1Connection.getvHost());

        AmqpConnectionProperties local2Connection = connectionMap.get(namePrefix2);
        assertNotNull(local2Connection);

        assertEquals("fakeHost2", local2Connection.getHost());
        assertEquals(2, local2Connection.getPort());
        assertEquals("fakeUser2", local2Connection.getUser());
        assertEquals("fakePassword2", local2Connection.getPassword());
        assertEquals("fakeVHost2", local2Connection.getvHost());

        AmqpConnectionProperties local3Connection = connectionMap.get(namePrefix3);
        assertNotNull(local3Connection);

        assertEquals("fakeHost3", local3Connection.getHost());
        assertEquals(3, local3Connection.getPort());
        assertEquals("fakeUser3", local3Connection.getUser());
        assertEquals("fakePassword3", local3Connection.getPassword());
        assertEquals("fakeVHost3", local3Connection.getvHost());
    }

    @Test
    public void test_oneSimpleQueue() {
        String namePrefix = "local";
        ApplicationContext context = contextWrapper
                .withConfiguration(MessageConverterConfiguration.class, AmqpAutoConfiguration.class)
                .withEnvironment(
                        "queue.rabbit.management.configurations.local.connection-prefix:local.jms",
                        "local.jms.host:fakeHost",
                        "local.jms.port:123",
                        "local.jms.user:fakeUser",
                        "local.jms.password:fakePassword",
                        "local.jms.vHost:fakeVHost",
                        "queue.rabbit.management.configurations.local.queues.doc-request.name:fakeQueueName",
                        "queue.rabbit.management.configurations.local.queues.doc-request.durable:true")
                .build()
                .getContext();

        assertConnectionBeans(namePrefix);

        Queue localQueue = context
                .getBean(getQueueBeanName(namePrefix, "doc-request"), Queue.class);
        assertNotNull(localQueue);
        assertEquals("fakeQueueName", localQueue.getName());
        assertTrue(localQueue.isDurable());
        assertFalse(localQueue.isExclusive());
        assertFalse(localQueue.isAutoDelete());
        Collection<RabbitAdmin> declaringAdmins = (Collection<RabbitAdmin>) localQueue.getDeclaringAdmins();
        assertNotNull(declaringAdmins);
        assertEquals(1, declaringAdmins.size());
        assertSame(context.getBean(getRabbitAdminBeanName(namePrefix)), declaringAdmins.stream().findFirst().get());
    }

    @Test
    public void test_simpleQueuesInOneRabbit() {
        String namePrefix = "local";
        ApplicationContext context = contextWrapper
                .withConfiguration(MessageConverterConfiguration.class, AmqpAutoConfiguration.class)
                .withEnvironment("queue.rabbit.management.configurations.local.connection-prefix:local.jms",
                        "local.jms.host:fakeHost",
                        "local.jms.port:123",
                        "local.jms.user:fakeUser",
                        "local.jms.password:fakePassword",
                        "local.jms.vHost:fakeVHost",
                        "queue.rabbit.management.configurations.local.queues.doc-request.name:docRequestName",
                        "queue.rabbit.management.configurations.local.queues.doc-request.durable:true",
                        "queue.rabbit.management.configurations.local.queues.doc-request.listener.concurrentConsumers:5",
                        "queue.rabbit.management.configurations.local.queues.doc-request.listener.maxConcurrentConsumers:10",
                        "queue.rabbit.management.configurations.local.queues.doc-response.name:docResponseName",
                        "queue.rabbit.management.configurations.local.queues.doc-response.durable:false")
                .build()
                .getContext();

        assertConnectionBeans(namePrefix);
        RabbitAdmin localRabbitAdmin = context.getBean(getRabbitAdminBeanName(namePrefix), RabbitAdmin.class);

        Queue localDocRequestQueue = context
                .getBean(getQueueBeanName(namePrefix, "doc-request"), Queue.class);
        assertNotNull(localDocRequestQueue);
        assertEquals("docRequestName", localDocRequestQueue.getName());
        assertTrue(localDocRequestQueue.isDurable());
        assertFalse(localDocRequestQueue.isExclusive());
        assertFalse(localDocRequestQueue.isAutoDelete());
        Collection<RabbitAdmin> docRequestDeclaringAdmins =
                (Collection<RabbitAdmin>) localDocRequestQueue.getDeclaringAdmins();
        assertNotNull(docRequestDeclaringAdmins);
        assertEquals(1, docRequestDeclaringAdmins.size());
        assertSame(localRabbitAdmin, docRequestDeclaringAdmins.stream().findFirst().get());

        Queue localDocResponseQueue = context.getBean(getQueueBeanName(namePrefix, "doc-response"), Queue.class);

        assertNotNull(localDocResponseQueue);
        assertEquals("docResponseName", localDocResponseQueue.getName());
        assertFalse(localDocResponseQueue.isDurable());
        assertFalse(localDocResponseQueue.isExclusive());
        assertFalse(localDocResponseQueue.isAutoDelete());
        Collection<RabbitAdmin> docResponseDeclaringAdmins =
                (Collection<RabbitAdmin>) localDocResponseQueue.getDeclaringAdmins();
        assertNotNull(docResponseDeclaringAdmins);
        assertEquals(1, docResponseDeclaringAdmins.size());
        assertSame(localRabbitAdmin, docResponseDeclaringAdmins.stream().findFirst().get());
    }

    @Test
    public void test_ManyConnections_WithComplexQueues() {
        String namePrefix1 = "local1";
        String namePrefix2 = "local2";
        String namePrefix3 = "local3";
        ApplicationContext context = contextWrapper
                .withConfiguration(MessageConverterConfiguration.class, AmqpAutoConfiguration.class)
                .withEnvironment(
                        "spring.application.name:fake-Application",
                        "queue.rabbit.management.configurations.local1.connection-prefix:local1.jms",
                        "local1.jms.host:fakeHost1",
                        "local1.jms.port:1",
                        "local1.jms.user:fakeUser1",
                        "local1.jms.password:fakePassword1",
                        "local1.jms.vHost:fakeVHost1",
                        "queue.rabbit.management.configurations.local1.dead-letter-suffix:.errors1",
                        "queue.rabbit.management.configurations.local1.mismatchedQueuesFatal:false",
                        "queue.rabbit.management.configurations.local1.queues.fake-queue.name:fakeQueueName1",
                        "queue.rabbit.management.configurations.local1.queues.fake-queue.withDeadLetter:true",
                        "queue.rabbit.management.configurations.local1.queues.fake-queue.durable:true",
                        "queue.rabbit.management.configurations.local1.queues.fake-queue.exclusive:true",
                        "queue.rabbit.management.configurations.local1.queues.fake-queue.auto-delete:true",

                        "queue.rabbit.management.configurations.local2.connection-prefix:local2.jms",
                        "local2.jms.host:fakeHost2",
                        "local2.jms.port:2",
                        "local2.jms.user:fakeUser2",
                        "local2.jms.password:fakePassword2",
                        "local2.jms.vHost:fakeVHost2",
                        "queue.rabbit.management.configurations.local2.dead-letter-suffix:.errors2",
                        "queue.rabbit.management.configurations.local2.queues.fake-queue.name:fakeQueueName2",
                        "queue.rabbit.management.configurations.local2.queues.fake-queue.withDeadLetter:true",
                        "queue.rabbit.management.configurations.local2.queues.fake-queue.dead-letter-config.bidirectional:true",
                        "queue.rabbit.management.configurations.local2.queues.fake-queue.dead-letter-config.timeToLive:100",

                        "queue.rabbit.management.configurations.local3.connection-prefix:local3.jms",
                        "local3.jms.host:fakeHost3",
                        "local3.jms.port:3",
                        "local3.jms.user:fakeUser3",
                        "local3.jms.password:fakePassword3",
                        "local3.jms.vHost:fakeVHost3",
                        "queue.rabbit.management.configurations.local3.dead-letter-suffix:.errors3",
                        "queue.rabbit.management.configurations.local3.queues.fake-queue.name:fakeQueueName3",
                        "queue.rabbit.management.configurations.local3.queues.fake-queue.arguments.key3:value3",

                        "queue.rabbit.management.configurations.local3.queues.queueForBinding1.name:someQueueForBinding",
                        "queue.rabbit.management.configurations.local3.queues.queueForBinding1.durable:true",
                        "queue.rabbit.management.configurations.local3.queues.queueForBinding1.autoDelete:true",
                        "queue.rabbit.management.configurations.local3.topicExchanges.topicExchangeForBinding1.name:someTopicName",
                        "queue.rabbit.management.configurations.local3.topicExchanges.topicExchangeForBinding1.durable:true",
                        "queue.rabbit.management.configurations.local3.topicExchanges.topicExchangeForBinding1.autoDelete:true",
                        "queue.rabbit.management.configurations.local3.bindings.queueForBinding1.topicExchangeForBinding1.routingKeys.rk1:request1.*.*",
                        "queue.rabbit.management.configurations.local3.bindings.queueForBinding1.topicExchangeForBinding1.routingKeys.rk2:request2.*.*"
                )
                .build()
                .getContext();
        assertConnectionBeans(namePrefix1);
        assertConnectionBeans(namePrefix2);
        assertConnectionBeans(namePrefix3);

        // Assert local1
        Queue queue1 = context.getBean(getQueueBeanName(namePrefix1, "fake-queue"), Queue.class);
        Queue deadLetterQueue1 = context.getBean(
                getDeadLetterQueueBeanName(namePrefix1, "fake-queue"), Queue.class);

        assertNotNull(queue1);
        assertNotNull(deadLetterQueue1);

        // Assert queue1
        assertTrue(queue1.isAutoDelete());
        assertTrue(queue1.isDurable());
        assertTrue(queue1.isExclusive());
        assertEquals("fakeQueueName1", queue1.getName());
        Map<String, Object> arguments1 = queue1.getArguments();
        assertNotNull(arguments1);
        Assert.assertEquals(ARGUMENT_VALUE_DEAD_LETTER_EXCHANGE, arguments1.get(ARGUMENT_KEY_DEAD_LETTER_EXCHANGE));
        assertEquals(deadLetterQueue1.getName(), arguments1.get(ARGUMENT_KEY_DEAD_LETTER_ROUTING));

        // Assert deadLetterQueue1
        assertTrue(deadLetterQueue1.isAutoDelete());
        assertTrue(deadLetterQueue1.isDurable());
        assertTrue(deadLetterQueue1.isExclusive());
        assertEquals("fakeQueueName1.errors1", deadLetterQueue1.getName());
        assertTrue(CollectionUtils.isEmpty(deadLetterQueue1.getArguments()));

        // Assert local2
        Queue queue2 = context.getBean(getQueueBeanName(namePrefix2, "fake-queue"), Queue.class);
        Queue deadLetterQueue2 = context.getBean(getDeadLetterQueueBeanName(namePrefix2, "fake-queue"), Queue.class);

        assertNotNull(queue2);
        assertNotNull(deadLetterQueue2);

        // Assert queue2
        assertFalse(queue2.isAutoDelete());
        assertFalse(queue2.isDurable());
        assertFalse(queue2.isExclusive());
        assertEquals("fakeQueueName2", queue2.getName());
        Map<String, Object> arguments2 = queue2.getArguments();
        assertNotNull(arguments2);
        Assert.assertEquals(ARGUMENT_VALUE_DEAD_LETTER_EXCHANGE, arguments2.get(ARGUMENT_KEY_DEAD_LETTER_EXCHANGE));
        assertEquals(deadLetterQueue2.getName(), arguments2.get(ARGUMENT_KEY_DEAD_LETTER_ROUTING));

        // Assert deadLetterQueue2
        assertFalse(deadLetterQueue2.isAutoDelete());
        assertFalse(deadLetterQueue2.isDurable());
        assertFalse(deadLetterQueue2.isExclusive());
        assertEquals("fakeQueueName2.errors2", deadLetterQueue2.getName());
        Map<String, Object> deadLetterQueueArguments2 = deadLetterQueue2.getArguments();
        assertNotNull(deadLetterQueueArguments2);
        Assert.assertEquals(ARGUMENT_VALUE_DEAD_LETTER_EXCHANGE,
                deadLetterQueueArguments2.get(ARGUMENT_KEY_DEAD_LETTER_EXCHANGE));
        assertEquals(queue2.getName(), deadLetterQueueArguments2.get(ARGUMENT_KEY_DEAD_LETTER_ROUTING));
        assertEquals(100, deadLetterQueueArguments2.get(ARGUMENT_KEY_MESSAGE_TTL));

        // Assert local3
        Queue queue3 = context.getBean(getQueueBeanName(namePrefix3, "fake-queue"), Queue.class);
        assertNotNull(queue3);
        assertFalse(context.containsBean(getDeadLetterQueueBeanName(namePrefix3, "fake-queue")));

        assertFalse(queue3.isExclusive());
        assertFalse(queue3.isAutoDelete());
        assertFalse(queue3.isDurable());
        assertEquals("fakeQueueName3", queue3.getName());
        Map<String, Object> arguments3 = queue3.getArguments();
        assertNotNull(arguments3);
        assertEquals(1, arguments3.size());
        assertEquals("value3", arguments3.get("key3"));

        Queue queueForBinding1 = context
                .getBean(getQueueBeanName(namePrefix3, "queueForBinding1"), Queue.class);

        TopicExchange topicExchangeForBinding1 = context.getBean(
                getTopicExchangeBeanName(namePrefix3, "topicExchangeForBinding1"),
                TopicExchange.class);
        Binding queueForBinding1topicExchangeForBinding1rk1 = context.getBean(
                getBindingBeanName(namePrefix3, "queueForBinding1", "topicExchangeForBinding1", "rk1"),
                Binding.class);
        Binding queueForBinding1topicExchangeForBinding1rk2 = context.getBean(
                getBindingBeanName(namePrefix3, "queueForBinding1", "topicExchangeForBinding1", "rk2"),
                Binding.class);
        assertNotNull(queueForBinding1);
        assertNotNull(topicExchangeForBinding1);
        assertNotNull(queueForBinding1topicExchangeForBinding1rk1);
        assertEquals("someTopicName", queueForBinding1topicExchangeForBinding1rk1.getExchange());
        assertEquals("request1.*.*", queueForBinding1topicExchangeForBinding1rk1.getRoutingKey());

        assertNotNull(queueForBinding1topicExchangeForBinding1rk2);
        assertEquals("someTopicName", queueForBinding1topicExchangeForBinding1rk2.getExchange());
        assertEquals("request2.*.*", queueForBinding1topicExchangeForBinding1rk2.getRoutingKey());
    }

    @Test
    public void test_applyRetryProperties() throws NoSuchFieldException, IllegalAccessException {
        String namePrefix = "local";
        ApplicationContext context = contextWrapper
                .withConfiguration(MessageConverterConfiguration.class, AmqpAutoConfiguration.class)
                .withEnvironment("queue.rabbit.management.configurations.local.connection-prefix:local.jms",
                        "queue.rabbit.management.configurations.local.retry.enabled:true",
                        "queue.rabbit.management.configurations.local.retry.max-attempts:5",
                        "queue.rabbit.management.configurations.local.retry.initial-interval:50",
                        "queue.rabbit.management.configurations.local.retry.multiplier:1.5",
                        "queue.rabbit.management.configurations.local.retry.max-interval:500",
                        "local.jms.host:fakeHost",
                        "local.jms.port:123",
                        "local.jms.user:fakeUser",
                        "local.jms.password:fakePassword",
                        "local.jms.vHost:fakeVHost",
                        "queue.rabbit.management.configurations.local.queues.doc-request.name:fakeQueueName",
                        "queue.rabbit.management.configurations.local.queues.doc-request.durable:true")
                .build()
                .getContext();

        assertConnectionBeans(namePrefix);
        RabbitTemplate rabbitTemplate = context.getBean(getRabbitTemplateBeanName(namePrefix), RabbitTemplate.class);
        Field retryTemplateField = RabbitTemplate.class.getDeclaredField("retryTemplate");
        retryTemplateField.setAccessible(true);
        RetryTemplate retryTemplate = (RetryTemplate) retryTemplateField.get(rabbitTemplate);
        assertNotNull(retryTemplate);

        Field retryPolicyField = RetryTemplate.class.getDeclaredField("retryPolicy");
        retryPolicyField.setAccessible(true);
        SimpleRetryPolicy simpleRetryPolicy = (SimpleRetryPolicy) retryPolicyField.get(retryTemplate);
        assertEquals(5, simpleRetryPolicy.getMaxAttempts());


        Field backOffPolicyField = RetryTemplate.class.getDeclaredField("backOffPolicy");
        backOffPolicyField.setAccessible(true);
        ExponentialBackOffPolicy backOffPolicy = (ExponentialBackOffPolicy) backOffPolicyField.get(retryTemplate);
        assertNotNull(backOffPolicy);

        assertEquals(50, backOffPolicy.getInitialInterval());
        assertEquals(500, backOffPolicy.getMaxInterval());
        assertEquals(1.5, backOffPolicy.getMultiplier(), 10E-5);

    }

    @Test
    public void test_containerListenerProperties() throws NoSuchFieldException, IllegalAccessException {
        String namePrefix = "local";
        ApplicationContext context = contextWrapper
                .withConfiguration(MessageConverterConfiguration.class, AmqpAutoConfiguration.class)
                .withEnvironment(
                        "queue.rabbit.management.configurations.local.connection-prefix:local.jms",
                        "queue.rabbit.management.configurations.local.defaultRequeueRejected:false",
                        "queue.rabbit.management.configurations.local.listener.concurrentConsumers:2",
                        "queue.rabbit.management.configurations.local.listener.maxConcurrentConsumers:5",
                        "local.jms.host:fakeHost",
                        "local.jms.port:123",
                        "local.jms.user:fakeUser",
                        "local.jms.password:fakePassword",
                        "local.jms.vHost:fakeVHost",
                        "queue.rabbit.management.configurations.local.queues.doc-request.name:fakeQueueName",
                        "queue.rabbit.management.configurations.local.queues.doc-request.durable:true")
                .build()
                .getContext();

        assertConnectionBeans(namePrefix);
        SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory = context.getBean(
                getRabbitListenerContainerFactoryBeanName(namePrefix),
                SimpleRabbitListenerContainerFactory.class);
        Field concurrentConsumersField = SimpleRabbitListenerContainerFactory.class
                .getDeclaredField("concurrentConsumers");
        Field maxConcurrentConsumersField = SimpleRabbitListenerContainerFactory.class
                .getDeclaredField("maxConcurrentConsumers");
        Field defaultRequeueRejectedField = AbstractRabbitListenerContainerFactory.class
                .getDeclaredField("defaultRequeueRejected");

        concurrentConsumersField.setAccessible(true);
        maxConcurrentConsumersField.setAccessible(true);
        defaultRequeueRejectedField.setAccessible(true);
        assertEquals(2, concurrentConsumersField.get(rabbitListenerContainerFactory));
        assertEquals(5, maxConcurrentConsumersField.get(rabbitListenerContainerFactory));
        assertEquals(false, defaultRequeueRejectedField.get(rabbitListenerContainerFactory));
    }


    @Configuration
    protected static class CustomBeansFactoryConfiguration {

        @Bean
        public AmqpBeansFactory customBeansFactory() {
            return mock(AmqpBeansFactory.class);
        }

    }

    @Test
    public void test_customAmqpBeansFactory() {
        String customBeansFactoryName = "customBeansFactory";
        ApplicationContext context = contextWrapper
                .withConfiguration(CustomBeansFactoryConfiguration.class,
                        MessageConverterConfiguration.class,
                        AmqpAutoConfiguration.class)
                .withEnvironment(
                        FACTORY_BEAN_NAME + ":" + customBeansFactoryName,
                        "queue.rabbit.management.configurations.local.connection-prefix:local.jms",
                        "local.jms.host:fakeHost",
                        "local.jms.port:123",
                        "local.jms.user:fakeUser",
                        "local.jms.password:fakePassword",
                        "local.jms.vHost:fakeVHost",
                        "queue.rabbit.management.configurations.local.queues.doc-request.name:fakeQueueName",
                        "queue.rabbit.management.configurations.local.queues.doc-request.durable:true")
                .build()
                .getContext();
        String[] beanNamesForAmqpBeansFactory = context.getBeanNamesForType(AmqpBeansFactory.class);
        assertNotNull(beanNamesForAmqpBeansFactory);
        assertEquals(1, beanNamesForAmqpBeansFactory.length);
        assertEquals(customBeansFactoryName, beanNamesForAmqpBeansFactory[0]);
    }

    public static class TestAmqpBeanDefinitionCustomizer extends DefaultAmqpBeanDefinitionCustomizer {

        public static int invocationsAmount;

        @Override
        public Class<?> getConnectionFactoryClass() {
            invocationsAmount++;
            return super.getConnectionFactoryClass();
        }

        @Override
        public Class<?> getRabbitListenerContainerFactoryClass() {
            invocationsAmount++;
            return super.getRabbitListenerContainerFactoryClass();
        }

        @Override
        public Class<?> getRabbitTemplateClass() {
            invocationsAmount++;
            return super.getRabbitTemplateClass();
        }

        @Override
        public Class<?> getRabbitAdminClass() {
            invocationsAmount++;
            return super.getRabbitAdminClass();
        }
    }

    @Test
    public void test_customAmqpBeanDefinitionCustomizer() {
        assertEquals(0, TestAmqpBeanDefinitionCustomizer.invocationsAmount);
        contextWrapper
                .withConfiguration(MessageConverterConfiguration.class,
                        AmqpAutoConfiguration.class)
                .withEnvironment(
                        BEAN_DEFINITION_CUSTOMIZER + ":" + "com.github.eugenemsv.amqp.rabbit.AmqpAutoConfigurationTest$TestAmqpBeanDefinitionCustomizer",
                        "queue.rabbit.management.configurations.local.connection-prefix:local.jms",
                        "local.jms.host:fakeHost",
                        "local.jms.port:123",
                        "local.jms.user:fakeUser",
                        "local.jms.password:fakePassword",
                        "local.jms.vHost:fakeVHost",
                        "queue.rabbit.management.configurations.local.queues.doc-request.name:fakeQueueName",
                        "queue.rabbit.management.configurations.local.queues.doc-request.durable:true")
                .build();

        assertEquals(4, TestAmqpBeanDefinitionCustomizer.invocationsAmount);
    }
}