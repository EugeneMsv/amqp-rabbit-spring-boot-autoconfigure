package com.github.eugene.msv.amqp.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.amqp.support.converter.AbstractJackson2MessageConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

public class AmqpMessageConverterAutoConfigurationTest {

    private AnnotationApplicationContextTestWrapper contextWrapper;

    @Before
    public void setUp() {
        contextWrapper = AnnotationApplicationContextTestWrapper.from(new AnnotationConfigApplicationContext());
    }

    @After
    public void tearDown() {
        contextWrapper.close();
    }


    @Test
    public void test_objectMapper_created() throws NoSuchFieldException, IllegalAccessException {
        ApplicationContext context = contextWrapper.withConfiguration(AmqpMessageConverterAutoConfiguration.class)
                .withEnvironment("queue.management.custom-object-mapper:false")
                .build()
                .getContext();

        assertTrue(context.containsBean("amqpDefaultObjectMapper"));
        ObjectMapper amqpDefaultObjectMapper = (ObjectMapper) context.getBean("amqpDefaultObjectMapper");
        Jackson2JsonMessageConverter jacksonMessageConverter = context.getBean(
                "jacksonMessageConverter", Jackson2JsonMessageConverter.class);
        assertNotNull(jacksonMessageConverter);

        Field jsonObjectMapper = AbstractJackson2MessageConverter.class.getDeclaredField("objectMapper");
        jsonObjectMapper.setAccessible(true);
        jsonObjectMapper.get(jacksonMessageConverter);

        assertSame(amqpDefaultObjectMapper, jsonObjectMapper.get(jacksonMessageConverter));
    }

    @Configuration
    protected static class ObjectMapperConfiguration {

        @Bean
        public ObjectMapper customObjectMapper() {
            return new ObjectMapper();
        }

    }

    @Test
    public void test_objectMapper_tookCustom() throws NoSuchFieldException, IllegalAccessException {
        ApplicationContext context = contextWrapper.withConfiguration(
                ObjectMapperConfiguration.class,
                AmqpMessageConverterAutoConfiguration.class)
                .withEnvironment("queue.management.custom-object-mapper:false")
                .build()
                .getContext();

        assertFalse(context.containsBean("amqpDefaultObjectMapper"));
        ObjectMapper customObjectMapper = (ObjectMapper) context.getBean("customObjectMapper");
        Jackson2JsonMessageConverter jacksonMessageConverter = context.getBean("jacksonMessageConverter",
                Jackson2JsonMessageConverter.class);
        assertNotNull(jacksonMessageConverter);

        Field jsonObjectMapper = AbstractJackson2MessageConverter.class.getDeclaredField("objectMapper");
        jsonObjectMapper.setAccessible(true);
        jsonObjectMapper.get(jacksonMessageConverter);

        assertSame(customObjectMapper, jsonObjectMapper.get(jacksonMessageConverter));
    }

}