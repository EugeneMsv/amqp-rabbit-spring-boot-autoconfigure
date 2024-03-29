[![Build Status](https://travis-ci.org/EugeneMsv/amqp-rabbit-spring-boot-autoconfigure.png)](https://travis-ci.org/EugeneMsv/amqp-rabbit-spring-boot-autoconfigure)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.eugenemsv.amqp.rabbit/amqp-rabbit-spring-boot-autoconfigure/badge.png)](https://maven-badges.herokuapp.com/maven-central/com.github.eugenemsv.amqp.rabbit/amqp-rabbit-spring-boot-autoconfigure)

amqp-rabbit-spring-boot-autoconfigure
====

The goal of this project is to create Spring-Boot starter which allows integrate with RabbitMQ only via properties.
You don't need to write code for creating connections, queues, exchanges, bindings and etc. 
Besides it allows to connect to multiple RabbitMQ instances separately.

All configurations are based on [spring-amqp](https://docs.spring.io/spring-amqp/docs/latest-snapshot/reference/htmlsingle/) abstractions.

## Compatability

| amqp-rabbit-spring-boot-autoconfigure  | Spring-Boot |
| ------------- | ------------- |
| 1.0.0 | 2.1.6.RELEASE |

## Quick start

* Add dependencies:

    ```xml
    
        <dependency>
            <groupId>com.github.eugenemsv.amqp.rabbit</groupId>
            <artifactId>amqp-rabbit-spring-boot-autoconfigure</artifactId>
            <version>${version}</version>
        </dependency>
        
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-messaging</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.amqp</groupId>
            <artifactId>spring-rabbit</artifactId>
        </dependency>
     ```
* Add to properties file:
    ```properties
    local.jms.host=localhost
    local.jms.port=5672
    local.jms.user=guest
    local.jms.password=guest
    local.jms.vHost=/
    queue.rabbit.management.configurations.local.connection-prefix=local.jms
    queue.rabbit.management.configurations.local.queues.firstQueue.name=simple-queue
    queue.rabbit.management.configurations.local.queues.firstQueue.durable=true
    ```
* Add bean with method:
    ``` java
    @RabbitListener(queues = "${queue.rabbit.management.configurations.local.queues.firstQueue.name}")
    public void handleMessage(org.springframework.amqp.core.Message message) {
        System.out.println("Got message="+message);
    }
    ``` 
* Run application and publish message to the `simple-queue`  via Rabbit Management Plugin web ui.

## Documentation

* Definitions:

    * `<connection-name>` - any name associated with one RabbitMQ, takes part in creation of all library beans.
    * `<queue-key>` - any key associated with one Queue, takes part in queues and bindings creation.
    * `<topic-exchange-key>` - any key associated with one TopicExchange, takes part in topic exchanges and bindings creation.
    * `<routing-key-name>` - any name associated with one routingKey, takes part in bindings creation.
    
* Configuring properties:

    * Bind `<connection-name>` with RabbitMQ instance
        ```properties
        queue.rabbit.management.configurations.local.connection-prefix=local.jms
        
        local.jms.host=localhost
        local.jms.port=5672
        local.jms.user=guest
        local.jms.password=guest
        local.jms.vHost=/
        ```
         host, port, user, password, vHost - required properties
    
    * Message serialization is customized by `org.springframework.amqp.support.converter.MessageConverter`.
        The library adds **Jackson2JsonMessageConverter** as a default message converter to all connected RabbitMQ instances, 
        you can provide custom message converter by adding your bean to context.
        **Jackson2JsonMessageConverter** uses `com.fasterxml.jackson.databind.ObjectMapper` if there is no ObjectMapper in the context,
         than library will provide its own instance of **ObjectMapper**. 
    
        If you don't want use library's **MessageConverter** and you don't need its **ObjectMapper**
        you should set property:
        ``` properties
        queue.rabbit.management.custom-object-mapper=true
        ``` 
    
    * There is an ability to add retry support ([see the documentation for details](https://docs.spring.io/spring-amqp/docs/latest-snapshot/reference/htmlsingle/#template-retry))

         ```properties    
         queue.rabbit.management.configurations.<connection-name>.retry.maxAttempts=3
         queue.rabbit.management.configurations.<connection-name>.retry.initial-interval=1000
         queue.rabbit.management.configurations.<connection-name>.retry.multiplier=1.0
         queue.rabbit.management.configurations.<connection-name>.retry.maxI-interval=10000
         ```
    * [Exception handling](https://docs.spring.io/spring-amqp/docs/latest-snapshot/reference/htmlsingle/#exception-handling)
        ```properties    
         queue.rabbit.management.configurations.<connection-name>.defaultRequeueRejected
         ```
    * Concurrency settings for all listeners in one connection:
       ```properties
         queue.rabbit.management.configurations.<connection-name>.listener.concurrentConsumers
         queue.rabbit.management.configurations.<connection-name>.listener.maxConcurrentConsumers
       ``` 
    * Concurrency settings for listeners by queue:
        ```properties
          queue.rabbit.management.configurations.<connection-name>.queues.<queue-key>.listener.concurrentConsumers=<value>
          queue.rabbit.management.configurations.<connection-name>.queues.<queue-key>.listener.maxConcurrentConsumers=<value>
         ```   
         Since listener can listen simultaneously more than one queue there are such options:
         1. Only one listening queue has concurrency properties. In that case listener will be setup according this properties. 
    
         2. None of queues don't have concurrency properties. Listener will work in sequential way or according to:
         
            ```properties
            queue.rabbit.management.configurations.<connection-name>.listener.concurrentConsumers=<value>
            queue.rabbit.management.configurations.<connection-name>.listener.maxConcurrentConsumers=<value>
            ```
         3. Two or more queues have concurrency properties. There will be chosen configuration with highest 
            **maxConcurrentConsumers** value.
    * Support of fail fast behavior during startup.
        ```properties    
         queue.rabbit.management.configurations.<connection-name>.mismatchedQueuesFatal
         ```
        From official spring-amqp docs: 
            `
            This was added in version 1.6. When the container starts, if this property is true, 
            the container checks that all queues declared in the context are compatible with queues already on the broker.
            If mismatched properties (e.g. auto-delete) or arguments (e.g. x-message-ttl) exist, 
            the container (and application context) will fail to start with a fatal exception.
            If the problem is detected during recovery (e.g. after a lost connection), the container will be stopped.
             `
    * For disabling configuration you need add:
        ```properties
         queue.rabbit.management.enabled=false
        ```
    * Default property values:
        ```
        queue.rabbit.management.enabled=false
        queue.rabbit.management.custom-object-mapper=false
        
        queue.rabbit.management.configurations.<connection-name>.retry.enabled=false
        queue.rabbit.management.configurations.<connection-name>.retry.maxAttempts=3
        queue.rabbit.management.configurations.<connection-name>.retry.initial-interval=1000
        queue.rabbit.management.configurations.<connection-name>.retry.multiplier=1.0
        queue.rabbit.management.configurations.<connection-name>.retry.maxI-interval=10000
        
        queue.rabbit.management.configurations.<connection-name>.mismatchedQueuesFatal=true
        queue.rabbit.management.configurations.<connection-name>.defaultRequeueRejected=true
        queue.rabbit.management.configurations.<connection-name>.dead-letter-suffix=.errors
        
        queue.rabbit.management.configurations.<connection-name>.listener.concurrentConsumers=1
        queue.rabbit.management.configurations.<connection-name>.listener.maxConcurrentConsumers=1
        
        queue.rabbit.management.configurations.<connection-name>.queues.<queue-key>.durable=false
        queue.rabbit.management.configurations.<connection-name>.queues.<queue-key>.exclusive=false
        queue.rabbit.management.configurations.<connection-name>.queues.<queue-key>.autoDelete=false
        queue.rabbit.management.configurations.<connection-name>.queues.<queue-key>.with-dead-letter=false
        queue.rabbit.management.configurations.<connection-name>.queues.<queue-key>.dead-letter-config.bidirectional=false
        queue.rabbit.management.configurations.<connection-name>.queues.<queue-key>.dead-letter-config.time-to-live=3600000
        
        
        queue.rabbit.management.configurations.<connection-name>.topicExchanges.<topic-exchange-key>.name=
        queue.rabbit.management.configurations.<connection-name>.topicExchanges.<topic-exchange-key>.durable=false
        queue.rabbit.management.configurations.<connection-name>.topicExchanges.<topic-exchange-key>.autoDelete=false
        
        queue.rabbit.management.configurations.<connection-name>.bindings.<queue-key>.<topic-exchange-key>.routingKeys.<routing-key-name>=*.*.*
        ```
* Other features:   

    * Working with more than RabbitMQ instance you need carefully define listeners.
        Example:
        ```java
        @RabbitListener(queues = "fakeQueueName", containerFactory = "localRabbitListenerContainerFactory",  admin="localRabbitAdmin")
        ```
        In order to get info about registered beans, just setup log level to debug for package `com.github.eugenemsv.amqp.rabbit`
        
    * Library automatically excludes `org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration`
    
    * Each RabbitMQ connection has a name is built like this:
     `${spring.application.name}+<Connection-name>` 
        
    * In order to customize registration of bean definition you can provide custom implementation for `com.github.eugenemsv.amqp.rabbit.bean.AmqpBeanDefinitionCustomizer`
        and set full class name to the property `queue.rabbit.management.bean.definition.customizer`.
        Take care about no-args constructor in your custom implementation.
        
    * In order to customize creation of beans you can provide custom implementation for `com.github.eugenemsv.amqp.rabbit.bean.AmqpBeansFactory` 
    and set bean name to the property `queue.rabbit.management.bean.factory`. 
    Add custom implementation to the application context  like ordinary bean.
    
