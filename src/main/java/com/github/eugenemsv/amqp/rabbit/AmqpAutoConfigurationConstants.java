package com.github.eugenemsv.amqp.rabbit;

public class AmqpAutoConfigurationConstants {

    public static final String CONSTRUCTOR_CALL_NOT_ALLOWED = "Constructor call not allowed";

    private AmqpAutoConfigurationConstants() {
        throw new UnsupportedOperationException(CONSTRUCTOR_CALL_NOT_ALLOWED);
    }

    public static class Queue {

        private Queue() {
            throw new UnsupportedOperationException(CONSTRUCTOR_CALL_NOT_ALLOWED);
        }

        public static final String ARGUMENT_KEY_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";

        public static final String ARGUMENT_KEY_DEAD_LETTER_ROUTING = "x-dead-letter-routing-key";

        public static final String ARGUMENT_VALUE_DEAD_LETTER_EXCHANGE = "";

        public static final String ARGUMENT_KEY_MESSAGE_TTL = "x-message-ttl";
    }

    public static class Property {

        private Property() {
            throw new UnsupportedOperationException(CONSTRUCTOR_CALL_NOT_ALLOWED);
        }

        public static final String AUTO_CONFIGURE_EXCLUDE = "spring.autoconfigure.exclude";

        public static final String ROOT_PREFIX = "queue.management";

        public static final String MANAGEMENT_ENABLED = ROOT_PREFIX+".enabled";

        public static final String FACTORY_BEAN_NAME = "queue.management.bean.factory";

        public static final String FACTORY_BEAN_NAME_DEFAULT = "amqpBeansFactory";

        public static final String BEAN_DEFINITION_CUSTOMIZER = "queue.management.bean.definition.customizer";

        public static final String BEAN_DEFINITION_CUSTOMIZER_DEFAULT = "com.github.eugenemsv.amqp.rabbit.bean.DefaultAmqpBeanDefinitionCustomizer";

        public static final String APPLICATION_NAME = "spring.application.name";

        public static final String APPLICATION_NAME_DEFAULT = "app";

        public static final String HOST = ".host";

        public static final String PORT = ".port";

        public static final String USER = ".user";

        public static final String PASSWORD = ".password";

        public static final String VHOST = ".vHost";
    }


    public static class Bean {

        public static final String CONNECTION_FACTORY_SUFFIX = "ConnectionFactory";

        public static final String RABBIT_LISTENER_CONTAINER_FACTORY_SUFFIX = "RabbitListenerContainerFactory";

        public static final String RABBIT_ADMIN_SUFFIX = "RabbitAdmin";

        public static final String RABBIT_TEMPLATE_SUFFIX = "RabbitTemplate";

        public static final String QUEUE_SUFFIX = "Queue";

        public static final String TOPIC_EXCHANGE_SUFFIX = "TopicExchange";

        public static final String BINDING_SUFFIX = "Binding";

        public static final String QUEUE_DEAD_LETTER_SUFFIX = "DeadLetterQueue";

        public static final String FACTORY_METHOD_PREFIX = "supply";

        private Bean() {
            throw new UnsupportedOperationException(CONSTRUCTOR_CALL_NOT_ALLOWED);
        }

    }
}
