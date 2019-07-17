package com.github.eugenemsv.amqp.rabbit;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class AnnotationApplicationContextTestWrapper {

    private final AnnotationConfigApplicationContext context;

    private AnnotationApplicationContextTestWrapper(
            AnnotationConfigApplicationContext context) {
        this.context = context;
    }

    public static AnnotationApplicationContextTestWrapper from(AnnotationConfigApplicationContext context) {
        return new AnnotationApplicationContextTestWrapper(context);
    }

    public AnnotationApplicationContextTestWrapper withConfiguration(Class<?>... annotatedClasses) {
        context.register(annotatedClasses);
        return this;
    }

    public AnnotationApplicationContextTestWrapper withEnvironment(String... pairs) {
        TestPropertyValues.of(pairs).applyTo(context);
        return this;
    }

    public AnnotationApplicationContextTestWrapper build() {
        context.refresh();
        return this;
    }

    public void close() {
        if (this.context != null) {
            this.context.close();
        }
    }

    public ApplicationContext getContext(){
        return this.context;
    }

}
