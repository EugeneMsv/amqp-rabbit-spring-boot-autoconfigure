package com.demo.amqp.utils;

import com.demo.amqp.AmqpAutoConfigurationConstants;
import com.demo.amqp.IllegalAmqpEnvironmentException;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

public class ValidationUtils {

    private ValidationUtils() {
        throw new UnsupportedOperationException(AmqpAutoConfigurationConstants.CONSTRUCTOR_CALL_NOT_ALLOWED);
    }

    public static <T> void notNull(T object, Supplier<RuntimeException> exceptionSupplier) {
        if (object == null) {
            throw exceptionSupplier.get();
        }
    }

    public static <T extends CharSequence> void notEmpty(T chars, Supplier<RuntimeException> exceptionSupplier) {
        if (chars == null || chars.length() == 0) {
            throw exceptionSupplier.get();
        }
    }

    public static <T extends Map> void notEmpty(T map, Supplier<RuntimeException> exceptionSupplier) {
        if (map == null || map.isEmpty()) {
            throw exceptionSupplier.get();
        }
    }

    public static <T extends Collection> void notEmpty(T collection, Supplier<RuntimeException> exceptionSupplier) {
        if (collection == null || collection.isEmpty()) {
            throw exceptionSupplier.get();
        }
    }

    public static <T> void notNullEnv(T object, String message, Object... params) {
        notNull(object, () -> new IllegalAmqpEnvironmentException(message, params));
    }

    public static <T extends CharSequence> void notEmptyEnv(T chars, String message, Object... params) {
        notEmpty(chars, () -> new IllegalAmqpEnvironmentException(message, params));
    }

    public static <T extends Map> void notEmptyEnv(T map, String message, Object... params) {
        notEmpty(map, () -> new IllegalAmqpEnvironmentException(message, params));
    }

    public static <T extends Collection> void notEmptyEnv(T collection, String message, Object... params) {
        notEmpty(collection, () -> new IllegalAmqpEnvironmentException(message, params));
    }
}
