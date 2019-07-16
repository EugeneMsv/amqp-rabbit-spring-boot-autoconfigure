package com.demo.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.HashMap;
import java.util.Map;

import static com.demo.amqp.AmqpAutoConfigurationConstants.Property.AUTO_CONFIGURE_EXCLUDE;

public class AmqpEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AmqpEnvironmentPostProcessor.class);

    public static final String EXCLUDE_RABBIT_AUTO_CONFIGURATION_VALUE = RabbitAutoConfiguration.class
            .getCanonicalName();

    private static final String EXCLUDE_AUTO_CONFIGURATION_DELIMITER = ",";

    public static final String PROPERTY_SOURCE_NAME = "amqpBuiltSource";


    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

        String enabled = environment.getProperty(AmqpAutoConfigurationConstants.Property.MANAGEMENT_ENABLED);
        if ("false".equalsIgnoreCase(enabled)) {
            logger.debug("Skip cause {} is false", AmqpAutoConfigurationConstants.Property.MANAGEMENT_ENABLED);
            return;
        }
        String exclusions = environment.getProperty(AUTO_CONFIGURE_EXCLUDE);
        if (exclusions != null) {
            if (exclusions.contains(EXCLUDE_RABBIT_AUTO_CONFIGURATION_VALUE)) {
                logger.debug("Auto configuration '{}' already excluded", EXCLUDE_RABBIT_AUTO_CONFIGURATION_VALUE);
                return;
            }
            String updatedExclusions = exclusions + EXCLUDE_AUTO_CONFIGURATION_DELIMITER + EXCLUDE_RABBIT_AUTO_CONFIGURATION_VALUE;
            addFirstPriorityProperty(environment, updatedExclusions);
        } else {
            String newExclusions = EXCLUDE_RABBIT_AUTO_CONFIGURATION_VALUE;
            addFirstPriorityProperty(environment, newExclusions);
        }
    }

    private void addFirstPriorityProperty(ConfigurableEnvironment environment, String autoConfigurationExclusions) {
        MutablePropertySources propertySources = environment.getPropertySources();
        Map<String, Object> properties = new HashMap<>();
        properties.put(AUTO_CONFIGURE_EXCLUDE, autoConfigurationExclusions);
        propertySources.addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
        logger.debug("Added property '{}:{}' with highest priority", AUTO_CONFIGURE_EXCLUDE,
                autoConfigurationExclusions);
    }
}
