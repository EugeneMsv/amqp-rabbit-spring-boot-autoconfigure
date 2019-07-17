package com.github.eugene.msv.amqp.rabbit;

import java.util.Arrays;

public class IllegalAmqpEnvironmentException extends RuntimeException {

    public IllegalAmqpEnvironmentException(String message) {
        super(message);
    }

    public IllegalAmqpEnvironmentException(String message, Object... params) {
        super(params[params.length - 1] instanceof Throwable
                ? String.format(message, Arrays.copyOfRange(params, 0, params.length - 1))
                : String.format(message, params));
        Object lastArg = params[params.length - 1];
        if (lastArg instanceof Throwable) {
            this.initCause((Throwable) lastArg);
        }
    }

    public IllegalAmqpEnvironmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
