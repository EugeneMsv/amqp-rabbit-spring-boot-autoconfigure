package com.github.eugenemsv.amqp.rabbit.properties;

import java.util.HashMap;
import java.util.Map;

public class AmqpTopicExchangeProperties {
  private String name;

  private boolean durable;

  private boolean autoDelete;

  private Map<String, Object> arguments;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isDurable() {
    return durable;
  }

  public void setDurable(boolean durable) {
    this.durable = durable;
  }

  public boolean isAutoDelete() {
    return autoDelete;
  }

  public void setAutoDelete(boolean autoDelete) {
    this.autoDelete = autoDelete;
  }

  public Map<String, Object> getArguments() {
    if (arguments == null) {
      arguments = new HashMap<>();
    }
    return arguments;
  }

  public void setArguments(Map<String, Object> arguments) {
    this.arguments = arguments;
  }
}
