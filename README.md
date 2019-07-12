<!--todo translate-->
Управление и работа с очередями RabbitMQ.

### Необоходимые шаги для подключения:
1. Подключить зависимости:

    ```xml
    <dependency>
        <groupId>com.demo.amqp</groupId>
        <artifactId>spring-amqp-rabbit-configurator</artifactId>
        <version>currentVersion</version>
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

2. Добавить проперти для настройки
  * Если `queue.management.custom-object-mapper=false` и в контексте нет `ObjectMapper` то библиотека создаст дефолтный, если `queue.management.custom-object-mapper=true` или в контексте есть `ObjectMapper`, то дефолтный создаваться не будет.
  * Определение connection:
        ```
        queue.management.configurations.<connection-name>.connection-prefix=<prefix>
        ```

        * `<connection-name>` - любое имя которым вы идентифицируете коннекшн к раббиту и префикс для имен бинов относящих  к данному коннекшену.
        * `<prefix>` - префикс пропертей для установления коннекшена.

        Пример: 
      
        ```
        queue.management.configurations.local.connection-prefix=local.jms
        ```
    
        Означает что для коннекта к раббиту будут использоваться:
        ```
        local.jms.host=localhost
        local.jms.port=5672
        local.jms.user=guest
        local.jms.password=guest
        local.jms.vHost=/
        ```
        Если хотя бы одна из пропертей: host, port, user, password, vHost не будет найдена, то будет исключение.
   

        * Поддержка retry(дефолтные значения при enabled=true):

        ```        
        queue.management.configurations.<connection-name>.retry.maxAttempts=3
        queue.management.configurations.<connection-name>.retry.initial-interval=1000
        queue.management.configurations.<connection-name>.retry.multiplier=1.0
        queue.management.configurations.<connection-name>.retry.maxI-interval=10000
        ```
     
      Дополнительно (см. документацию spring-amqp)к dead-letter:
      `queue.management.configurations.<connection-name>.defaultRequeueRejected`

5. Все бины созданные библиотекой имеют префикс зависящий от `<connection-name>`. Все типы бинов описаны здесь `AmqpAutoConfigurationConstants.Bean.*`.
  
6. В аннотации @RabbitListener нужно указывать пропертю `conatinerFactory` и `admin`. Пример:
    ```java
    @RabbitListener(queues = "fakeQueueName", containerFactory = "localRabbitListenerContainerFactory",  admin="localRabbitAdmin")
    ```

7.  Есть настройки для многопоточной обработки @RabbitListener
    ```
    queue.management.configurations.<connection-name>.listener.concurrentConsumers
    queue.management.configurations.<connection-name>.listener.maxConcurrentConsumers
    ```

8. Для отключения конфигурации в тестах добавить в .properties
    ```
    queue.management.enabled=false
    ```
10. Дефолтные значения настроек:
    ```
    queue.management.enabled=false
    queue.management.custom-object-mapper=false
    
    queue.management.configurations.<connection-name>.retry.enabled=false 
    queue.management.configurations.<connection-name>.retry.maxAttempts=3
    queue.management.configurations.<connection-name>.retry.initial-interval=1000
    queue.management.configurations.<connection-name>.retry.multiplier=1.0
    queue.management.configurations.<connection-name>.retry.maxI-interval=10000
    
    queue.management.configurations.<connection-name>.mismatchedQueuesFatal=true
    queue.management.configurations.<connection-name>.defaultRequeueRejected=true
    queue.management.configurations.<connection-name>.dead-letter-suffix=.errors
    
    queue.management.configurations.<connection-name>.listener.concurrentConsumers=1
    queue.management.configurations.<connection-name>.listener.maxConcurrentConsumers=1
    
    queue.management.configurations.<connection-name>.queues.<queue-name>.durable=false
    queue.management.configurations.<connection-name>.queues.<queue-name>.exclusive=false
    queue.management.configurations.<connection-name>.queues.<queue-name>.autoDelete=false
    queue.management.configurations.<connection-name>.queues.<queue-name>.with-dead-letter=false
    queue.management.configurations.<connection-name>.queues.<queue-name>.dead-letter-config.bidirectional=false
    queue.management.configurations.<connection-name>.queues.<queue-name>.dead-letter-config.time-to-live=3600000
    
    
    queue.management.configurations.<connection-name>.topicExchanges.<topic-exchange-name>.name=
    queue.management.configurations.<connection-name>.topicExchanges.<topic-exchange-name>.durable=false
    queue.management.configurations.<connection-name>.topicExchanges.<topic-exchange-name>.autoDelete=false
    
    queue.management.configurations.<connection-name>.bindings.<queue-name>.<topic-exchange-name>.routingKeys.<routing-key-name>=*.*.*
    ```
    
11. Features:
    
    1.  Добавлена стратегия наименования подключения к rabbit.
        В ui RabbitAdmin в разделе connection можно будет увидеть имя у connection, 
        которое строится по правилу: `${spring.application.name}+<connection-name>`.
    2.  В настройки добавлен параметр mismatchedQueuesFatal.
        Из документации: 
        `
            This was added in version 1.6. When the container starts, if this property is true, 
            the container checks that all queues declared in the context are compatible with queues already on the broker.
            If mismatched properties (e.g. auto-delete) or arguments (e.g. x-message-ttl) exist, 
            the container (and application context) will fail to start with a fatal exception.
            If the problem is detected during recovery (e.g. after a lost connection), the container will be stopped.
        `
    
    3. Добавлена поддержка многопоточных слушателей,
    которые можно настроить персонально для каждой очереди 
        
          ```
          queue.management.configurations.<connection-name>.queues.<queue-name>.listener.concurrentConsumers=<value>
          queue.management.configurations.<connection-name>.queues.<queue-name>.listener.maxConcurrentConsumers=<value>
          ```   
          Т.к. слушатель может быть настроен сразу на несколько очередей, то есть следующие варианты:
         1. Только одна из очередей слушателя настроена на многопоточную обработку. 
         Тогда ко всему слушателю будут применены настройки этой очереди.
         2. Ни одна из очередей не требует многопоточной обработки.
         Тогда слушатель будет работать в однопоточном режиме, или согласно общим настройкам для всех слушателей:
         
            ```
            queue.management.configurations.<connection-name>.listener.concurrentConsumers=<value>
            queue.management.configurations.<connection-name>.listener.maxConcurrentConsumers=<value>
            ```
         3. Две или более очередей имеют персональные настройки многопоточности.
            Будет выбрана одна из персональных настроек, у которой maxConcurrentConsumers максимальное.
        
    
    